package com.nedellis.willow

import com.typesafe.config.ConfigFactory
import io.grpc.ServerBuilder

class ConfigPlane {

}

fun main() {
    val config = Config(ConfigFactory.load())
    val server = ServerBuilder
        .forPort(config.INPUT_PLANE_PORT)
        .build()
    server.start()
    server.awaitTermination()
}
