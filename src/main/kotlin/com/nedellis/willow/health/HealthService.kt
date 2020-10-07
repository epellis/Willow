package com.nedellis.willow.health

import com.nedellis.willow.Table
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI

const val TABLE_HISTORY_SIZE = 2

data class HealthConfig(
    val address: URI,
    val members: Set<URI>
)

class HealthService(val config: HealthConfig, private val healthRpc: HealthRpc) {
    val lock = Mutex()

    var table: Table = Table.newBuilder()
        .putAllEntries(config.members.map { it.toString() to 0L }.toMap())
        .build()

    // Stores the previous two tables in a queue.
    // The most recent table is pushed and the least recent table is popped only in [advance]
    val tableHistory = ArrayDeque<Table>()

    /**
     * Called by an external clock or test fixture. All operations are locked by a mutex so
     */
    suspend fun advance() = lock.withLock() {
        increment()
        gossip()
        prune()
    }

    private fun increment() {
        tableHistory.addFirst(table)
        if (tableHistory.size > TABLE_HISTORY_SIZE) {
            tableHistory.removeLast()
        }
        assert(tableHistory.size <= TABLE_HISTORY_SIZE)

        table = table.incrementSelf(config.address)
    }

    private suspend fun gossip() {
        val randomNeighbor = table.randomNeighbor(config.address)
        if (randomNeighbor != null) {
            // TODO: Find out how to make sure this function does not block
            healthRpc.update(randomNeighbor, table)
        }
    }

    private fun prune() {}

    suspend fun update(otherTable: Table) = lock.withLock {
        table = table.merge(otherTable)
    }
}
