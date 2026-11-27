package com.example.myapplication

import kotlinx.coroutines.delay

/**
 * ============================================================================
 *  LLM CONNECTION POINT
 * ============================================================================
 *
 * Anything that turns a poster photo into event JSON implements this interface.
 * The prototype uses [MockEventExtractor]. To connect a real model (e.g. Gemini):
 *
 *   1. Create a class, e.g. `GeminiEventExtractor : EventExtractor`
 *        - requiresNetwork = true
 *        - extractJson(): send [jpeg] + [EXTRACTION_PROMPT] to the API
 *          (use withContext(Dispatchers.IO) for the HTTP call),
 *          return the model's text answer as is.
 *        - on HTTP / API errors throw ScanException.Llm("...")
 *   2. Return it from [ExtractorFactory.create].
 *
 * Parsing, validation, timeout, network check, error screen and the unit tests
 * in ScanPipelineTest already work for any implementation.
 */
interface EventExtractor {
    /** Shown in logs and on the Wrap up / Settings screen. */
    val name: String

    /** If true, the pipeline checks the internet connection first. */
    val requiresNetwork: Boolean

    /** Returns the RAW text answer of the model (expected: JSON, see [MockData]). */
    suspend fun extractJson(jpeg: ByteArray): String
}

/** The single place that decides which extractor the app uses. */
object ExtractorFactory {
    fun create(): EventExtractor = MockEventExtractor()
    // Later: fun create(): EventExtractor = GeminiEventExtractor(apiKey = BuildConfig.GEMINI_API_KEY)
}

/** Prompt to send together with the image once a real LLM is connected. */
const val EXTRACTION_PROMPT = """
You read photos of posters, flyers and notice boards (also handwriting).
Find the event and answer ONLY with one JSON object, no markdown, no comments:
{"title": string, "date": "yyyy-MM-dd", "time": "HH:mm" or null, "location": string, "description": string}
Resolve relative dates (e.g. "next Friday") using today's date. If there is no year, use the nearest future date.
"description" = one short sentence in English.
"""

/**
 * Fake LLM. Returns [MockData.DEFAULT_LLM_RESPONSE_JSON] (or a broken answer when
 * [AppConfig.SIMULATED_ERROR] is set) after a short delay.
 */
class MockEventExtractor(
    private val simulatedError: SimulatedError = AppConfig.SIMULATED_ERROR,
    private val delayMs: Long = AppConfig.MOCK_PROCESSING_DELAY_MS,
    private val response: String = MockData.DEFAULT_LLM_RESPONSE_JSON,
) : EventExtractor {

    override val name: String = "MockEventExtractor"
    override val requiresNetwork: Boolean = false

    override suspend fun extractJson(jpeg: ByteArray): String {
        DebugLog.info("EXTRACT", "MOCK: no AI connected, returning hard-coded data (simulatedError=$simulatedError)")
        delay(delayMs)
        return when (simulatedError) {
            SimulatedError.LLM_FAILURE ->
                throw ScanException.Llm("Simulated: model API returned HTTP 500.")
            SimulatedError.LLM_TIMEOUT -> {
                delay(AppConfig.EXTRACTOR_TIMEOUT_MS + 1_000L)
                response
            }
            SimulatedError.INVALID_JSON ->
                "Sorry, I could not read this poster."
            SimulatedError.MISSING_TITLE ->
                """{"title": "", "date": "2025-07-06", "time": "19:00", "location": "", "description": ""}"""
            SimulatedError.INVALID_DATE ->
                """{"title": "Bus Drivers Link Up", "date": "next sunday", "time": "19:00"}"""
            else -> response
        }
    }
}
