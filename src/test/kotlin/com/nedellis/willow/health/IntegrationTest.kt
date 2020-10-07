package com.nedellis.willow.health

import com.google.protobuf.Empty
import com.nedellis.willow.Table
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.URI

private class LocalHealthRpc() : HealthRpc {
    val incomingMessages = mutableMapOf<URI, MutableList<Table>>().withDefault { mutableListOf() }
    // TODO: Find a way to make outgoingMessages

    lateinit var members: Map<URI, HealthService>

    override suspend fun update(dest: URI, request: Table): Empty {
        incomingMessages.getValue(dest).add(request)

        members[dest]!!.update(request)
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

    "Health System with One Node" {
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
})
