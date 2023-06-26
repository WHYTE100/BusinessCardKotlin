package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Businesses(
    val business_name: String?,
    val business_logo: String?,
    val business_bio: String?,
    val business_qr_code: String?,
    val business_id: String?
): Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        ""
    )
}
