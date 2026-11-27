package com.example.myapplication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Data flow from the project docs, after the photo was taken and compressed:
 *
 *   INPUT -> NETWORK -> EXTRACT (LLM) -> PARSE (JSON) -> VALIDATE -> ExtractedEvent
 *
 * Pure Kotlin (no Android classes) so it is fully covered by JVM unit tests
 * (see ScanPipelineTest). Every step logs [OK]; every failure is a [ScanException].
 */
class ScanPipeline(
    private val extractor: EventExtractor,
    private val isOnline: () -> Boolean,
    private val timeoutMs: Long = AppConfig.EXTRACTOR_TIMEOUT_MS,
) {

    /**
     * @param onProgress called with 0.0–1.0 after each finished step (drives the loading bar).
     *        Values: see [Progress]. The LLM call is the long part between EXTRACT_START and EXTRACT_DONE.
     */
    suspend fun process(jpeg: ByteArray, onProgress: (Float) -> Unit = {}): ExtractedEvent {
        if (jpeg.isEmpty()) throw ScanException.Compression("Image data is empty.")
        DebugLog.ok("INPUT", "Image ready: ${jpeg.size} bytes")
        onProgress(Progress.INPUT)

        if (!isOnline()) throw ScanException.NoNetwork()
        DebugLog.ok("NETWORK", "Connection check passed (extractor needs network: ${extractor.requiresNetwork})")
        onProgress(Progress.NETWORK)

        DebugLog.info("EXTRACT", "Sending image to ${extractor.name} (timeout ${timeoutMs} ms)")
        onProgress(Progress.EXTRACT_START)
        val raw = try {
            withTimeout(timeoutMs) { extractor.extractJson(jpeg) }
        } catch (e: TimeoutCancellationException) {
            throw ScanException.Timeout(timeoutMs)
        } catch (e: ScanException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ScanException.Llm("Extractor failed: ${e.message ?: e.javaClass.simpleName}", e)
        }
        DebugLog.ok("EXTRACT", "Response received (${raw.length} chars)")
        onProgress(Progress.EXTRACT_DONE)

        val parsed = EventJsonParser.parse(raw)
        DebugLog.ok("PARSE", "JSON parsed -> title=\"${parsed.title}\", date=${parsed.date}, time=${parsed.time}")
        onProgress(Progress.PARSE)

        val event = EventValidator.validate(parsed)
        DebugLog.ok("VALIDATE", "Required fields present and formats valid")
        onProgress(Progress.DONE)
        return event
    }

    /** Loading bar position after each step (edit to rebalance the bar). */
    object Progress {
        const val START = 0.00f
        const val COMPRESS = 0.15f      // set by AppViewModel (compression runs before the pipeline)
        const val INPUT = 0.20f
        const val NETWORK = 0.30f
        const val EXTRACT_START = 0.35f // request sent to the LLM
        const val EXTRACT_DONE = 0.85f  // LLM answered
        const val PARSE = 0.95f
        const val DONE = 1.00f
    }
}
