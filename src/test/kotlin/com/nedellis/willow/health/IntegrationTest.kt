package com.nedellis.willow.health

import com.google.protobuf.Empty
import com.nedellis.willow.Table
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.URI


sealed class NetworkStatus
object Healthy : NetworkStatus()
object Fail : NetworkStatus()
data class PartialFail(val failedMembers: Set<URI>) : NetworkStatus()

private class LocalHealthRpc(var networkStatus: NetworkStatus = Healthy) : HealthRpc {
    val incomingMessages = mutableMapOf<URI, List<Table>>().withDefault { listOf() }
    // TODO: Find a way to make outgoingMessages

    lateinit var members: Map<URI, HealthService>

    override suspend fun update(dest: URI, request: Table): Empty {
        when (networkStatus) {
            is Healthy -> {
                incomingMessages[dest] = incomingMessages.getValue(dest).plus(listOf(request))
                members[dest]!!.update(request)
            }
            is Fail -> Unit
            is PartialFail -> {
                if (!(networkStatus as PartialFail).failedMembers.contains(dest)) {
                    incomingMessages[dest] = incomingMessages.getValue(dest).plus(listOf(request))
                    members[dest]!!.update(request)
                }
            }
        }
        return Empty.getDefaultInstance()
    }
}

class IntegrationTest : StringSpec({
    /**
     * Build initialized health services, connected to each other by [LocalHealthRpc]
     */
    fun buildServices(count: Int): Pair<List<HealthService>, LocalHealthRpc> {
        val healthRpc = LocalHealthRpc()

        val addreses = (8000 until 8000 + count).map { port ->
            URI("http://127.0.0.1:$port")
        }.toSet()

        val services = addreses.map { addr ->
            HealthService(HealthConfig(addr, addreses), healthRpc)
        }

        healthRpc.members = services.map { it.config.address to it }.toMap()

        return Pair(services, healthRpc)
    }

    "Connected Health System with One Node" {
        val (services, healthRpc) = buildServices(1)

        for (cycleCount in 0..100) {
            /**
             * Before Gossip: Health should be the number of current clock cycles
             * After Gossip: Health should be the number of current clock cycles + 1
             * Always (for one node): No messages should be sent to it
             */
            services.forEach {
                it.table.entriesMap shouldBe mapOf(it.config.address.toString() to cycleCount)
                it.incrementAndGossip()
                it.table.entriesMap shouldBe mapOf(it.config.address.toString() to cycleCount + 1L)
                healthRpc.incomingMessages.getValue(it.config.address).size shouldBe 0
            }
        }
    }

    "Connected Health System with Two Nodes" {
        val (services, healthRpc) = buildServices(2)

        for (cycleCount in 0..100) {
            // Before IncrementAndGossip()
            services.forEach {
                it.table.entriesMap shouldBe services.map { it.config.address.toString() to cycleCount }.toMap()
            }

            services.forEach { it.incrementAndGossip() }

            // After IncrementAndGossip()
            services.forEach {
                it.table.entriesMap shouldBe services.map { it.config.address.toString() to cycleCount + 1L }.toMap()
                healthRpc.incomingMessages.getValue(it.config.address).size shouldBe cycleCount + 1
            }
        }
    }

    "Eventually Failed Health System with Two Nodes" {
        val (services, healthRpc) = buildServices(2)

        for (cycleCount in 0 until 10) {
            // Before IncrementAndGossip()
            services.forEach {
                it.table.entriesMap shouldBe services.map { it.config.address.toString() to cycleCount }.toMap()
            }

            services.forEach { it.incrementAndGossip() }

            // After IncrementAndGossip()
            services.forEach {
                it.table.entriesMap shouldBe services.map { it.config.address.toString() to cycleCount + 1L }.toMap()
                healthRpc.incomingMessages.getValue(it.config.address).size shouldBe cycleCount + 1
            }
        }

        healthRpc.networkStatus = Fail

        for (cycleCount in 10 until 100) {
            services.forEach { it.incrementAndGossip() }

            // After IncrementAndGossip()
            services.forEach {
                val expectedTable = services.map { svc ->
                    if (svc.config.address == it.config.address) svc.config.address.toString() to cycleCount + 1L
                    else svc.config.address.toString() to 10L
                }.toMap()

                it.table.entriesMap shouldBe expectedTable
                healthRpc.incomingMessages.getValue(it.config.address).size shouldBe 10L
            }
        }
    }
})
