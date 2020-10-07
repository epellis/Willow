package com.nedellis.willow.health

import com.nedellis.willow.Table
import java.net.URI
import kotlin.math.max

/**
 * Update this table with the other's entries.
 * If the entry is in both tables, keep the higher value
 */
fun Table.merge(other: Table): Table {
    val ours = this.entriesMap.toMap()
    val theirs = other.entriesMap.toMap()
    val both = (ours.keys + theirs.keys)
        .map { key -> Pair(key, max(ours.getOrDefault(key, 0), theirs.getOrDefault(key, 0))) }
        .toMap()
    return Table.newBuilder().putAllEntries(both).build()
}

fun Table.incrementSelf(localAddress: URI): Table {
    val ours = this.entriesMap.toMutableMap()
    ours[localAddress.toString()] = ours[localAddress.toString()]?.inc()
    return Table.newBuilder().putAllEntries(ours).build()
}

fun Table.randomNeighbor(localAddress: URI): URI? {
    return this.entriesMap.keys.filter { it != localAddress.toString() }.randomOrNull()?.let { URI(it) }
}

/**
 * Return all elements in [this] that have equal or less heartbeat than their entries in [old]
 */
fun Table.stale(old: Table): Map<String, Long> {
    return this.entriesMap.toMap().filter { it.value <= old.entriesMap[it.key] ?: -1 }
}

/**
 * Return a copy of [this] with all elements that:
 * - Are not in [marked]
 * OR
 * - Are in [marked] but have a higher value
 */
fun Table.prune(marked: Map<String, Long>): Table {
    val new = this.entriesMap.toMap().filter { !marked.containsKey(it.key) || it.value > marked[it.key] ?: -1 }
    return Table.newBuilder().putAllEntries(new).build()
}

fun Table.markAndPrune(newer: Table, older: Table): Table {
    val marked = newer.stale(older)
    return this.prune(marked)
}
