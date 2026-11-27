package com.example.myapplication

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Debug tests for the whole scan pipeline (no Android device needed).
 * Run: right-click the `test` folder -> Run 'Tests in ...'  or  ./gradlew testDebugUnitTest
 *
 * When the real LLM is connected, these keep guarding the contract:
 * every failure must end up as the right ScanException (= right Error screen).
 */
class ScanPipelineTest {

    private val fakeJpeg = ByteArray(2048) { 1 }

    private fun pipeline(error: SimulatedError = SimulatedError.NONE, online: Boolean = true) =
        ScanPipeline(
            extractor = MockEventExtractor(simulatedError = error, delayMs = 0),
            isOnline = { online },
        )

    private fun pipelineReturning(rawResponse: String) =
        ScanPipeline(
            extractor = MockEventExtractor(simulatedError = SimulatedError.NONE, delayMs = 0, response = rawResponse),
            isOnline = { true },
        )

    @Test
    fun happyPath_returnsDefaultMockEvent() = runBlocking<Unit> {
        val event = pipeline().process(fakeJpeg)
        assertEquals("Bus Drivers Link Up", event.title)
        assertEquals("2025-07-06", event.date)
        assertEquals("19:00", event.time)
        assertEquals("British Place or whatnot", event.location)
        assertEquals("Really interesting", event.description)
    }

    @Test
    fun progress_goesUpTo100Percent_inOrder() = runBlocking<Unit> {
        val reported = mutableListOf<Float>()
        pipeline().process(fakeJpeg) { reported += it }
        assertEquals(reported.sorted(), reported)
        assertEquals(ScanPipeline.Progress.DONE, reported.last())
    }

    @Test
    fun llmAnswerInMarkdownFence_isAccepted() = runBlocking<Unit> {
        val raw = "Here you go:\n```json\n{\"title\":\"Jazz\",\"date\":\"2026-12-12\",\"time\":null}\n```"
        val event = pipelineReturning(raw).process(fakeJpeg)
        assertEquals("Jazz", event.title)
        assertEquals(null, event.time)
    }

    @Test
    fun emptyImage_throwsCompression() {
        assertThrows(ScanException.Compression::class.java) {
            runBlocking { pipeline().process(ByteArray(0)) }
        }
    }

    @Test
    fun offline_throwsNoNetwork() {
        assertThrows(ScanException.NoNetwork::class.java) {
            runBlocking { pipeline(online = false).process(fakeJpeg) }
        }
    }

    @Test
    fun llmFailure_throwsLlm() {
        assertThrows(ScanException.Llm::class.java) {
            runBlocking { pipeline(SimulatedError.LLM_FAILURE).process(fakeJpeg) }
        }
    }

    @Test
    fun invalidJson_throwsParse() {
        assertThrows(ScanException.Parse::class.java) {
            runBlocking { pipeline(SimulatedError.INVALID_JSON).process(fakeJpeg) }
        }
    }

    @Test
    fun missingTitle_throwsValidation() {
        assertThrows(ScanException.Validation::class.java) {
            runBlocking { pipeline(SimulatedError.MISSING_TITLE).process(fakeJpeg) }
        }
    }

    @Test
    fun invalidDate_throwsValidation() {
        assertThrows(ScanException.Validation::class.java) {
            runBlocking { pipeline(SimulatedError.INVALID_DATE).process(fakeJpeg) }
        }
    }

    @Test
    fun slowExtractor_throwsTimeout() {
        val slow = object : EventExtractor {
            override val name = "SlowExtractor"
            override val requiresNetwork = false
            override suspend fun extractJson(jpeg: ByteArray): String {
                delay(1_000)
                return MockData.DEFAULT_LLM_RESPONSE_JSON
            }
        }
        val pipeline = ScanPipeline(slow, isOnline = { true }, timeoutMs = 50)
        assertThrows(ScanException.Timeout::class.java) {
            runBlocking { pipeline.process(fakeJpeg) }
        }
    }

    @Test
    fun crashingExtractor_isWrappedAsLlm() {
        val crashing = object : EventExtractor {
            override val name = "CrashingExtractor"
            override val requiresNetwork = true
            override suspend fun extractJson(jpeg: ByteArray): String =
                throw IllegalStateException("socket closed")
        }
        val pipeline = ScanPipeline(crashing, isOnline = { true })
        assertThrows(ScanException.Llm::class.java) {
            runBlocking { pipeline.process(fakeJpeg) }
        }
    }
}
