package com.example.myapplication

/**
 * Every failure in the scan flow is converted to one of these.
 * [code] is logged and shown small on the Error screen,
 * [userMessage] replaces "%Error description%" from the design.
 */
sealed class ScanException(
    val code: String,
    val userMessage: String,
    cause: Throwable? = null,
) : Exception("[$code] $userMessage", cause) {

    /** Camera could not take / decode the picture. */
    class CameraFailure(msg: String, cause: Throwable? = null) :
        ScanException("E_CAMERA", msg, cause)

    /** Local pre-compression failed or produced no data. */
    class Compression(msg: String, cause: Throwable? = null) :
        ScanException("E_COMPRESSION", msg, cause)

    /** No internet connection (the real LLM needs it – see ADR 1). */
    class NoNetwork :
        ScanException("E_NO_NETWORK", "No internet connection. Connect to the internet and try again.")

    /** The model / API returned an error. */
    class Llm(msg: String, cause: Throwable? = null) :
        ScanException("E_LLM", msg, cause)

    /** The model did not answer in time. */
    class Timeout(timeoutMs: Long) :
        ScanException("E_TIMEOUT", "The model did not respond within ${timeoutMs / 1000} s. Try again.")

    /** The model answered, but not with the expected JSON. */
    class Parse(msg: String, cause: Throwable? = null) :
        ScanException("E_PARSE", msg, cause)

    /** JSON was fine, but data is missing or has a wrong format. */
    class Validation(msg: String) :
        ScanException("E_VALIDATION", msg)

    /** No calendar app could open the event. */
    class CalendarUnavailable(msg: String, cause: Throwable? = null) :
        ScanException("E_CALENDAR", msg, cause)

    /** Local SQLite database problem. */
    class Database(msg: String, cause: Throwable? = null) :
        ScanException("E_DATABASE", msg, cause)

    /** Anything we did not foresee. */
    class Unknown(cause: Throwable) :
        ScanException("E_UNKNOWN", "Unexpected error: ${cause.message ?: cause.javaClass.simpleName}", cause)
}
