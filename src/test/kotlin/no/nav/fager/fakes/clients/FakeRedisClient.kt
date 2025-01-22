package no.nav.fager.fakes.clients

import no.nav.fager.altinn.AltinnService
import no.nav.fager.redis.AltinnTilgangerRedisClient
import no.nav.fager.fakes.FakeClientBase

class FakeRedisClient(
    private val cache: MutableMap<String, AltinnService.AltinnTilgangerResultat> = mutableMapOf(),
) : AltinnTilgangerRedisClient, FakeClientBase() {

    override suspend fun get(fnr: String): AltinnService.AltinnTilgangerResultat? {
        addFunctionCall(this::get.name, fnr)
        return cache[fnr]
    }

    override suspend fun set(fnr: String, altinnTilganger: AltinnService.AltinnTilgangerResultat) {
        addFunctionCall(this::set.name, fnr, altinnTilganger)
        cache[fnr] = altinnTilganger
    }
}