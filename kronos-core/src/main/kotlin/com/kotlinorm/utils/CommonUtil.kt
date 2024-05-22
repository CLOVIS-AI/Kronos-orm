package com.kotlinorm.utils

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.serializeResolver
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaLocalDateTime


/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/7 16:01
 **/

fun setCommonStrategy(
    strategy: KronosCommonStrategy,
    timeStrategy: Boolean = false,
    deleted: Boolean = false,
    callBack: (field: Field, value: Any?) -> Unit
) {
    if (strategy.enabled) {
        if (timeStrategy) {
            val format = (strategy.config ?: defaultDateFormat).toString()
            callBack(strategy.field, currentDateTime(format))
        } else {
            callBack(strategy.field, 1.takeIf { deleted } ?: 0)
        }
    }
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = linkedSetOf<T>().apply { addAll(this@toLinkedSet) }

@OptIn(FormatStringsInDatetimeFormats::class)
@Suppress("UNUSED")
fun getSafeValue(
    kPojo: KPojo,
    kotlinType: String,
    superTypes: List<String>,
    map: Map<String, Any?>,
    key: String,
    useSerializeResolver: Boolean
): Any? {
    val column = kPojo.kronosColumns().find { it.name == key }!!

    fun getEpochSecond(): Long {
        return when (map[key]) {
            is Long -> map[key] as Long
            is Int -> (map[key] as Int).toLong()
            is Short -> (map[key] as Short).toLong()
            is Float -> (map[key] as Float).toLong()
            is Double -> (map[key] as Double).toLong()
            is Byte -> (map[key] as Byte).toLong()
            else -> LocalDateTime.parse(map[key].toString()).toJavaLocalDateTime()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().epochSecond
        }
    }
    val safeKey =
        if (map[key] != null || kPojo.kronosColumns().any { it.name == column.columnName }) key else column.columnName
    return when {
        map[safeKey] == null -> null
        else -> {
            val typeOfVal = map[safeKey]!!::class
            if (kotlinType != typeOfVal.qualifiedName) {
                if (useSerializeResolver) {
                    return serializeResolver.deserializeObj(map[safeKey].toString(), kPojo::class)
                }
                when (kotlinType) {
                    "kotlin.Int" -> map[safeKey].toString().toInt()
                    "kotlin.Long" -> map[safeKey].toString().toLong()
                    "kotlin.Short" -> map[safeKey].toString().toShort()
                    "kotlin.Float" -> map[safeKey].toString().toFloat()
                    "kotlin.Double" -> map[safeKey].toString().toDouble()
                    "kotlin.Byte" -> map[safeKey].toString().toByte()
                    "kotlin.Char" -> map[safeKey].toString().toCharArray()[0]
                    "kotlin.String" -> when {
                        (listOf(typeOfVal.qualifiedName) + typeOfVal.supertypes.map { it.toString() }).any {
                            it in listOf(
                                "java.util.Date",
                                "java.time.LocalDateTime",
                                "java.time.LocalDate",
                                "java.time.LocalTime"
                            )
                        } -> {
                            LocalDateTime.parse(map[safeKey].toString()).format(LocalDateTime.Format {
                                byUnicodePattern(column.dateFormat ?: defaultDateFormat)
                            })
                        }

                        else -> map[safeKey].toString()
                    }

                    "kotlin.Boolean" -> (map[safeKey] is Number && map[safeKey] as Number != 0) || map[safeKey].toString()
                        .toBoolean()

                    "java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime" -> {
                        val epochSecond = getEpochSecond()
                        when (kotlinType) {
                            "java.time.LocalDateTime" -> java.time.Instant.ofEpochSecond(epochSecond)
                                .atZone(java.time.ZoneId.systemDefault())

                            "java.time.LocalDate" -> java.time.Instant.ofEpochSecond(epochSecond)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                            "java.time.LocalTime" -> java.time.Instant.ofEpochSecond(epochSecond)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()

                            else -> map[safeKey]
                        }
                    }

                    "kotlinx.datetime.LocalDateTime", "kotlinx.datetime.LocalDate", "kotlinx.datetime.LocalTime" -> Instant.parse(
                        map[safeKey].toString()
                    )

                    else -> when {
                        "java.util.Date" in superTypes -> {
                            val epochSecond = getEpochSecond()
                            val constructor =
                                Class.forName(kotlinType).constructors.find { it.parameters.size == 1 && it.parameterTypes[0] == Long::class.java }!!
                            constructor.newInstance(epochSecond * 1000)
                        }

                        else -> map[safeKey]

                    }
                }
            } else {
                map[safeKey]
            }
        }
    }
    //TODO:
    // 1.类型转换 Any->String,Long->Int, Short->Int, Int->Short, Int->Boolean...
    // 2.日期转换 String->Date, Long->Date, String-> LocalDateTime, Long->LocalDateTime
    // 3.将String使用serialize resolver转为指定类型
    // 4.若columnLabel在map中的值为null，尝试查找columnName在map中的值存入KPojo
}