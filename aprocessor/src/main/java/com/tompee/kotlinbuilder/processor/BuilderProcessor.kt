package com.tompee.kotlinbuilder.processor

import com.google.auto.service.AutoService
import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.Nullable
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Provider
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
class BuilderProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            Builder::class.java.name,
            Optional::class.java.name,
            Nullable::class.java.name,
            Provider::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
        env?.getElementsAnnotatedWith(Builder::class.java)?.forEach(this::generate)
        return true
    }

    /**
     * Generates a Builder class from the element
     */
    private fun generate(element: Element) {
        BuilderGenerator(processingEnv, element).generate()
    }
}