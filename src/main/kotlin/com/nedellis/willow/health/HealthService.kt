package com.nedellis.willow.health

import com.nedellis.willow.Table
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.concurrent.Executors

data class HealthConfig(
    val address: URI,
    val members: Set<URI>,
    val dispatcher: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
)

class HealthService(val config: HealthConfig, private val healthRpc: HealthRpc) {
    var table: Table = Table.newBuilder()
        .putAllEntries(config.members.map { it.toString() to 0L }.toMap())
        .build()

    suspend fun incrementAndGossip() = withContext(config.dispatcher) {
        table = table.incrementSelf(config.address)
        val randomNeighbor = table.randomNeighbor(config.address)

        if (randomNeighbor != null) {
            healthRpc.update(randomNeighbor, table)
        }
    }

    suspend fun update(otherTable: Table) = withContext(config.dispatcher) {
        table = table.merge(otherTable)
    }
}
