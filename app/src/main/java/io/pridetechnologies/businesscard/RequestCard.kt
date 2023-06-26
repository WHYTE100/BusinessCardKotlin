package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RequestCard(
    val sender_name: String?,
    val sender_id: String?,
    val sender_profession: String?,
    val sender_image: String?,
    val is_approved: Boolean?,
    ) : Parcelable {
        constructor() : this(
        "",
        "",
        "",
        "",
        false
        ) }
