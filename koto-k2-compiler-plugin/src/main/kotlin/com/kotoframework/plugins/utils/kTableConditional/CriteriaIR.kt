package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.scopes.KotoBuildScope
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Describes a condition for constructing an IR, which can be used to specify how parts of the IR should be built based on certain criteria.
 * 描述一个IR的构建条件，可以用于指定基于某些条件如何构建IR的部分。
 */
class CriteriaIR(
    // The name of the parameter
    // 参数的名称
    var parameterName: IrExpression? = null,
    // The type of the criterion
    // 条件的类型
    var type: String,
    // Whether the condition is negated
    // 是否对条件进行否定
    var not: Boolean,
    // The value to compare with, optional
    // 用于比较的值，可选
    val value: IrExpression? = null,
    // List of child variables, optional
    // 子变量列表，可选
    val children: List<IrVariable> = listOf(),
    // The name of the table, optional
    // 表的名称，可选
    var tableName: IrExpression? = null,
) {
    context(KotoBuildScope)
    fun toIrVariable(): IrVariable {
        return createCriteria(parameterName, type, not, value, children, tableName)
    }
}