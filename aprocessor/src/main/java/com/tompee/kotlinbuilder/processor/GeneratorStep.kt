package com.tompee.kotlinbuilder.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.extensions.*
import com.tompee.kotlinbuilder.processor.models.DefaultParameter
import com.tompee.kotlinbuilder.processor.models.Parameter
import com.tompee.kotlinbuilder.processor.parser.ParameterParser
import com.tompee.kotlinbuilder.processor.processor.ProviderProcessor
import javax.annotation.Generated
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

    private fun generate(element: TypeElement, providers: List<Element>) {
        try {
            val packageName = element.getPackageName(elements)
            val builderName = element.parseAnnotation<KBuilder>()?.name?.let {
                val builderName = if (it.isEmpty()) "${element.simpleName}Builder" else it
                ClassName(packageName, builderName)
            } ?: throw ProcessorException(element, "KBuilder annotation not found")

            val fileSpec = FileSpec.builder(packageName, builderName.toString())
                .addType(
                    buildClassSpec(
                        element,
                        builderName,
                        providers,
                        elements,
                        types
                    )
                )
                .build()
            fileSpec.writeTo(filer)
        } catch (e: ProcessorException) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.message, e.element)
        } catch (e: Throwable) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.message, element)
        }
    }

    /**
     * Builds the class spec. Also adds properties to be able to set constructor parameter's
     * modifier
     */
    private fun buildClassSpec(
        element: TypeElement,
        builderName: ClassName,
        providers: List<Element>,
        elements: Elements,
        types: Types
    ): TypeSpec {
        if (element.isPrivate) throw ProcessorException(element, "Class is defined as private")

        val providerMap = ProviderProcessor(elements, types).getProviderMap(providers)
        val parameterParser = ParameterParser(providerMap, elements, types)
        val parameterList = parameterParser.parse(element)

        val classSpecBuilder = TypeSpec.classBuilder(builderName)
            .addOriginatingElement(element)
            .addAnnotation(generatedAnnotation())
            .primaryConstructor(buildConstructor(parameterList))
            .addType(createCompanionObject(element, builderName, parameterList))
            .addProperties(parameterList.map { it.toPropertySpec() })
            .addFunctions(parameterList.map { it.toBuilderFunSpec(builderName) })
            .addFunction(createBuildMethod(element, parameterList))
        if (element.isInternal) classSpecBuilder.addModifiers(KModifier.INTERNAL)
        return classSpecBuilder.build()
    }

    /**
     * Adds a Generated annotation to the class
     */
    private fun generatedAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(Generated::class.asClassName())
            .addMember("%S", BuilderProcessor::class.java.canonicalName)
            .addMember("comments = %S", "https://github.com/tompee26/KotlinBuilder")
            .build()
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
        element: TypeElement,
        builderName: ClassName,
        parameterList: List<Parameter>,
    ): TypeSpec {
        //region First invoke overload
        val createOverload = FunSpec.builder("invoke")
            .addModifiers(KModifier.INTERNAL, KModifier.OPERATOR, KModifier.INLINE)
            .returns(element.className)
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
        element: TypeElement,
        parameterList: List<Parameter>,
    ): FunSpec {
        val className = element.className
        val builder = FunSpec.builder("build")
            .returns(className)

        /**
         * Add initializers
         */
        parameterList.mapNotNull { it.toBuildInitializer() }
            .forEach { builder.addStatement(it) }

        val defaultParameters = parameterList.filterIsInstance<DefaultParameter>()
        if (defaultParameters.isEmpty()) {
            builder.addStatement(
                "return ${className}(${
                    parameterList.joinToString(
                        separator = ", "
                    ) { it.name }
                })".wrapProof()
            )
        } else {
            val nonDefaultParameters = parameterList.filterNot { it is DefaultParameter }

            builder.beginControlFlow("return when")
            defaultParameters.powerSet().filterNot { it.isEmpty() }
                .sortedByDescending { it.count() }
                .map { params ->
                    val condition = params.joinToString(separator = " && ") { "${it.name} != null" }
                    val nonDefaultInitializer = if (nonDefaultParameters.isEmpty()) ""
                    else nonDefaultParameters.joinToString(
                        separator = ", ", postfix = ", "
                    ) { "${it.name} = ${it.name}" }
                    val defaultInitializer =
                        params.joinToString(separator = ", ") { "${it.name} = ${it.name}!!" }
                    return@map "$condition -> ${className}($nonDefaultInitializer$defaultInitializer)"
                }
                .forEach { builder.addStatement(it.wrapProof()) }
            builder.addStatement(
                "else -> ${className}(${nonDefaultParameters.joinToString(separator = ", ") { "${it.name} = ${it.name}" }})".wrapProof()
            )
            builder.endControlFlow()
        }
        return builder.build()
    }

    private fun <T> Collection<T>.powerSet(): Set<Set<T>> = when {
        isEmpty() -> setOf(setOf())
        else -> drop(1).powerSet().let { set -> set + set.map { it + first() } }
    }
}