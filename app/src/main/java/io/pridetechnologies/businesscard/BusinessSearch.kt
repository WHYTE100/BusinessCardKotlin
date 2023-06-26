package io.pridetechnologies.businesscard

import kotlinx.serialization.Serializable


@Serializable
data class BusinessSearch(
    val objectID: String,
    val business_name: String
)