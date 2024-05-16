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

package com.kotlinorm.orm.update

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.update(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, true, setUpdateFields)
}

inline fun <reified T : KPojo> Iterable<T>.update(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, false, setUpdateFields) }
}

//  添加测试用例
inline fun <reified T : KPojo> Iterable<T>.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, true, setUpdateFields) }
}

inline fun <reified T : KPojo> Array<T>.update(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, false, setUpdateFields) }
}

//  添加测试用例
inline fun <reified T : KPojo> Array<T>.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, true, setUpdateFields) }
}
