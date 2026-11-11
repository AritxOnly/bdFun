package com.aritxonly.bdfun.core.model.user

/** Relationship here always belongs to the logged-in account, never the viewed space owner. */
data class RelationUser(
    val mid: Long, val name: String, val face: String, val sign: String,
    val attribute: Int? = null,
) {
    val followed: Boolean get() = attribute == 2 || attribute == 6
}
