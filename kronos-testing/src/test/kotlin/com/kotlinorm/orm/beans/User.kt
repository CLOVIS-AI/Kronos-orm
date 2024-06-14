package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.Mysql
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
@TableIndex(name = "idx_multi", columns = ["id", "username"], type = "UNIQUE", method = "BTREE")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    @Column("gender")
    @ColumnType(CHAR)
    @Default("0")
    var gender: Int? = null,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @NotNull
    var createTime: String? = null,
    @UpdateTime
    @NotNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NotNull
    var deleted: Boolean? = null
) : KPojo()