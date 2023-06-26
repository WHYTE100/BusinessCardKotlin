package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Individuals(
    val user_name: String?,
    val user_image: String?,
    val user_id: String?
): Parcelable {
    constructor() : this(
        "",
        "",
        ""
    )
}