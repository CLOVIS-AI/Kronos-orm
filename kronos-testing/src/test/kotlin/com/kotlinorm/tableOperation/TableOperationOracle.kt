package com.kotlinorm.tableOperation

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.tableOperation.beans.OracleUser
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 此类演示了如何使用KotlinORM进行数据库表的操作，包括查询表是否存在、动态创建表、删除表以及结构同步。
 * 通过与数据库交互，实现了基于实体类的表管理功能。
 */
class TableOperationOracle {

    // 初始化oracle数据库连接池
    private val ds = BasicDataSource().apply {
        driverClassName = "oracle.jdbc.OracleDriver" // Oracle驱动类名
        url = "jdbc:oracle:thin:@localhost:1521/FREEPDB1" // Oracle数据库URL
        username = "sdy" // Oracle用户名
        password = "********" // Oracle密码
        maxIdle = 10 // 最大空闲连接数
    }
    val user = OracleUser()

    init {
        // 配置Kronos ORM框架的基本设置
        Kronos.apply {
            // 设置字段命名策略为驼峰命名
            fieldNamingStrategy = LineHumpNamingStrategy
            // 设置表命名策略为驼峰命名
            tableNamingStrategy = LineHumpNamingStrategy
            // 设置数据源提供器
            dataSource = { KronosBasicWrapper(ds) }
        }
    }


    /**
     * 测试表是否存在功能。
     * 此方法应完成一个测试用例，验证某个表是否存在，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testExists() {
//        // 不管有没有先删
//        dataSource.table.dropTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(false, exists)
//        // 创建表
//        dataSource.table.createTable(user)
//        // 判断表是否存在
//        val exists2 = dataSource.table.exists(user)
//        assertEquals(exists2, true)
    }

    /**
     * 测试oracle动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_oracle() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = getTableColumns(dataSource(), "TB_USER")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns().map {
            it.copy(it.columnName.uppercase())
        }

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                columnCreateDefSql(DBType.Oracle, column),
                columnCreateDefSql(DBType.Oracle, actualColumn),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
    }

    /**
     * 测试删除表功能。
     * 此方法应完成一个测试用例，删除某个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testDropTable() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)
        // 删除表
        dataSource.table.dropTable(user)
        // 判断表是否存在
        val exists2 = dataSource.table.exists(user)
        assertEquals(exists2, false)
    }


    /**
     * 测试oracle结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncScheme_oracle() {
        // 同步user表结构
        val tableSync = dataSource.table.syncTable(user)
        if (!tableSync) {
            println("表结构相同无需同步")
        }

        // 索引
//        val list = user.kronosTableIndex()

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns().map {
            it.columnName = it.columnName.uppercase()
            it
        }

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        val actualColumns = getTableColumns(dataSource(), "TB_USER")

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                columnCreateDefSql(DBType.Oracle, actualColumn),
                columnCreateDefSql(DBType.Oracle, column),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    /**
     * 测试获取自增主键
     */
    @Test
    fun testLastInsertId() {
        val newUser = OracleUser(
            username = "yf",
            gender = 93
        )
        val (_, lastInsertId) = newUser.insert().execute()
        if (lastInsertId != null) {
            println("插入成功，新记录的ID为：$lastInsertId")
        } else {
            println("插入失败")
        }
    }
}