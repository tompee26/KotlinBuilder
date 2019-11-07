package com.tompee.kotlinbuilder.processor

import com.marcinmoskala.math.powerset
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.models.DefaultParameter
import com.tompee.kotlinbuilder.processor.models.Parameter
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * Generates a builder code and file from the given [Element]
 *
 * @property env processing environment
 * @param element the input element
 * @property providerMap default value provider map
 */
@KotlinPoetMetadataPreview
internal class BuilderGenerator(
    private val env: ProcessingEnvironment,
    element: Element,
    private val providerMap: Map<TypeName, TypeName>
) {
    /**
     * Type element property
     */
    private val property =
        TypeElementProperties(env, element as TypeElement)

    /**
     * Output builder class name
     */
    private val builderClassName by lazy { generateBuilderClassName() }

    /**
     * Constructor parameter list
     */
    private val parameterList by lazy {
        Parameter.parse(element as TypeElement, property.getTypeSpec(), env, providerMap)
    }

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
        val name = property.getBuilderAnnotation().name
        val builderName = if (name.isEmpty()) "${property.getName()}Builder" else name
        return ClassName(property.getPackageName(), builderName)
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
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR, KModifier.INLINE)
            .returns(property.getTypeName())
            .addParameters(parameterList.filterNot { it.isOptional() }.map { it.toInvokeParamSpec() })
            .addParameter(
                "builderInit",
                LambdaTypeName.get(builderClassName, returnType = Unit::class.java.asTypeName()),
                KModifier.CROSSINLINE
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
            .returns(property.getTypeName())

        val defaultParameters = parameterList.filterIsInstance<DefaultParameter>()
        if (defaultParameters.isEmpty()) {
            builder.addStatement(
                "return ${property.getTypeName()}(${parameterList.joinToString(
                    separator = ", "
                ) { it.name }})".wrapProof()
            )
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
                    return@map "$condition -> ${property.getTypeName()}($nonDefaultInitializer$defaultInitializer)"
                }
                .forEach { builder.addStatement(it.wrapProof()) }
            builder.addStatement(
                "else -> ${property.getTypeName()}(${nonDefaultParameters.joinToString(separator = ", ") { "${it.name} = ${it.name}" }})".wrapProof()
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
        check(!property.getTypeSpec().modifiers.any { it == KModifier.PRIVATE }) { "${property.getName()} is a private class" }

        val shouldBeInternal = property.getTypeSpec().modifiers.any { it == KModifier.INTERNAL }
        val classSpecBuilder = TypeSpec.classBuilder(builderClassName)
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
        val fileSpec = FileSpec.builder(property.getPackageName(), name)
            .addType(outputClassSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "$name.kt"))
    }
}