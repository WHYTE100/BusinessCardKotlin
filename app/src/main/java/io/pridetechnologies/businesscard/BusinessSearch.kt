package io.pridetechnologies.businesscard

import kotlinx.serialization.Serializable


@Serializable
data class BusinessSearch(
    val objectID: String,
    val business_logo: String,
    val business_name: String,
    val area_located: String,
    val district_name: String,
    val country: String
)