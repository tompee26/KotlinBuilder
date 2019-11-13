package com.tompee.kotlinbuilder.processor.di

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.processor.BuilderProcessor
import dagger.BindsInstance
import dagger.Component
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton

@Singleton
@Component(modules = [ProcessingModule::class])
@KotlinPoetMetadataPreview
internal interface AppComponent {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance
            environment: ProcessingEnvironment
        ): AppComponent
    }

    fun inject(processor: BuilderProcessor)
}