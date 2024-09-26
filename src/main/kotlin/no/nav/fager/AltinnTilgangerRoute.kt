package no.nav.fager

import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.schemakenerator.core.annotations.Description
import io.github.smiley4.schemakenerator.core.annotations.Example
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.fager.AltinnTilgangerResponse.Companion.toResponse

fun Route.routeAltinnTilganger(altinnService: AltinnService) {
    authenticate {
        // TODO: it may be useful to be able to set a filter with service/resource (and optionally orgnr) as input
        // a lot of other teams often only care about a single service/resource

        post("/altinn-tilganger", {
            description = "Hent tilganger fra Altinn for innlogget bruker."
            request {
                // todo document optional callid header
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<AltinnTilgangerResponse> {
                        exampleRef("Successful Respons", "tilganger_success")
                    }
                }
            }
        }) {
            val fnr = call.principal<InloggetBrukerPrincipal>()!!.fnr
            val tilganger = altinnService.hentTilganger(fnr, this)

            call.respond(tilganger.toResponse())
        }
    }
}


@Description("Brukerens tilganger til Altinn 2 og Altinn 3 for en organisasjon")
@Serializable
data class AltinnTilgang(
    @Description("Organisasjonsnummer")
    @Example("11223344")
    val orgNr: String,
    @Description("Tilganger til Altinn 3")
    val altinn3Tilganger: Set<String>,
    @Description("Tilganger til Altinn 2")
    val altinn2Tilganger: Set<String>,
    @Description("list av underenheter til denne organisasjonen hvor brukeren har tilganger")
    val underenheter: List<AltinnTilgang>,
    @Description("Navn på organisasjonen")
    val name: String,
    @Description("Organisasjonsform. se https://www.brreg.no/bedrift/organisasjonsformer/")
    @Example("BEDR")
    val organizationForm: String,
)

@Serializable
data class AltinnTilgangerResponse(
    @Description("Om det var en feil ved henting av tilganger. Dersom denne er true kan det bety at ikke alle tilganger er hentet.")
    val isError: Boolean,
    @Description("Organisasjonshierarkiet med brukerens tilganger")
    val hierarki: List<AltinnTilgang>,
    @Description("Map fra organisasjonsnummer til tilganger. Convenience for å slå opp tilganger på orgnummer.")
    val orgNrTilTilganger: Map<String, Set<String>>,
    @Description("Map fra tilgang til organisasjonsnummer. Convenience for å slå opp orgnummer på tilgang.")
    val tilgangTilOrgNr: Map<String, Set<String>>,
) {
    companion object {
        fun AltinnService.AltinnTilgangerResultat.toResponse(): AltinnTilgangerResponse {
            val orgNrTilTilganger: Map<String, Set<String>> =
                this.altinnTilganger.flatMap { it.underenheter }
                    .associate {
                        it.orgNr to it.altinn2Tilganger + it.altinn3Tilganger
                    }

            val tilgangToOrgNr = orgNrTilTilganger.flatMap { (orgNr, tjenester) ->
                tjenester.map { it to orgNr }
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }


            return AltinnTilgangerResponse(
                isError = this.isError,
                hierarki = this.altinnTilganger,
                orgNrTilTilganger = orgNrTilTilganger,
                tilgangTilOrgNr = tilgangToOrgNr,
            )
        }
    }
}
