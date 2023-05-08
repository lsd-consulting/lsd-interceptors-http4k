package com.lsd.interceptors.http4k

import org.assertj.core.api.Assertions.assertThat
import org.http4k.core.Headers
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class HeaderDataRendererTest {

    @ParameterizedTest
    @MethodSource("headers")
    fun rendersHeadersInATable(scenario: HeaderScenario) {
        val request = Request(GET, "/some-path").headers(scenario.headers)

        val rendered = renderHeaders(request.headers)

        scenario.expectedRows.forEach {
            assertThat(rendered).contains(it)
        }
    }

    @Test
    fun rendersEmptyWhenNoHeadersPresent() {
        val request = Request(GET, "/some-path")

        val rendered = renderHeaders(request.headers)

        assertThat(rendered).isEmpty()
    }
    
    @Test
    fun rendersHeading() {
        val request = Request(GET, "/some-path").header("key", "value")

        val rendered = renderHeaders(request.headers)

        assertThat(rendered).contains("HEADERS")
    }

    companion object {
        @JvmStatic
        fun headers(): Set<HeaderScenario> {
            return setOf(
                HeaderScenario(listOf("key" to "value"), listOf("<tr><td>key</td><td>value</td></tr>")),
                HeaderScenario(listOf("key" to null), listOf("<tr><td>key</td><td></td></tr>")),
                HeaderScenario(
                    listOf(
                        "name" to "nick",
                        "age" to "21"
                    ),
                    listOf(
                        "<tr><td>name</td><td>nick</td></tr>",
                        "<tr><td>age</td><td>21</td></tr>",
                    )
                ),
            )
        }
    }

}



data class HeaderScenario(val headers: Headers, val expectedRows: List<String>)