package com.ledgeros.app.model

data class Business(
    val id: String,
    val userId: String,
    val abn: String,
    val businessName: String,
    val gstRegistered: Boolean,
    val basFrequency: BasFrequency,
)

enum class BasFrequency(val label: String) {
    Monthly("Monthly"),
    Quarterly("Quarterly"),
    Annually("Annually"),
}
