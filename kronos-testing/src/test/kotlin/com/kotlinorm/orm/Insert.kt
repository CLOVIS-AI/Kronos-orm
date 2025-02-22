package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.strategies.LineHumpNamingStrategy
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.tableOperation.beans.MysqlUser
import kotlin.test.Test
import kotlin.test.assertEquals

class Insert {
    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }

    val user = MysqlUser(1)
    private val testUser = MysqlUser(1, "test")

    @Test
    fun testInsert() {
        val (sql, paramMap) = user.insert().build()
        assertEquals(
            "INSERT INTO `tb_user` (`id`, `create_time`, `update_time`, `deleted`) VALUES (:id, :createTime, :updateTime, :deleted)",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
                "deleted" to 0
            ), paramMap
        )
    }

}