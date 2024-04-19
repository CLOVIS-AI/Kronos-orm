package com.kotoframework.plugins

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class CriteriaParserCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val ignoreWarning = configuration.get(CriteriaCommandLineProcessor.ARG_OPTION_IGNORE_WARNING, true)
        IrGenerationExtension.registerExtension(CriteriaParserExtension(ignoreWarning))
    }

}