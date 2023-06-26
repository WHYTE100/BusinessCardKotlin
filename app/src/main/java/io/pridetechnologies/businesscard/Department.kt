package io.pridetechnologies.businesscard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Department(
    val department_name: String?,
    val department_desc: String?,
    val department_email: String?,
    val department_mobile: String?,
    val department_whatsapp: String?,
    val department_id: String?
): Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        ""
    ) }
