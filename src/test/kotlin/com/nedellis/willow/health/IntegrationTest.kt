package com.nedellis.willow.health

import com.google.protobuf.Empty
import com.nedellis.willow.Table
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.URI

private class LocalHealthRpc() : HealthRpc {
    lateinit var members: Map<URI, HealthService>

    override suspend fun update(dest: URI, request: Table): Empty {
        members[dest]!!.update(request)
        return Empty.getDefaultInstance()
    }
}

class IntegrationTest : StringSpec({
    /**
     * Build initialized health services, connected to each other by [LocalHealthRpc]
     */
    fun buildServices(count: Int): List<HealthService> {
        val healthRpc = LocalHealthRpc()

        val addreses = (8000 until 8000 + count).map { port ->
            URI("http://127.0.0.1:$port")
        }.toSet()

        val services = addreses.map { addr ->
            HealthService(HealthConfig(addr, addreses), healthRpc)
        }

        healthRpc.members = services.map { it.config.address to it }.toMap()

        return services
    }

    "Health System with One Node" {
        val services = buildServices(1)

        for (cycleCount in 0..100) {
            services.forEach {
                it.table.entriesMap shouldBe mapOf(it.config.address.toString() to cycleCount)
                it.incrementAndGossip()
                it.table.entriesMap shouldBe mapOf(it.config.address.toString() to cycleCount + 1L)
            }
        }
    }
})