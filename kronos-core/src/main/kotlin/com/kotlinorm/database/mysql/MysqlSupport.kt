package com.kotlinorm.database.mysql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import com.kotlinorm.utils.trimWhitespace

object MysqlSupport : DatabasesSupport {
    override var quotes = Pair("`", "`")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) =
        wrapper.url.split("?").first().split("//")[1].split("/").last()

    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT -> "TINYINT(1)"
            TINYINT -> "TINYINT"
            SMALLINT -> "SMALLINT"
            INT, SERIAL -> "INT"
            MEDIUMINT -> "MEDIUMINT"
            BIGINT -> "BIGINT"
            REAL -> "REAL"
            FLOAT -> "FLOAT"
            DOUBLE -> "DOUBLE"
            DECIMAL -> "DECIMAL"
            NUMERIC -> "NUMERIC"
            CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR, NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            TEXT, XML -> "TEXT"
            MEDIUMTEXT -> "MEDIUMTEXT"
            LONGTEXT -> "LONGTEXT"
            DATE -> "DATE"
            TIME -> "TIME"
            DATETIME -> "DATETIME"
            TIMESTAMP -> "TIMESTAMP"
            BINARY -> "BINARY"
            VARBINARY -> "VARBINARY"
            LONGVARBINARY, LONGBLOB -> "LONGBLOB"
            BLOB -> "BLOB"
            MEDIUMBLOB -> "MEDIUMBLOB"
            CLOB -> "CLOB"
            JSON -> "JSON"
            ENUM -> "ENUM"
            NCLOB -> "NCLOB"
            UUID -> "CHAR(36)"
            YEAR -> "YEAR"
            SET -> "SET"
            GEOMETRY -> "GEOMETRY"
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String =
        "${
            quote(column.columnName)
        }${
            " ${sqlColumnType(dbType, column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " AUTO_INCREMENT" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }${
            if (column.kDoc != null) " COMMENT '${column.kDoc}'" else ""
        }"

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type} INDEX ${index.name} ON ${quote(tableName)} (${index.columns.joinToString(",") { quote(it) }}) USING ${index.method.ifEmpty { "BTREE" }}"

    override fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String>  {
        //TODO: add Table#KDOC to comment support
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { indexCreateDefSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE IF NOT EXISTS ${quote(tableName)} ($columnsSql)",
            *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "SELECT COUNT(1) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :dbName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
        "TRUNCATE TABLE ${quote(tableName)}"

    override fun getTableDropSql(dbType: DBType, tableName: String) = "DROP TABLE IF EXISTS $tableName"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.COLUMN_NAME, 
                    c.DATA_TYPE, 
                    c.CHARACTER_MAXIMUM_LENGTH LENGTH, 
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    c.COLUMN_COMMENT,
                    (CASE WHEN c.EXTRA = 'auto_increment' THEN 'YES' ELSE 'NO' END) AS IDENTITY,
                    (CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END) AS PRIMARY_KEY
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                WHERE 
                 c.TABLE_SCHEMA = DATABASE() AND 
                 c.TABLE_NAME = :tableName
            """.trimWhitespace(), mapOf("tableName" to tableName)
            )
        ).map {
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(
                    DBType.Mysql, it["DATA_TYPE"].toString(), (it["LENGTH"] as Long? ?: 0).toInt()
                ),
                length = (it["LENGTH"] as Long? ?: 0).toInt(),
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = it["PRIMARY_KEY"] == "YES",
                identity = it["IDENTITY"] == "YES",
                defaultValue = it["COLUMN_DEFAULT"] as String?,
                kDoc = it["COLUMN_COMMENT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT DISTINCT
                    INDEX_NAME AS name
                FROM 
                 INFORMATION_SCHEMA.STATISTICS
                WHERE 
                 TABLE_SCHEMA = DATABASE() AND 
                 TABLE_NAME = :tableName AND 
                 INDEX_NAME != 'PRIMARY'  
                """.trimWhitespace(), mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String> {
        //TODO: add Table#KDOC to comment support
        return indexes.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP INDEX ${it.name}"
        } + columns.toAdd.map {
            "ALTER TABLE ${quote(tableName)} ADD COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it
                )
            }"
        } + columns.toModified.map {
            "ALTER TABLE ${quote(tableName)} MODIFY COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it
                ).replace(" PRIMARY KEY", "")
            } ${if (it.primaryKey) ", DROP PRIMARY KEY, ADD PRIMARY KEY (${quote(it)})" else ""}"
        } + columns.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP COLUMN ${quote(it)}"
        } + indexes.toAdd.map {
            "ALTER TABLE ${quote(tableName)} ADD ${it.type} INDEX ${it.name} (${
                it.columns.joinToString(", ") { f -> quote(f) }
            }) USING ${it.method}"
        }
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, _, toUpdateFields, toInsertFields) = conflictResolver
        return "INSERT INTO ${quote(tableName)} (${toInsertFields.joinToString { quote(it) }}) " + "VALUES (${
            toInsertFields.joinToString(
                ", "
            ) { ":$it" }
        }) " + "ON DUPLICATE KEY UPDATE ${toUpdateFields.joinToString(", ") { equation(it) }}"
    }

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO ${quote(tableName)} (${columns.joinToString { quote(it) }}) " + "VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?) =
        "DELETE FROM ${quote(tableName)}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ) =
        "UPDATE ${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                plusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} + :${it.second}" } +
                minusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} - :${it.second}" } +
                whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (databaseName, tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it.name)}"
                else -> quote(it)
            }
        }
        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        val lockSql = when (lock) {
            PessimisticLock.X -> " FOR UPDATE"
            PessimisticLock.S -> " LOCK IN SHARE MODE"
            else -> null
        }
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
            databaseName?.let { quote(it) + "." } ?: ""
        }${
            quote(tableName)
        }${
            whereClauseSql.orEmpty()
        }${
            groupByClauseSql.orEmpty()
        }${
            havingClauseSql.orEmpty()
        }${
            orderByClauseSql.orEmpty()
        }${
            paginationSql ?: limitSql ?: ""
        }${
            lockSql.orEmpty()
        }"
    }

    override fun getJoinSql(dataSource: KronosDataSourceWrapper, joinClause: JoinClauseInfo): String {
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, databaseOfTable, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql, joinSql) = joinClause

        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.second.type == CUSTOM_CRITERIA_SQL -> it.second.toString()
                it.second.name != it.second.columnName -> "${quote(it.second, true)} AS ${quote(it.second.name)}"
                else -> "${SqlManager.quote(dataSource, it.second, true, databaseOfTable)} AS ${quote(it.first)}"
            }
        }

        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
            SqlManager.quote(dataSource, tableName, true, map = databaseOfTable)
        }${
            joinSql.orEmpty()
        }${
            whereClauseSql.orEmpty()
        }${
            groupByClauseSql.orEmpty()
        }${
            havingClauseSql.orEmpty()
        }${
            orderByClauseSql.orEmpty()
        }${
            paginationSql ?: limitSql ?: ""
        }"
    }
}