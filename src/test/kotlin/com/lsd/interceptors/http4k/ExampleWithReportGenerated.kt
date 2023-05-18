package com.lsd.interceptors.http4k

import com.lsd.core.LsdContext
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.junit.jupiter.api.Test

class ExampleWithReportGenerated {

    private val lsd = LsdContext.instance
    private val lsdFilter = LsdFilterProvider(lsd).filter
    private val delegate = lsdFilter.then(Delegate())
    private val app = lsdFilter.then(App(delegate))

    @Test
    fun generateExampleReport() {
        app(
            Request(GET, "/do-work")
                .header("Host", "App")
                .header("User-Agent", "User")
        )

        lsd.completeScenario("delegates work")
        lsd.completeReport("lsd report")
    }
}

class App(val delegate: HttpHandler) : HttpHandler {
    override fun invoke(request: Request): Response {
        val delegateResponse = delegate(
            request
                .replaceHeader("User-Agent", "App")
                .replaceHeader("Host", "Delegate")
        )
        return Response(Status.OK).body("Called delegate with response: $delegateResponse")
    }
}

class Delegate : HttpHandler {
    override fun invoke(request: Request): Response {
        return Response(Status.OK).body("Delegation complete")
    }
}