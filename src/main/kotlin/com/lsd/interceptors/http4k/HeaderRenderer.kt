package com.lsd.interceptors.http4k

import org.http4k.core.Headers

fun renderHeaders(headers: Headers): String {
    return if (headers.isNotEmpty()) """
        <section>
            <h3>HEADERS</h3>
            <table>
            ${
                headers.joinToString(separator = "\n") {
                    "<tr><td>${it.first}</td><td>${it.second ?: ""}</td></tr>"
                }
            }
            </table>
        </section>
    """.trimIndent()
    else ""
}