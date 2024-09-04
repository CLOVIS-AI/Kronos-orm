/**
* Copyright 2022-2024 kronos-orm
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.orm.pagination.PagedClause
import com.kotlinorm.utils.toLinkedSet

// Generated by generate-join-clause.gradle.kts
class SelectFrom7<T1: KPojo, T2: KPojo, T3: KPojo, T4: KPojo, T5: KPojo, T6: KPojo, T7: KPojo>(
    override var t1: T1,
    var t2: T2, var t3: T3, var t4: T4, var t5: T5, var t6: T6, var t7: T7
) : SelectFrom<T1>(t1) {
    override var tableName = t1.kronosTableName()
    override var paramMap = (t1.toDataMap() + t2.toDataMap() + t3.toDataMap() + t4.toDataMap() + t5.toDataMap() + t6.toDataMap() + t7.toDataMap()).toMutableMap()
    override var logicDeleteStrategy = t1.kronosLogicDelete()
    override var allFields = t1.kronosColumns().filter { it.isColumn }.toLinkedSet()
    override var listOfPojo: MutableList<KPojo> = mutableListOf(t1, t2, t3, t4, t5, t6, t7)
    
    fun withTotal(): PagedClause<T1, SelectFrom7<T1, T2, T3, T4, T5, T6, T7>> {
        return PagedClause(this)
    }
}