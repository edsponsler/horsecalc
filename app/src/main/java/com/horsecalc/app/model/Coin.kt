package com.horsecalc.app.model

/**
 * Supported US coin denominations in the Horse Racing game pot.
 */
enum class Coin(val valueCents: Int, val displayName: String, val pluralName: String) {
    QUARTER(25, "Quarter", "Quarters"),
    DIME(10, "Dime", "Dimes"),
    NICKEL(5, "Nickel", "Nickels");

    fun format(cents: Int): String {
        return "$${String.format("%.2f", cents / 100.0)}"
    }
}
