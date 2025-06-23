package com.adyanta.iot.mask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MaskingApp

fun main(args: Array<String>) {
    runApplication<MaskingApp>(*args)
}
