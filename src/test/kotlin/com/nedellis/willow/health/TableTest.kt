package com.nedellis.willow.health

import com.nedellis.willow.Table
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.URI

class TableTest : StringSpec({
    "Single entry with empty merge" {
        val first = Table.newBuilder().putEntries("a", 1).build()
        val second = Table.newBuilder().build()
        first.merge(second).entriesMap shouldBe mapOf("a" to 1)
    }

    "Empty entry with single merge" {
        val first = Table.newBuilder().putEntries("a", 1).build()
        val second = Table.newBuilder().build()
        second.merge(first).entriesMap shouldBe mapOf("a" to 1)
    }

    "Single entry with Single merge" {
        val first = Table.newBuilder().putEntries("a", 1).build()
        val second = Table.newBuilder().putEntries("a", 2).build()
        first.merge(second).entriesMap shouldBe mapOf("a" to 2)
    }

    "Increment Self" {
        val table = Table.newBuilder().putEntries("a", 1).build()
        table.incrementSelf(URI("a")).entriesMap shouldBe mapOf("a" to 2)
    }

    "Stale with nonexistent old value keeps existent" {
        val table = Table.newBuilder().putEntries("a", 0).build()
        val old = Table.getDefaultInstance()
        table.stale(old) shouldBe mapOf()
    }

    "Stale with equivalent old value removes" {
        val table = Table.newBuilder().putEntries("a", 0).build()
        val old = Table.newBuilder().putEntries("a", 0).build()
        table.stale(old) shouldBe mapOf("a" to 0)
    }

    "Stale with older old value keeps existent" {
        val table = Table.newBuilder().putEntries("a", 1).build()
        val old = Table.newBuilder().putEntries("a", 0).build()
        table.stale(old) shouldBe mapOf()
    }

    "Prune correctly deletes a stale key" {
        val table = Table.newBuilder().putEntries("a", 0).build()
        val marked = mapOf("a" to 0L)
        table.prune(marked).entriesMap shouldBe mapOf()
    }

    "Prune does nothing if key is not present in marked" {
        val table = Table.newBuilder().putEntries("a", 0).build()
        val marked = mapOf("b" to 0L)
        table.prune(marked).entriesMap shouldBe mapOf("a" to 0L)
    }

    "Prune does nothing if key is updated" {
        val table = Table.newBuilder().putEntries("a", 1).build()
        val marked = mapOf("a" to 0L)
        table.prune(marked).entriesMap shouldBe mapOf("a" to 1L)
    }

    "MarkAndPrune works for [stale, stale, stale]" {
        val table = Table.newBuilder().putEntries("a", 0).build()
        val newer = Table.newBuilder().putEntries("a", 0).build()
        val older = Table.newBuilder().putEntries("a", 0).build()
        table.markAndPrune(newer, older).entriesMap shouldBe mapOf()
    }

    "MarkAndPrune works for [stale, stale, new]" {
        val table = Table.newBuilder().putEntries("a", 1).build()
        val newer = Table.newBuilder().putEntries("a", 0).build()
        val older = Table.newBuilder().putEntries("a", 0).build()
        table.markAndPrune(newer, older).entriesMap shouldBe mapOf("a" to 1L)
    }

    "MarkAndPrune works for [stale, stale, no]" {
        val table = Table.newBuilder().putEntries("b", 0).build()
        val newer = Table.newBuilder().putEntries("a", 0).build()
        val older = Table.newBuilder().putEntries("a", 0).build()
        table.markAndPrune(newer, older).entriesMap shouldBe mapOf("b" to 0L)
    }
})
