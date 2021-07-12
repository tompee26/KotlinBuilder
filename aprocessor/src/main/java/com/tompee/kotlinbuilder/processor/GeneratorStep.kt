package com.tompee.kotlinbuilder.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.models.DefaultParameter
import com.tompee.kotlinbuilder.processor.models.Parameter
import com.tompee.kotlinbuilder.processor.models.ProviderParameter
import com.tompee.kotlinbuilder.processor.parser.ParameterParser
import com.tompee.kotlinbuilder.processor.processor.ProviderProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@KotlinPoetMetadataPreview
internal class GeneratorStep(
    private val elements: Elements,
    private val types: Types,
    private val messager: Messager,
    private val filer: Filer
) : BasicAnnotationProcessor.ProcessingStep {

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        val providers = elementsByAnnotation.entries()
            .filter { it.key == Optional.Provides::class.java }
            .map { it.value }
        elementsByAnnotation.entries()
            .filter { it.key == KBuilder::class.java }
            .forEach { generate(it.value as TypeElement, providers) }
        return mutableSetOf()
    }

    override fun annotations(): MutableSet<out Class<out Annotation>> {
        return mutableSetOf(
            KBuilder::class.java,
            Optional::class.java,
            Optional.Nullable::class.java,
            Optional.ValueProvider::class.java,
            Optional.Default::class.java,
            Optional.Provides::class.java
        )
    }

    private fun generate(
        element: TypeElement,
        providers: List<Element>
    ) {
        try {
            val properties = TypeElementProperties(element, elements, types)
            val builderName = getBuilderClassName(properties)

            val fileSpec = FileSpec.builder(properties.getPackageName(), builderName.toString())
                .addType(buildClassSpec(properties, builderName, providers))
                .build()
            fileSpec.writeTo(filer)
        } catch (e: Throwable) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.message, element)
        }
    }

    /**
     * Generates the builder class name from the annotation if available
     */
    private fun getBuilderClassName(properties: TypeElementProperties): ClassName {
        val name = properties.getBuilderAnnotation().name
        val builderName = if (name.isEmpty()) "${properties.getName()}Builder" else name
        return ClassName(properties.getPackageName(), builderName)
    }

    /**
     * Builds the class spec. Also adds properties to be able to set constructor parameter's
     * modifier
     */
    private fun buildClassSpec(
        properties: TypeElementProperties,
        builderName: ClassName,
        providers: List<Element>
    ): TypeSpec {
        check(!properties.getTypeSpec().modifiers.any { it == KModifier.PRIVATE }) { "${properties.getName()} is a private class" }

        val parameterParser = ParameterParser(ProviderParameter.Builder.Factory(properties))
        val parameterList =
            parameterParser.parse(
                properties.typeElement,
                properties.getTypeSpec(),
                ProviderProcessor(properties.classInspector).getProviderMap(providers)
            )

        val shouldBeInternal = properties.getTypeSpec().modifiers.any { it == KModifier.INTERNAL }
        val classSpecBuilder = TypeSpec.classBuilder(builderName)
            .primaryConstructor(buildConstructor(parameterList))
            .addType(createCompanionObject(parameterList, builderName, properties))
            .addProperties(parameterList.map { it.toPropertySpec() })
            .addFunctions(parameterList.map { it.toBuilderFunSpec(builderName) })
            .addFunction(createBuildMethod(parameterList, properties))
        if (shouldBeInternal) classSpecBuilder.addModifiers(KModifier.INTERNAL)
        return classSpecBuilder.build()
    }

    /**
     * Builds the constructors using the parameter list
     */
    private fun buildConstructor(parameterList: List<Parameter>): FunSpec {
        val constructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameterList.map { it.toCtrParamSpec() })
        return constructor.build()
    }

    /**
     * Creates the companion object with a single build function that accepts the mandatory arguments
     */
    private fun createCompanionObject(
        parameterList: List<Parameter>,
        builderName: ClassName,
        properties: TypeElementProperties
    ): TypeSpec {
        //region First invoke overload
        val createOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR, KModifier.INLINE)
            .returns(properties.getTypeName())
            .addParameters(parameterList.filterNot { it.isOptional() }
                .map { it.toInvokeParamSpec() })
            .addParameter(
                "builderInit",
                LambdaTypeName.get(builderName, returnType = Unit::class.java.asTypeName()),
                KModifier.CROSSINLINE
            )

        parameterList.filter { it.isOptional() }
            .forEach { createOverload.addStatement(it.createInitializeStatement()) }

        createOverload
            .addStatement(
                "val builder = ${builderName}(${parameterList.joinToString(separator = ", ") { it.name }})".wrapProof()
            )
            .addStatement("builderInit(builder)".wrapProof())
            .addStatement("return builder.build()".wrapProof())
        //endregion

        // regionFirst invoke overload
        val builderOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR)
            .returns(builderName)
            .addParameters(parameterList.filterNot { it.isOptional() }
                .map { it.toInvokeParamSpec() })

        parameterList.filter { it.isOptional() }
            .forEach { builderOverload.addStatement(it.createInitializeStatement()) }

        builderOverload.addStatement(
            "return ${builderName}(${parameterList.joinToString(separator = ", ") { it.name }})".wrapProof()
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
    private fun createBuildMethod(
        parameterList: List<Parameter>, properties: TypeElementProperties
    ): FunSpec {
        val builder = FunSpec.builder("build")
            .returns(properties.getTypeName())

        /**
         * Add initializers
         */
        parameterList.mapNotNull { it.toBuildInitializer() }
            .forEach { builder.addStatement(it) }

        val defaultParameters = parameterList.filterIsInstance<DefaultParameter>()
        if (defaultParameters.isEmpty()) {
            builder.addStatement(
                "return ${properties.getTypeName()}(${parameterList.joinToString(
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
                    return@map "$condition -> ${properties.getTypeName()}($nonDefaultInitializer$defaultInitializer)"
                }
                .forEach { builder.addStatement(it.wrapProof()) }
            builder.addStatement(
                "else -> ${properties.getTypeName()}(${nonDefaultParameters.joinToString(separator = ", ") { "${it.name} = ${it.name}" }})".wrapProof()
            )
            builder.endControlFlow()
        }
        return builder.build()
    }

    private fun <T> Collection<T>.powerset(): Set<Set<T>> = when {
        isEmpty() -> setOf(setOf())
        else -> drop(1).powerset().let { it + it.map { it + first() } }
    }
}