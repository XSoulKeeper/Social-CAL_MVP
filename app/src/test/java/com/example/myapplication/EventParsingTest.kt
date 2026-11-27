package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Tests for the LLM JSON contract, validation and date/time conversions. */
class EventParsingTest {

    // ---------------------------------------------------------------- JSON parser

    @Test
    fun parser_readsDefaultMockJson() {
        val event = EventJsonParser.parse(MockData.DEFAULT_LLM_RESPONSE_JSON)
        assertEquals("Bus Drivers Link Up", event.title)
        assertEquals("2025-07-06", event.date)
        assertEquals("19:00", event.time)
    }

    @Test
    fun parser_jsonNullBecomesEmpty_notTextNull() {
        val event = EventJsonParser.parse("""{"title":"A","date":"2026-01-01","time":null,"location":null}""")
        assertNull(event.time)
        assertEquals("", event.location)
    }

    @Test
    fun parser_missingKeysAreEmpty() {
        val event = EventJsonParser.parse("""{"title":"A","date":"2026-01-01"}""")
        assertNull(event.time)
        assertEquals("", event.description)
    }

    @Test
    fun parser_emptyResponse_throwsParse() {
        assertThrows(ScanException.Parse::class.java) { EventJsonParser.parse("   ") }
    }

    @Test
    fun parser_proseResponse_throwsParse() {
        assertThrows(ScanException.Parse::class.java) { EventJsonParser.parse("I can't read that.") }
    }

    @Test
    fun parser_brokenJson_throwsParse() {
        assertThrows(ScanException.Parse::class.java) { EventJsonParser.parse("""{"title": "A", "date": }""") }
    }

    // ---------------------------------------------------------------- validator

    @Test
    fun validator_acceptsAllDayEvent() {
        val event = ExtractedEvent("Fair", "2026-12-05", null, "", "")
        assertEquals(event, EventValidator.validate(event))
    }

    @Test
    fun validator_rejectsBadTime() {
        val event = ExtractedEvent("Fair", "2026-12-05", "7pm", "", "")
        assertThrows(ScanException.Validation::class.java) { EventValidator.validate(event) }
    }

    @Test
    fun validator_rejectsImpossibleDate() {
        val event = ExtractedEvent("Fair", "2026-02-31", null, "", "")
        assertThrows(ScanException.Validation::class.java) { EventValidator.validate(event) }
    }

    // ---------------------------------------------------------------- formats

    @Test
    fun formats_isoToDisplay() {
        assertEquals("06.07.2025", EventFormats.isoToDisplayDate("2025-07-06"))
        assertEquals("7:00 PM", EventFormats.isoToDisplayTime("19:00"))
    }

    @Test
    fun formats_displayToIso_acceptsSeveralTimeStyles() {
        assertEquals("2025-07-06", EventFormats.displayToIsoDate("06.07.2025"))
        assertEquals("19:00", EventFormats.displayToIsoTime("7:00 PM"))
        assertEquals("19:00", EventFormats.displayToIsoTime("7:00 Pm"))
        assertEquals("19:00", EventFormats.displayToIsoTime("19:00"))
        assertEquals("09:30", EventFormats.displayToIsoTime("9:30 am"))
    }

    @Test
    fun formats_rejectGarbage() {
        assertNull(EventFormats.displayToIsoDate("2025-07-06"))
        assertNull(EventFormats.displayToIsoDate("31.02.2025"))
        assertNull(EventFormats.displayToIsoTime("evening"))
    }

    @Test
    fun formats_startMillis() {
        assertNotNull(EventFormats.startMillis("2025-07-06", "19:00"))
        assertNotNull(EventFormats.startMillis("2025-07-06", null))
        assertNull(EventFormats.startMillis("bad", null))
    }

    // ---------------------------------------------------------------- wrap up form

    @Test
    fun wrapUpForm_roundTrip() {
        val original = EventJsonParser.parse(MockData.DEFAULT_LLM_RESPONSE_JSON)
        val form = WrapUpForm.from(original)
        assertEquals("06.07.2025", form.date)
        assertEquals("7:00 PM", form.time)
        assertEquals(original, form.toExtractedEvent())
    }

    @Test
    fun wrapUpForm_badDate_throwsValidation() {
        val form = WrapUpForm("A", "6 July", "", "", "")
        assertThrows(ScanException.Validation::class.java) { form.toExtractedEvent() }
    }
}
