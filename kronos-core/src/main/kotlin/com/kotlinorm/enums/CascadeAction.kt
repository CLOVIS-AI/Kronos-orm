package com.kotlinorm.enums

/**
 * Enum class for cascade actions
 */
@Suppress("UNUSED_PARAMETER")
enum class CascadeAction(name: String) {
    CASCADE(Companion.CASCADE),
    RESTRICT(Companion.RESTRICT),
    SET_NULL(Companion.SET_NULL),
    NO_ACTION(Companion.NO_ACTION),
    SET_DEFAULT(Companion.SET_DEFAULT);

    object Companion {
        /**
         * CASCADE
         * Delete or update the row from the parent table, and automatically delete or update the matching rows in the child table.
         */
        const val CASCADE = "CASCADE"

        /**
         * RESTRICT
         * Rejects the delete or update operation for the parent table.
         */
        const val RESTRICT = "RESTRICT"

        /**
         * SET NULL
         * Delete or update the row from the parent table, and set the foreign key column or columns in the child table to NULL.
         */
        const val SET_NULL = "SET NULL"

        /**
         * NO ACTION
         * The default action, which is to restrict the deletion or update of the row from the parent table.
         */
        const val NO_ACTION = "NO ACTION"

        /**
         * SET DEFAULT
         * Delete or update the row from the parent table, and set the foreign key column or columns in the child table to the default value.
         */
        const val SET_DEFAULT = "SET DEFAULT"

        fun from(name: String): CascadeAction {
            return when (name) {
                CASCADE -> CascadeAction.CASCADE
                RESTRICT -> CascadeAction.RESTRICT
                SET_NULL -> CascadeAction.SET_NULL
                NO_ACTION -> CascadeAction.NO_ACTION
                SET_DEFAULT -> CascadeAction.SET_DEFAULT
                else -> throw IllegalArgumentException("Invalid cascade action: $name")
            }
        }
    }
}