package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.utils.toLinkedSet

class SelectFrom8<T1: KPojo, T2: KPojo, T3: KPojo, T4: KPojo, T5: KPojo, T6: KPojo, T7: KPojo, T8: KPojo>(
    override var t1: T1,
    var t2: T2, var t3: T3, var t4: T4, var t5: T5, var t6: T6, var t7: T7, var t8: T8
) : SelectFrom<T1>(t1) {
    override var tableName = t1.kronosTableName()
    override var paramMap = t1.toDataMap()
    override var logicDeleteStrategy = t1.kronosLogicDelete()
    override var allFields = t1.kronosColumns().toLinkedSet()
    
    fun withTotal(): PagedClause<T1, SelectFrom8<T1, T2, T3, T4, T5, T6, T7, T8>> {
        return PagedClause(this)
    }
}