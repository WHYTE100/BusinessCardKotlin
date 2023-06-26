package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Team(
    val user_image: String?,
    val user_name: String?,
    val user_position: String?,
    val member_id: String?,
    val is_admin: Boolean?
): Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        false
    ) }