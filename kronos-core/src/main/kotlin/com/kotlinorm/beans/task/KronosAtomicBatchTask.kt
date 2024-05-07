package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KBatchTask

data class KronosAtomicBatchTask(
    override val sql: String,
    override val paramMapArr: Array<Map<String, Any?>>? = null,
    override val operationType: KOperationType
) : KAtomicTask, KBatchTask {

    @Deprecated("Please use 'paramMapArr' instead.")
    override val paramMap: Map<String, Any?> = mapOf()
    fun parsed() = (paramMapArr ?: arrayOf()).map { parseSqlStatement(sql, it) }.let {
        Pair(it.firstOrNull()?.jdbcSql, it.map { parsedSql ->  parsedSql.jdbcParamList })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KronosAtomicBatchTask

        if (sql != other.sql) return false
        if (paramMapArr != null) {
            if (other.paramMapArr == null) return false
            if (!paramMapArr.contentEquals(other.paramMapArr)) return false
        } else if (other.paramMapArr != null) return false
        if (operationType != other.operationType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + (paramMapArr?.contentHashCode() ?: 0)
        result = 31 * result + operationType.hashCode()
        return result
    }
}