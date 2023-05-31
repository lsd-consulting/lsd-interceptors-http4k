package com.lsd.interceptors.http4k

import com.lsd.core.LsdContext
import com.lsd.core.builders.MessageBuilder.Companion.messageBuilder
import com.lsd.core.domain.MessageType.SYNCHRONOUS_RESPONSE
import com.lsd.core.escapeHtml
import org.http4k.core.Filter
import org.http4k.core.Request
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream


class LsdFilterProvider(
    lsd: LsdContext,
    val showCompressedBody: Boolean = false,
    sourceNameProvider: (Request) -> String = ::extractSourceName,
    targetNameProvider: (Request) -> String = ::extractTargetName,
    nameCleaner: (String) -> String = String::normalise
) {

    val filter = Filter { next ->
        { request ->
            val sourceName = nameCleaner(sourceNameProvider(request))
            val targetHost = nameCleaner(targetNameProvider(request))
            val startTime = Instant.now()
            val requestBodyCopy = request.body.payload.array()
            lsd.capture(
                messageBuilder().id(lsd.idGenerator.next()).from(sourceName).to(targetHost)
                    .label("${request.method} ${request.uri}").data(
                        """
                           | <section>
                           |   <h3>PATH</h3>
                           |   <p>${sourceName}: ${request.method} to (${targetHost})${request.uri}</p>
                           | </section>
                           | ${renderHeaders(request.headers)}
                           | <section>
                           |    <h3>BODY</h3>
                           |    <p>${
                            readBody(
                                inputStream = requestBodyCopy.inputStream(),
                                contentType = request.header("Content-Type"),
                                encoding = request.header("Content-Encoding")
                            )
                        }</p>  
                           | </section>
                           |""".trimMargin()

                    ).build()
            )
            val originalResponse = next(request.body(requestBodyCopy.inputStream()))
            val responseBodyCopy = originalResponse.body.payload.array()
            val durationMillis = Instant.now().toEpochMilli() - startTime.toEpochMilli()

            originalResponse.body(responseBodyCopy.inputStream()).also { response ->
                lsd.capture(
                    messageBuilder().id(lsd.idGenerator.next())
                        .duration(Duration.ofMillis(durationMillis))
                        .data(
                        """
                                   | <section>
                                   |   <h3>RESPONSE</h3>
                                   |   <p>$targetHost to $sourceName (${durationMillis}ms)</p>
                                   | </section>
                                   | ${renderHeaders(response.headers)}
                                   | <section>
                                   |   <h3>BODY</h3>
                                   |   ${
                            readBody(
                                inputStream = responseBodyCopy.inputStream(),
                                contentType = response.header("Content-Type"),
                                encoding = response.header("Content-Encoding")
                            )
                        }        
                                   | </section>
                                """.trimMargin()
                    ).from(targetHost).to(sourceName).type(SYNCHRONOUS_RESPONSE)
                        .label("${response.status} (${durationMillis}ms) ").colour(
                            when (response.status.code) {
                                in 200..299 -> "green"
                                in 300..399 -> "purple"
                                in 400..499 -> "orange"
                                in 500..599 -> "red"
                                else -> "black"
                            }
                        ).build()
                )
            }
        }
    }

    private fun readBody(inputStream: ByteArrayInputStream, contentType: String?, encoding: String?): String {
        if (encoding != null && !showCompressedBody) 
            return "[encoded body: ${encoding}]"
        
        return when {
            contentType == null || containsSupported(contentType) ->
                when (encoding) {
                    "gzip" -> GZIPInputStream(inputStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        .escapeHtml()

                    "zip" -> ZipInputStream(inputStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        .escapeHtml()

                    else -> inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText).escapeHtml()
                }

            else -> "[${contentType}]"
        }
    }
}

fun extractSourceName(it: Request) = it.header("User-Agent") ?: "Anonymous"

fun extractTargetName(it: Request): String = (it.header("Host") ?: it.uri.host.split(".").first()).ifBlank { "Unknown" }


fun containsSupported(contentType: String): Boolean =
    listOf("text", "json", "xml", "yaml").any { contentType.contains(it) }


fun String.normalise(): String =
    replace("""Apache-HttpClient.*""".toRegex(), "http_client").replace("""#.*""".toRegex(), "")
        .replace("""\.local.*""".toRegex(), "").replace(""":.*""".toRegex(), "").replace("""[$,.:]""".toRegex(), "")
        .replace("-", "_")

