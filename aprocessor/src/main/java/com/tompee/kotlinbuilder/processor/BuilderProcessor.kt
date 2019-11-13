package com.tompee.kotlinbuilder.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.di.AppComponent
import com.tompee.kotlinbuilder.processor.di.DaggerAppComponent
import com.tompee.kotlinbuilder.processor.processor.BuilderGenerator
import com.tompee.kotlinbuilder.processor.processor.ProviderMap
import com.tompee.kotlinbuilder.processor.processor.ProviderProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.*
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@KotlinPoetMetadataPreview
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
internal class BuilderProcessor : AbstractProcessor() {

    private lateinit var appComponent: AppComponent

    @Inject
    lateinit var providerProcessor: ProviderProcessor

    @Inject
    lateinit var generatorFactory: BuilderGenerator.Factory

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            KBuilder::class.java.name,
            Optional::class.java.name,
            Optional.Nullable::class.java.name,
            Optional.ValueProvider::class.java.name,
            Optional.Default::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
        appComponent = DaggerAppComponent.factory().create(processingEnv)
        appComponent.inject(this)

        val providerMap = getProviderMap(env)

        env?.getElementsAnnotatedWith(KBuilder::class.java)?.forEach {
            generate(it, providerMap)
        }
        return true
    }

    private fun getProviderMap(env: RoundEnvironment?): ProviderMap {
        return try {
            val providerElements =
                env?.getElementsAnnotatedWith(Optional.Provides::class.java)?.toList()
                    ?: emptyList()
            providerProcessor.getProviderMap(providerElements)
        } catch (e: Throwable) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message)
            emptyMap()
        }
    }

    /**
     * Generates a KBuilder class from the element
     */
    private fun generate(element: Element, providerMap: ProviderMap) {
        try {
            generatorFactory.create(element, providerMap).generate()
        } catch (e: Throwable) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message, element)
        }
    }
}