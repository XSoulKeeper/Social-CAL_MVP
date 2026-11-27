package com.example.myapplication

/**
 * ============================================================================
 *  PROTOTYPE CONFIGURATION – the one place to tweak how the MVP behaves.
 * ============================================================================
 *
 *  Logcat filter for all debug messages:  tag:EventScanner
 */
object AppConfig {

    /** Logcat tag used by [DebugLog]. */
    const val LOG_TAG = "EventScanner"

    /** Shows the last few pipeline debug lines on screen (black strip). */
    const val SHOW_DEBUG_OVERLAY = true

    /**
     * TEMPORARY: orange "DEBUG: test error" button on the camera screen.
     * Each tap shows the next error type on the Error screen. Set to false to hide it.
     */
    const val SHOW_DEBUG_ERROR_BUTTON = true

    /**
     * Force an error to test the Error screen and exception handling.
     * Set back to [SimulatedError.NONE] for the normal "happy path".
     */
    val SIMULATED_ERROR: SimulatedError = SimulatedError.NONE

    /** How long the fake LLM "thinks" – keeps the Loading screen visible. */
    const val MOCK_PROCESSING_DELAY_MS = 1_500L

    /** Max time we wait for the extractor (LLM) before throwing E_TIMEOUT. */
    const val EXTRACTOR_TIMEOUT_MS = 30_000L

    /** Local pre-compression before upload (see docs: "Przetwarzanie lokalne"). */
    const val IMAGE_MAX_DIMENSION_PX = 1280
    const val IMAGE_JPEG_QUALITY = 80

    /** Size of thumbnails stored for the Event History screen. */
    const val THUMBNAIL_MAX_DIMENSION_PX = 240

    /** Default event length passed to the calendar when the poster has a start time. */
    const val DEFAULT_EVENT_DURATION_MS = 2 * 60 * 60 * 1000L

    /**
     * true  -> on every app start the database is wiped and refilled from [SeedData].
     * false -> seed data is inserted only once (when the database is first created).
     */
    const val RESEED_DATABASE_ON_START = false
}

/** Errors that can be simulated without a real LLM / API. */
enum class SimulatedError {
    NONE,
    NO_NETWORK,        // -> E_NO_NETWORK
    LLM_FAILURE,       // -> E_LLM        (e.g. HTTP 500 from the model API)
    LLM_TIMEOUT,       // -> E_TIMEOUT    (model does not answer in EXTRACTOR_TIMEOUT_MS)
    INVALID_JSON,      // -> E_PARSE      (model answered with prose instead of JSON)
    MISSING_TITLE,     // -> E_VALIDATION (JSON ok, but required field empty)
    INVALID_DATE,      // -> E_VALIDATION (date not in yyyy-MM-dd)
    NO_CALENDAR_APP,   // -> E_CALENDAR   (no app can handle ACTION_INSERT)
}
