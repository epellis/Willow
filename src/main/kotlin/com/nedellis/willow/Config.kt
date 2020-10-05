package com.nedellis.willow

import com.typesafe.config.Config

class Config(config: Config) {
    val INPUT_PLANE_PORT = config.getInt("willow.inputPlane.port")
}
