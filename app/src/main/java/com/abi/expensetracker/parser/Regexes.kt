package com.abi.expensetracker.parser

/** Named-group access. Returns null when the group is absent or did not participate. */
fun MatchResult.namedOrNull(name: String): String? = try {
    (groups as? MatchNamedGroupCollection)?.get(name)?.value?.trim()?.ifBlank { null }
} catch (e: IllegalArgumentException) {
    // Group name not defined in this pattern — normal, rules declare different groups.
    null
}
