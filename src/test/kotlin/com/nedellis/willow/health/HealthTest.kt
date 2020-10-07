package com.nedellis.willow.health

import com.google.protobuf.Empty
import com.nedellis.willow.Table
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class HealthTest : StringSpec({
    val localAddress = URI("http://127.0.0.1:8000")

    "Newly created service has all members at 0" {
        val members = setOf(localAddress, URI("http://127.0.0.1:8001"))
        val mockHealthRpc = mockk<HealthRpc>()
        val service = HealthService(HealthConfig(localAddress, members), mockHealthRpc)
        service.table.entriesMap shouldBe members.map { it.toString() to 0L }.toMap()
        service.tableHistory.size shouldBe 0
    }

    "Advance" {
        val members = setOf(localAddress, URI("http://127.0.0.1:8001"))
        val mockHealthRpc = mockk<HealthRpc>()
        val service = HealthService(HealthConfig(localAddress, members), mockHealthRpc)

        coEvery { mockHealthRpc.update(URI("http://127.0.0.1:8001"), any()) } returns Empty.getDefaultInstance()

        service.advance()
        service.table.entriesMap[localAddress.toString()] shouldBe 1L
        service.tableHistory.size shouldBe 1
        service.tableHistory.last().entriesMap shouldBe members.map { it.toString() to 0L }.toMap()

        coVerify { mockHealthRpc.update(URI("http://127.0.0.1:8001"), any()) }
    }

    "Neighbor gossips heartbeat change" {
        val members = setOf(localAddress, URI("http://127.0.0.1:8001"))
        val mockHealthRpc = mockk<HealthRpc>()
        val service = HealthService(HealthConfig(localAddress, members), mockHealthRpc)

        val otherTable = Table.newBuilder()
            .putEntries(localAddress.toString(), 0L)
            .putEntries("http://127.0.0.1:8001", 1L)
            .build()

        service.update(otherTable)
        service.table.entriesMap shouldBe otherTable.entriesMap
        service.tableHistory.size shouldBe 0
    }

    "Table updates are serialized" {
        val members = setOf(localAddress)
        val mockHealthRpc = mockk<HealthRpc>()
        val service = HealthService(HealthConfig(localAddress, members), mockHealthRpc)

        launch {
            withContext(Dispatchers.Default) {
                for (i in 0 until 1000) {
                    launch { service.advance() }
                }
            }
        }.join()

        service.table.entriesMap shouldBe mapOf(localAddress.toString() to 1000L)
    }
})
