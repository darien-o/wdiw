package io.github.darieno.wdiw.domain

/** "v1.4.0" and "1.4.0-rc1" both read as [1, 4, 0]. Null when there is no leading number. */
private fun versionParts(version: String): List<Int>? =
    version.trim().removePrefix("v").substringBefore('-').split('.')
        .map { it.toIntOrNull() ?: return null }
        .takeIf { it.isNotEmpty() }

/** True when [candidate] is a later release than [current]. Unreadable versions are never newer. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = versionParts(candidate) ?: return false
    val b = versionParts(current) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val difference = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (difference != 0) return difference > 0
    }
    return false
}
