package com.lsd.interceptors.http4k

import com.lsd.core.LsdContext
import com.lsd.core.domain.Message
import com.lsd.core.domain.MessageType.SYNCHRONOUS
import com.lsd.core.domain.MessageType.SYNCHRONOUS_RESPONSE
import com.lsd.core.domain.ParticipantType.PARTICIPANT
import com.lsd.core.domain.SequenceEvent
import org.assertj.core.api.Assertions.assertThat
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.CONTINUE
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class LsdFilterTest {

    class SpyContext : LsdContext() {
        val messages = mutableListOf<SequenceEvent>()

        override fun capture(vararg events: SequenceEvent) {
            messages.addAll(events)
            super.capture(events = events)
        }
    }

    private val requestBody = "A request body"
    private val responseBody = "A response body"
    private val anonymous = PARTICIPANT.called("Anonymous")
    private val unknown = PARTICIPANT.called("Unknown")
    private val bond = PARTICIPANT.called("Bond")
    private val shop = PARTICIPANT.called("Shop")
    private val lsd = SpyContext()
    private val lsdFilter = LsdFilterProvider(lsd).filter


    @Test
    fun sourceNameAndTargetNamesAreDefaultedIfHeadersAreMissing() {
        val app = lsdFilter.then { Response(OK) }

        app(Request(GET, "/some-path"))

        lsd.requestMessage().let {
            assertThat(it.from).isEqualTo(anonymous)
            assertThat(it.to).isEqualTo(unknown)
        }

        lsd.responseMessage().let {
            assertThat(it.to).isEqualTo(anonymous)
            assertThat(it.from).isEqualTo(unknown)
        }
    }

    @Test
    fun compressedContentIsNotShown() {
        val filterIgnoringCompressedBody = LsdFilterProvider(
            lsd = lsd,
            showCompressedBody = false
        ).filter

        val app = filterIgnoringCompressedBody.then {
            Response(OK).body(responseBody)
                .header("Content-Encoding", "compress")
        }

        app(
            Request(GET, "/").body(requestBody)
                .header("Content-Encoding", "gzip")
        )

        lsd.requestMessage().let {
            assertThat(it.data as String)
                .doesNotContain(requestBody)
                .contains("[encoded body: gzip]")
        }

        lsd.responseMessage().let {
            assertThat(it.data as String)
                .doesNotContain(responseBody)
                .contains("[encoded body: compress]")
        }
    }

    @ParameterizedTest
    @CsvSource(
        "application/pdf",
        "application/octet-stream",
        "multipart/mixed",
        "image/gif",
        "audio/mpeg"
    )
    fun notShowBodyForBinaryContent(contentType: String) {
        val app = lsdFilter.then {
            Response(OK)
                .body(responseBody)
                .header("Content-Type", contentType)
        }

        app(
            Request(GET, "/")
                .body(requestBody)
                .header("Content-Type", contentType)
        )

        lsd.requestMessage().let {
            assertThat(it.data as String)
                .contains("[$contentType]")
                .doesNotContain(requestBody)
        }

        lsd.responseMessage().let {
            assertThat(it.data as String)
                .contains("[$contentType]")
                .doesNotContain(responseBody)
        }
    }

    @Test
    fun uriHostNameIsUsedIfHostHeaderIsMissing() {
        val app = lsdFilter.then { Response(OK) }

        app(Request(GET, Uri.of("https://app1/some-path")))

        lsd.requestMessage().let {
            assertThat(it.from).isEqualTo(anonymous)
            assertThat(it.to).isEqualTo(PARTICIPANT.called("app1"))
        }

        lsd.responseMessage().let {
            assertThat(it.from).isEqualTo(PARTICIPANT.called("app1"))
            assertThat(it.to).isEqualTo(anonymous)
        }
    }

    @ParameterizedTest
    @MethodSource("testScenarios")
    fun captureRequestAndResponse(scenario: Scenario) {
        val app = lsdFilter.then {
            Response(scenario.status)
                .header("Content-Type", scenario.contentType.value)
                .body(responseBody)
        }

        app(aGetRequest())

        assertThat(lsd.messages).hasSize(2)
        assertRequestMessage(lsd.requestMessage())
        assertResponseMessage(lsd.responseMessage(), scenario)
        assertThat(lsd.responseMessage().duration).isNotNull()
    }

    private fun aGetRequest() =
        Request(GET, "/price")
            .header("User-Agent", "Bond")
            .header("Host", "Shop")
            .query("product", "apple")

    private fun assertRequestMessage(requestMessage: Message) {
        requestMessage.let {
            assertThat(it.from).isEqualTo(bond)
            assertThat(it.to).isEqualTo(shop)
            assertThat(it.label).isEqualTo("GET /price?product=apple")
            assertThat(it.type).isEqualTo(SYNCHRONOUS)
            assertThat(it.colour).isEqualTo("")
            assertThat(it.data.toString())
                .contains("User-Agent", "Bond")
                .contains("Host", "Shop")
        }
    }

    private fun assertResponseMessage(requestMessage: Message, scenario: Scenario) {
        requestMessage.let {
            assertThat(it.from).isEqualTo(shop)
            assertThat(it.to).isEqualTo(bond)
            assertThat(it.label).contains(scenario.status.description)
            assertThat(it.type).isEqualTo(SYNCHRONOUS_RESPONSE)
            assertThat(it.colour).isEqualTo(scenario.arrowColour)
            assertThat(it.data.toString()).contains(responseBody)
        }
    }

    companion object {
        @JvmStatic
        fun testScenarios(): Set<Scenario> {
            return setOf(
                Scenario(CONTINUE, arrowColour = "black", ContentType.TEXT_PLAIN),
                Scenario(OK, arrowColour = "green", ContentType.TEXT_PLAIN),
                Scenario(OK, arrowColour = "green", ContentType.TEXT_XML),
                Scenario(OK, arrowColour = "green", ContentType.TEXT_HTML),
                Scenario(OK, arrowColour = "green", ContentType.TEXT_CSV),
                Scenario(OK, arrowColour = "green", ContentType.APPLICATION_JSON),
                Scenario(OK, arrowColour = "green", ContentType.APPLICATION_XML),
                Scenario(OK, arrowColour = "green", ContentType.APPLICATION_YAML),
                Scenario(FOUND, arrowColour = "purple", ContentType.TEXT_PLAIN),
                Scenario(NOT_FOUND, arrowColour = "orange", ContentType.TEXT_PLAIN),
                Scenario(INTERNAL_SERVER_ERROR, arrowColour = "red", ContentType.TEXT_PLAIN)
            )
        }
    }
}

private fun LsdFilterTest.SpyContext.requestMessage(): Message {
    assertThat(messages).hasSize(2)
    return messages[0] as Message
}

private fun LsdFilterTest.SpyContext.responseMessage(): Message {
    assertThat(messages).hasSize(2)
    return messages[1] as Message
}

data class Scenario(
    val status: Status,
    val arrowColour: String,
    val contentType: ContentType
)
    
