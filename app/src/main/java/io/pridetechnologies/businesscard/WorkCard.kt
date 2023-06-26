package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkCard(
    val business_logo: String?,
    val business_name: String?,
    val business_id: String?,
    val user_position: String?,
    val admin: Boolean
    ) : Parcelable {
        constructor() : this(
        "",
        "",
        "",
        "",
        false
        ) }
