package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.Builder
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
     * Builder class annotation instance
     */
    private val annotation = element.getAnnotation(Builder::class.java)

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
    private val parameterList by lazy { Parameter.parse(element as TypeElement, inputTypeSpec) }

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
    private val setterMethods by lazy { generateBuilderMethods() }

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
        parameterList.forEach {
            constructor.addParameter(it.name, it.type, KModifier.PRIVATE)
        }
        return constructor.build()
    }

    /**
     * Creates the companion object with a single build function that accepts the mandatory arguments
     */
    private fun createCompanionObject(): TypeSpec {
        fun createProviderEvaluationStatement(
            buildSpecBuilder: FunSpec.Builder,
            parameter: Parameter
        ) {
            when {
                parameter.isNullable() -> {
                    buildSpecBuilder.addStatement("val ${parameter.name} : ${parameter.type}? = null")
                }
                parameter.withGenerator() -> {
                    val typeName = parameter.getProvider()
                    buildSpecBuilder.addStatement("val ${parameter.name} = $typeName().get()")
                }
                else -> throw IllegalStateException("Class: ${inputClassName}, Parameter: ${parameter.name}. Optional parameter must define at least one method to determine default value")
            }
        }

        //region First invoke overload
        val createOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
        parameterList.filterNot { it.isOptional }.forEach {
            createOverload.addParameter(it.name, it.type)
        }
        // Need the builder type here. Just gonna use the name here
        createOverload.addParameter(
            "builderInit",
            LambdaTypeName.get(builderClassName, returnType = Unit::class.java.asTypeName())
        ).returns(inputClassName)

        val createParamNames = parameterList.map {
            if (it.isOptional) createProviderEvaluationStatement(createOverload, it)
            it.name
        }

        createOverload.addStatement(
            "val builder = ${builderClassName}(${createParamNames.joinToString(
                separator = ", "
            )})"
        )
        createOverload.addStatement("builderInit(builder)")
        createOverload.addStatement("return builder.build()")
        //endregion

        // regionFirst invoke overload
        val builderOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
        parameterList.filterNot { it.isOptional }.forEach {
            builderOverload.addParameter(it.name, it.type)
        }
        // Need the builder type here. Just gonna use the name here
        builderOverload.returns(builderClassName)

        val builderParamNames = parameterList.map {
            if (it.isOptional) createProviderEvaluationStatement(builderOverload, it)
            it.name
        }

        builderOverload.addStatement(
            "return ${builderClassName}(${builderParamNames.joinToString(
                separator = ", "
            )})"
        )
        //endregion

        return TypeSpec.companionObjectBuilder()
            .addFunction(createOverload.build())
            .addFunction(builderOverload.build())
            .build()
    }

    /**
     * Creates the builder methods from the optional arguments
     */
    private fun generateBuilderMethods(): List<FunSpec> {
        return parameterList.map {
            val name = it.setter?.name ?: it.name
            val providerParamType =
                LambdaTypeName.get(builderClassName, returnType = it.type)
            FunSpec.builder(name)
                .addParameter(ParameterSpec.builder("provider", providerParamType).build())
                .returns(builderClassName)
                .addStatement("return apply { ${it.name} = provider()}")
                .build()
        }
    }

    /**
     * Creates the build method
     */
    private fun createBuildMethod(): FunSpec {
        return FunSpec.builder("build")
            .returns(inputClassName)
            .addStatement("return $inputClassName(${parameterList.joinToString(separator = ", ") { it.name }})")
            .build()
    }

    /**
     * Builds the class spec. Also adds properties to be able to set constructor parameter's
     * modifier
     */
    private fun buildClassSpec(): TypeSpec {
        val className =
            if (annotation.name.isEmpty()) "${element.simpleName}Builder" else annotation.name
        val classSpecBuilder = TypeSpec.classBuilder(className)
            .primaryConstructor(builderConstructor)
            .addType(outputCompanionObject)
        parameterList.forEach {
            val propertySpec = PropertySpec.builder(it.name, it.type)
                .initializer(it.name)
                .mutable()
            classSpecBuilder.addProperty(propertySpec.build())
        }
        setterMethods.forEach { classSpecBuilder.addFunction(it) }
        classSpecBuilder.addFunction(buildMethod)
        return classSpecBuilder.build()
    }

    fun generate() {
        val name = outputClassSpec.name.toString()
        val fileSpec = FileSpec.builder(packageName, name)
            .addType(outputClassSpec)
            .build()
        val kaptKotlinGeneratedDir = env.options[BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "$name.kt"))
    }
}