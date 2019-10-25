package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.models.Parameter
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror


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
    private val parameterList by lazy { getParameters() }

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


    private val kotlinClassType = (element as TypeElement).toImmutableKmClass()


//    private val builderType = ClassName(packageName, className)
//
//    private val classSpec = TypeSpec.classBuilder(className)
//    private val fileSpec = FileSpec.builder(packageName, className)

    init {
        buildConstructor()
    }

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
     * Parses the constructor parameters. We will rely on manually parsing the primary constructor
     * as input type spec cannot determine it
     */
    private fun getParameters(): List<Parameter> {
        val javaConstructor =
            element.enclosedElements.firstOrNull { it.kind == ElementKind.CONSTRUCTOR } as? ExecutableElement
                ?: throw IllegalStateException("No constructor found for ${element.simpleName}")

        /**
         * Now we use the java constructor to check for annotations
         */
        val parameters = javaConstructor.parameters.map {
            Parameter(it.simpleName.toString(), optional = it.getAnnotation(Optional::class.java))
        }

        /**
         * Now we will need the actual kotlin type of the parameter
         */
        inputTypeSpec.propertySpecs.forEach { propertySpec ->
            parameters.find { it.name == propertySpec.name }?.apply { typeName = propertySpec.type }
        }
        return parameters
    }

    /**
     * Builds the constructors using the parameter list
     */
    private fun buildConstructor(): FunSpec {
        val constructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
        parameterList.forEach {
            constructor.addParameter(it.name, it.getTypeOrFail(), KModifier.PRIVATE)
        }
        return constructor.build()
    }

    /**
     * Creates the companion object with a single build function that accepts the mandatory arguments
     */
    private fun createCompanionObject(): TypeSpec {
        fun createProviderEvaluationStatement(
            buildSpecBuilder: FunSpec.Builder,
            name: String,
            type: TypeName
        ) {
            buildSpecBuilder.addStatement("val $name = $type().get()")
        }

        //region First invoke overload
        val createOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
        parameterList.filterNot { it.isOptional() }.forEach {
            createOverload.addParameter(it.name, it.getTypeOrFail())
        }
        // Need the builder type here. Just gonna use the name here
        createOverload.addParameter(
            "builderInit",
            LambdaTypeName.get(builderClassName, returnType = Unit::class.java.asTypeName())
        ).returns(inputClassName)

        val createParamNames = parameterList.map {
            if (it.isOptional()) {
                val typeName = it.optional!!.getProvider().asTypeName()
                createProviderEvaluationStatement(createOverload, it.name, typeName)
            }
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
        parameterList.filterNot { it.isOptional() }.forEach {
            builderOverload.addParameter(it.name, it.getTypeOrFail())
        }
        // Need the builder type here. Just gonna use the name here
        builderOverload.returns(builderClassName)

        val builderParamNames = parameterList.map {
            if (it.isOptional()) {
                val typeName = it.optional!!.getProvider().asTypeName()
                createProviderEvaluationStatement(builderOverload, it.name, typeName)
            }
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
    private fun generateBuilderMethods() {
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
            val propertySpec = PropertySpec.builder(it.name, it.getTypeOrFail())
                .initializer(it.name)
                .mutable()
            classSpecBuilder.addProperty(propertySpec.build())
        }
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

    private fun Optional.getProvider(): TypeMirror {
        try {
            provider
        } catch (mte: MirroredTypeException) {
            return mte.typeMirror
        }

        throw IllegalStateException("Provider type cannot be determined")
    }
}