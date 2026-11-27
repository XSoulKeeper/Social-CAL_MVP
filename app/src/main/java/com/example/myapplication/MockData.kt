package com.example.myapplication

/**
 * ============================================================================
 *  SCREEN 3 ("Wrap up") DEFAULT DATA
 * ============================================================================
 *
 * In the real app this JSON comes back from the LLM. In the prototype the
 * [MockEventExtractor] returns exactly this string, and it then goes through
 * the REAL parser ([EventJsonParser]) and validator ([EventValidator]).
 * So once the LLM is connected and returns the same format, nothing else
 * has to change.
 *
 * Contract (all values are strings):
 *   title        – required
 *   date         – required, ISO format  yyyy-MM-dd
 *   time         – optional, 24h format  HH:mm   (null / "" = all-day event)
 *   location     – optional
 *   description  – optional
 */
object MockData {

    const val DEFAULT_LLM_RESPONSE_JSON = """
{
  "title": "Bus Drivers Link Up",
  "date": "2025-07-06",
  "time": "19:00",
  "location": "British Place or whatnot",
  "description": "Really interesting"
}
"""
}
