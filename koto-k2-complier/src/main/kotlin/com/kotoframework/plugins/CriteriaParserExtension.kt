package com.kotoframework.plugins

import com.kotoframework.plugins.transformer.CriteriaParserTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*

class CriteriaParserExtension(
    val ignoreWarnings: Boolean
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(CriteriaParserTransformer(pluginContext), null)
        print(
            moduleFragment.dumpKotlinLike(
                KotlinLikeDumpOptions(
                    CustomKotlinLikeDumpStrategy.Default,
                    printRegionsPerFile = true,
                    printFileName = true,
                    printFilePath = true,
                    useNamedArguments = true,
                    labelPrintingStrategy = LabelPrintingStrategy.ALWAYS,
                    printFakeOverridesStrategy = FakeOverridesStrategy.ALL,
                    bodyPrintingStrategy = BodyPrintingStrategy.PRINT_BODIES,
                    printElseAsTrue = false,
                    printUnitReturnType = true,
                    stableOrder = true
                )
            )
//            moduleFragment.dump()
        )
    }
}