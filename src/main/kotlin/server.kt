/* ktlint-disable no-wildcard-imports */
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/* ktlint-enable no-wildcard-imports */

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
        routing {
            get("/") {
                call.respondText("OK", status = HttpStatusCode.OK)
            }
        }
    }.start(wait = true)
}
