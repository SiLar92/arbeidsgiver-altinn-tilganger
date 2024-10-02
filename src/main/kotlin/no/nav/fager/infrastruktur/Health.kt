package no.nav.fager.infrastruktur

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

interface Service {
    fun isReady(): Boolean
    fun isAlive(): Boolean = true
}

object Health {
    private val log = logger()

    val services = mutableListOf<Service>()

    val alive
        get() = services.all { it.isAlive() }

    val ready
        get() = services.all { it.isReady() }

    private val terminatingAtomic = AtomicBoolean(false)

    val terminating: Boolean
        get() = !alive || terminatingAtomic.get()

    init {
        val shutdownTimeout = basedOnEnv(
            prod = { Duration.ofSeconds(0) },
            dev = { Duration.ofSeconds(0) },
            other = { Duration.ofMillis(0) },
        )

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                terminatingAtomic.set(true)
                log.info("shutdown signal received")
            }
        })
    }
}