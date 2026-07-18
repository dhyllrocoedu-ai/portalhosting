package com.portalhost.server.providers

import kotlin.math.max

data class SemverKey(val parts: List<Int>) : Comparable<SemverKey> {
    override fun compareTo(other: SemverKey): Int {
        val maxLen = max(parts.size, other.parts.size)
        for (i in 0 until maxLen) {
            val va = parts.getOrElse(i) { 0 }
            val vb = other.parts.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }
}

fun parseSemver(version: String): SemverKey {
    return SemverKey(
        version.trimStart('v').split(".").map { seg ->
            seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    )
}
