package com.kotlinorm.plugins.utils

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.utils.kTable.UseSerializeResolverAnnotationsFqName
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*

context(IrPluginContext)
val createPairSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createPair")
        .first()

context(IrPluginContext)
val createMutableMapSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createMutableMap")
        .first()

context(IrPluginContext)
val createFieldListSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createFieldList")
        .first()

context(IrPluginContext)
val createStringListSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createStringList")
        .first()

context(IrPluginContext)
private val getSafeValueSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "getSafeValue")
        .first()

context(IrPluginContext)
private val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrPluginContext)
private val mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")

/**
 * Creates a new IrBlockBody that represents a function that converts an instance of an IrClass
 * to a mutable map. The function takes in an IrClass and an IrFunction as parameters.
 *
 * @param declaration The IrClass to be converted to a map.
 * @param irFunction The IrFunction that contains the instance of the IrClass.
 * @return The IrBlockBody that represents the function.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createToMapFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    return irBlockBody {
        val pairs = declaration.properties.map {
            applyIrCall(
                createPairSymbol,
                irString(it.name.asString()),
                irGetField(
                    irGet(irFunction.dispatchReceiverParameter!!),
                    it.backingField!!
                )
            )
        }.toList()
        +irReturn(
            applyIrCall(
                createMutableMapSymbol, irVararg(
                    createPairSymbol.owner.returnType,
                    pairs
                )
            )
        )
    }
}

/**
 * Creates an IrBlockBody that sets the properties of an IrClass instance using values from a map.
 *
 * @param declaration the IrClass instance whose properties will be set
 * @param irFunction the IrFunction that contains the map parameter
 * @return an IrBlockBody that sets the properties of the IrClass instance using values from the map
 */
context(IrBuilderWithScope, IrPluginContext)
fun createFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    val map = irFunction.valueParameters.first()
    return irBlockBody {
        declaration.properties.forEach {
            +irSetField(
                irGet(irFunction.dispatchReceiverParameter!!),
                it.backingField!!,
                applyIrCall(
                    mapGetterSymbol!!,
                    irString(it.name.asString())
                ) {
                    dispatchBy(irGet(map))
                }
            )
        }

        +irReturn(
            irGet(irFunction.dispatchReceiverParameter!!)
        )
    }
}

/**
 * Creates a safe from map value function.
 *
 * @param declaration The IrClass declaration.
 * @param irFunction The IrFunction to create the safe from map value function for.
 * @return An IrBlockBody containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createSafeFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    val map = irFunction.valueParameters.first()
    return irBlockBody {
        declaration.properties.forEach {
            +irTry(
                irUnit().type,
                irSetField(
                    irGet(irFunction.dispatchReceiverParameter!!),
                    it.backingField!!,
                    applyIrCall(
                        getSafeValueSymbol,
                        irGet(irFunction.dispatchReceiverParameter!!),
                        irString(it.backingField!!.type.classFqName!!.asString()),
                        applyIrCall(
                            createStringListSymbol,
                            irVararg(
                                irBuiltIns.stringType,
                                it.backingField!!.type.getClass()!!.superTypes.map { type ->
                                    irString(type.getClass()!!.kotlinFqName.asString())
                                }
                            )
                        ),
                        irGet(map),
                        irString(it.name.asString()),
                        irBoolean(it.hasAnnotation(UseSerializeResolverAnnotationsFqName))
                    )
                ),
                listOf(),
                null
            )
        }

        +irReturn(
            irGet(irFunction.dispatchReceiverParameter!!)
        )
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getTableName function with the given IrClass declaration as an argument.
 *
 * @param declaration the IrClass declaration to generate the table name for
 * @return an IrBlockBody containing an IrReturn statement with the generated table name
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosTableName(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getTableName(declaration)
        )
    }
}

/**
 * Creates a safe from map value function for the given declaration and irFunction.
 *
 * @param declaration The IrClass declaration.
 * @return An IrBlockBody containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createGetFieldsFunction(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            applyIrCall(
                createFieldListSymbol, irVararg(
                    fieldSymbol.owner.defaultType,
                    declaration.properties.map {
                        getColumnName(it)
                    }.toList()
                )
            )
        )
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalCreateTimeSymbol, and
 * CreateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return An IrBlockBody containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosCreateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalCreateTimeSymbol, CreateTimeFqName)
        )
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalUpdateTimeSymbol, and
 * UpdateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return An IrBlockBody containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosUpdateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalUpdateTimeSymbol, UpdateTimeFqName)
        )
    }
}

/**
 * Creates a function that returns a block body containing an irCall to createFieldListSymbol
 * with the properties of the given IrClass as arguments.
 *
 * @param declaration the IrClass whose properties will be used as arguments for createFieldListSymbol
 * @return an IrBlockBody containing an irCall to createFieldListSymbol
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosLogicDelete(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalLogicDeleteSymbol, LogicDeleteFqName)
        )
    }
}