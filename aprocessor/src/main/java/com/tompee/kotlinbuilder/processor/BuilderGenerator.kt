package com.tompee.kotlinbuilder.processor

import com.marcinmoskala.math.powerset
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.models.DefaultParameter
import com.tompee.kotlinbuilder.processor.models.Parameter
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement


@UseExperimental(KotlinPoetMetadataPreview::class)
internal class BuilderGenerator(
    private val env: ProcessingEnvironment,
    private val element: Element
) {
    /**
     * Type utils
     */
    private val typeUtils = env.typeUtils

    /**
     * KBuilder class annotation instance
     */
    private val annotation = element.getAnnotation(KBuilder::class.java)

    /**
     * Package name
     */
    private val packageName = env.elementUtils.getPackageOf(element).toString()

    /**
     * Output builder class name
     */
    private val inputClassName by lazy { (element as TypeElement).asType().asTypeName() }

    /**
     * Output builder class name
     */
    private val builderClassName by lazy { generateBuilderClassName() }

    /**
     * Input class type spec
     */
    private val inputTypeSpec by lazy { determineTypeSpec(element) }

    /**
     * Constructor parameter list
     */
    private val parameterList by lazy { Parameter.parse(element as TypeElement, inputTypeSpec, typeUtils) }

    /**
     * Output builder constructor
     */
    private val builderConstructor by lazy { buildConstructor() }

    /**
     * Output builder companion object
     */
    private val outputCompanionObject by lazy { createCompanionObject() }

    /**
     * Output builder class spec
     */
    private val outputClassSpec by lazy { buildClassSpec() }

    /**
     * Setter methods
     */
    private val setterMethods by lazy { parameterList.map { it.toBuilderFunSpec(builderClassName) } }

    /**
     * Build method
     */
    private val buildMethod by lazy { createBuildMethod() }

    /**
     * Generates the builder class name from the annotation if available
     */
    private fun generateBuilderClassName(): ClassName {
        val builderName =
            if (annotation.name.isEmpty()) "${element.simpleName}Builder" else annotation.name
        return ClassName(packageName, builderName)
    }

    /**
     * Determines the type spec. This is a fallback to read the parameter types
     */
    private fun determineTypeSpec(element: Element): TypeSpec {
        return (element as TypeElement).toImmutableKmClass().toTypeSpec(null)
    }

    /**
     * Builds the constructors using the parameter list
     */
    private fun buildConstructor(): FunSpec {
        val constructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameterList.map { it.toCtrParamSpec() })
        return constructor.build()
    }

    /**
     * Creates the companion object with a single build function that accepts the mandatory arguments
     */
    private fun createCompanionObject(): TypeSpec {
        //region First invoke overload
        val createOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
            .returns(inputClassName)
            .addParameters(parameterList.filterNot { it.isOptional() }.map { it.toInvokeParamSpec() })
            .addParameter(
                "builderInit",
                LambdaTypeName.get(builderClassName, returnType = Unit::class.java.asTypeName())
            )

        parameterList.filter { it.isOptional() }
            .forEach { createOverload.addStatement(it.createInitializeStatement()) }

        createOverload
            .addStatement(
                "val builder = ${builderClassName}(${parameterList.joinToString(separator = ", ") { it.name }})".wrapProof()
            )
            .addStatement("builderInit(builder)".wrapProof())
            .addStatement("return builder.build()".wrapProof())
        //endregion

        // regionFirst invoke overload
        val builderOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
            .returns(builderClassName)
            .addParameters(parameterList.filterNot { it.isOptional() }.map { it.toInvokeParamSpec() })

        parameterList.filter { it.isOptional() }
            .forEach { builderOverload.addStatement(it.createInitializeStatement()) }

        builderOverload.addStatement(
            "return ${builderClassName}(${parameterList.joinToString(separator = ", ") { it.name }})".wrapProof()
        )
        //endregion

        return TypeSpec.companionObjectBuilder()
            .addFunction(createOverload.build())
            .addFunction(builderOverload.build())
            .build()
    }

    /**
     * Creates the build method
     */
    private fun createBuildMethod(): FunSpec {
        val builder = FunSpec.builder("build")
            .returns(inputClassName)

        val defaultParameters = parameterList.filterIsInstance<DefaultParameter>()
        if (defaultParameters.isEmpty()) {
            builder.addStatement("return $inputClassName(${parameterList.joinToString(separator = ", ") { it.name }})".wrapProof())
        } else {
            val nonDefaultParameters = parameterList.filterNot { it is DefaultParameter }

            builder.beginControlFlow("return when")
            defaultParameters.powerset().filterNot { it.isEmpty() }
                .sortedByDescending { it.count() }
                .map { params ->
                    val condition = params.joinToString(separator = " && ") { "${it.name} != null" }
                    val nonDefaultInitializer = if (nonDefaultParameters.isEmpty()) ""
                    else nonDefaultParameters.joinToString(
                        separator = ", ", postfix = ", "
                    ) { "${it.name} = ${it.name}" }
                    val defaultInitializer =
                        params.joinToString(separator = ", ") { "${it.name} = ${it.name}!!" }
                    return@map "$condition -> $inputClassName($nonDefaultInitializer$defaultInitializer)"
                }
                .forEach { builder.addStatement(it.wrapProof()) }
            builder.addStatement(
                "else -> $inputClassName(${nonDefaultParameters.joinToString(separator = ", ") { "${it.name} = ${it.name}" }})".wrapProof()
            )
            builder.endControlFlow()
        }
        return builder.build()
    }

    /**
     * Builds the class spec. Also adds properties to be able to set constructor parameter's
     * modifier
     */
    private fun buildClassSpec(): TypeSpec {
        val className =
            if (annotation.name.isEmpty()) "${element.simpleName}Builder" else annotation.name

        check(!inputTypeSpec.modifiers.any { it == KModifier.PRIVATE }) { "$inputClassName is a private class" }

        val shouldBeInternal = inputTypeSpec.modifiers.any { it == KModifier.INTERNAL } ||
                parameterList.any { param -> param.propertySpec.modifiers.any { it == KModifier.INTERNAL } }

        val classSpecBuilder = TypeSpec.classBuilder(className)
            .primaryConstructor(builderConstructor)
            .addType(outputCompanionObject)
            .addProperties(parameterList.map { it.toPropertySpec() })
            .addFunctions(setterMethods)
            .addFunction(buildMethod)
        if (shouldBeInternal) classSpecBuilder.addModifiers(KModifier.INTERNAL)
        return classSpecBuilder.build()
    }

    fun generate() {
        val name = outputClassSpec.name.toString()
        val fileSpec = FileSpec.builder(packageName, name)
            .addType(outputClassSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "$name.kt"))
    }
}