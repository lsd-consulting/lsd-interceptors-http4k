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
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class LsdFilterTest {

    class SpyContext : LsdContext() {
        val messages = mutableListOf<SequenceEvent>()

        override fun capture(vararg events: SequenceEvent) {
            messages.addAll(events)
            super.capture(events = events)
        }
    }

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
        val app = lsdFilter.then { Response(scenario.status).body("[response body]") }

        app(aGetRequest())
        
        assertThat(lsd.messages).hasSize(2)
        assertRequestMessage(lsd.messages[0] as Message)
        assertResponseMessage(lsd.messages[1] as Message, scenario)
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
            assertThat(it.data.toString()).contains("[response body]")
        }
    }

    companion object {
        @JvmStatic
        fun testScenarios(): Set<Scenario> {
            return setOf<Scenario>() + (100..199 step 3).map {
                Scenario(Status(code = it, description = "status description for $it"), arrowColour = "black")
            } + (200..299 step 3).map {
                Scenario(Status(code = it, description = "status description for $it"), arrowColour = "green")
            } + (300..399 step 3).map {
                Scenario(Status(code = it, description = "status description for $it"), arrowColour = "purple")
            } + (400..499 step 3).map {
                Scenario(Status(code = it, description = "status description for $it"), arrowColour = "orange")
            } + (500..599 step 3).map {
                Scenario(Status(code = it, description = "status description for $it"), arrowColour = "red")
            }
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

data class Scenario(val status: Status, val arrowColour: String)
