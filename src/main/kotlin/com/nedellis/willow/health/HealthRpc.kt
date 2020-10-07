package com.nedellis.willow.health

import com.google.protobuf.Empty
import com.nedellis.willow.Table
import java.net.URI

interface HealthRpc {
    suspend fun update(dest: URI, request: Table): Empty
}
