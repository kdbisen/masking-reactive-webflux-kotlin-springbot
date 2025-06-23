package com.adyanta.iot.mask.model

data class User(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val mobile: String? = null,
    val address: Address? = null
) {
    data class Address(
        val city: String? = null,
        val state: String? = null,
        val pincode: String? = null
    )
}
