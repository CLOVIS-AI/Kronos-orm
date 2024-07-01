package com.kotlinorm.sql.sqlite

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.sql.SqlManager.sqlColumnType

object SqliteSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "TINYINT(1)"
            KColumnType.TINYINT -> "TINYINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INT"
            KColumnType.MEDIUMINT -> "MEDIUMINT"
            KColumnType.BIGINT -> "BIGINT"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "TEXT"
            KColumnType.MEDIUMTEXT -> "MEDIUMTEXT"
            KColumnType.LONGTEXT -> "LONGTEXT"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "TIME"
            KColumnType.DATETIME -> "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BINARY"
            KColumnType.VARBINARY -> "VARBINARY"
            KColumnType.LONGVARBINARY -> "LONGBLOB"
            KColumnType.BLOB -> "BLOB"
            KColumnType.MEDIUMBLOB -> "MEDIUMBLOB"
            KColumnType.LONGBLOB -> "LONGBLOB"
            KColumnType.CLOB -> "CLOB"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NCLOB"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "INT"
            KColumnType.YEAR -> "YEAR"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "TEXT"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String =
        "${
            column.columnName
        }${
            " ${sqlColumnType(dbType, column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " AUTOINCREMENT" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"

    // 生成SQLite的列定义字符串
    // 索引 CREATE INDEX "dfsdf"
    //ON "_tb_user_old_20240617" (
    //  "password"
    //);
    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String {
        return "CREATE ${index.method} INDEX IF NOT EXISTS ${index.name} ON $tableName (${
            index.columns.joinToString(",") { column ->
                if (index.type.isNotEmpty())
                    "$column COLLATE ${index.type}"
                else
                    column
            }
        });"
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "SELECT COUNT(1)  as CNT FROM sqlite_master where type='table' and name= :tableName"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        fun extractNumberInParentheses(input: String): Int {
            val regex = Regex("""\((\d+)\)""") // 匹配括号内的数字部分
            val matchResult = regex.find(input)

            return matchResult?.groupValues?.get(1)?.toInt() ?: 0
        }
        return dataSource.forList(
            KronosAtomicQueryTask("PRAGMA table_info(:tableName)", mapOf("tableName" to tableName))
        ).map {
            Field(
                columnName = it["name"].toString(),
                type = KColumnType.fromString(it["type"].toString().split('(').first()), // 处理类型
                length = extractNumberInParentheses(it["type"].toString()), // 处理长度
                tableName = tableName,
                nullable = it["notnull"] as Int == 0, // 直接使用notnull字段判断是否可空
                primaryKey = it["pk"] as Int == 1,
                defaultValue = it["dflt_value"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name = :tableName",
                mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, emptyArray(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        return indexes.toDelete.map {
            "DROP INDEX ${it.name}"
        } + columns.toDelete.map {
            "ALTER TABLE $tableName ADD COLUMN ${getColumnCreateSql(dbType, it)}"
        } + columns.toModified.map {
            "ALTER TABLE $tableName MODIFY COLUMN ${getColumnCreateSql(dbType, it)}"
        } + columns.toDelete.map {
            "ALTER TABLE $tableName DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            // CREATE INDEX "aaa" ON "tb_user" ("username" COLLATE RTRIM )  如果${it.type}不是空 需要 在每个column后面加 COLLATE ${it.type} (${it.columns.joinToString(",")})需要改
            "CREATE ${it.method} INDEX ${it.name} ON $tableName (${
                it.columns.joinToString(",") { column ->
                    if (it.type.isNotEmpty()) "$column COLLATE ${it.type}"
                    else column
                }
            })"
        }
    }
}