package com.tompee.kotlinbuilder.processor.di

import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import dagger.Module
import dagger.Provides
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@Module
@KotlinPoetMetadataPreview
internal object ProcessingModule {

    @Singleton
    @Provides
    @JvmStatic
    fun provideMessager(environment: ProcessingEnvironment): Messager {
        return environment.messager
    }

    @Singleton
    @Provides
    @JvmStatic
    fun provideElements(environment: ProcessingEnvironment): Elements {
        return environment.elementUtils
    }

    @Singleton
    @Provides
    @JvmStatic
    fun provideTypes(environment: ProcessingEnvironment): Types {
        return environment.typeUtils
    }

    @Singleton
    @Provides
    @JvmStatic
    fun provideClassInspector(elements: Elements, types: Types): ClassInspector {
        return ElementsClassInspector.create(elements, types)
    }
}