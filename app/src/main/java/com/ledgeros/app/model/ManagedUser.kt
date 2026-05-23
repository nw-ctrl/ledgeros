package com.ledgeros.app.model

data class ManagedUser(
    val email: String,
    val tier: GrantedTier,
    val note: String = "",
)

/**
 * Access tier that the owner has manually granted to a user.
 *
 * [originalPriceLabel] is the retail price shown with a strikethrough on the
 * grantee's Settings screen so they can see the value of their complimentary access.
 * Keep these strings in sync with [com.ledgeros.app.ui.state.PremiumTier].
 */
enum class GrantedTier(
    val label: String,
    val originalPriceLabel: String?,
) {
    Free("Free", null),
    ProMonthly("Pro Monthly", "A\$7.99/month"),
    ProYearly("Pro Yearly", "A\$59.99/year"),
}
