package com.example.myapplication

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Screens from UI_v1 (01..06) + Settings. No transition animations – plain state switch. */
enum class Screen { CAMERA, PROCESSING, WRAP_UP, SUCCESS, ERROR, HISTORY, SETTINGS }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application

    val settings = AppSettings(application)
    /** Room DAO – every database operation goes through it (see EventDao.kt). */
    private val eventDao = EventDatabase.get(application).eventDao()
    private val extractor: EventExtractor = ExtractorFactory.create()
    private val pipeline = ScanPipeline(extractor = extractor, isOnline = { checkOnline() })

    val extractorName: String get() = extractor.name

    var screen by mutableStateOf(Screen.CAMERA)
        private set
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var form by mutableStateOf(WrapUpForm.EMPTY)
        private set
    var isSaving by mutableStateOf(false)
        private set

    /** 0.0–1.0, drives the loading bar on the Processing screen. */
    var scanProgress by mutableStateOf(0f)
        private set
    var errorMessage by mutableStateOf("")
        private set
    var errorCode by mutableStateOf("")
        private set
    var lastSavedEvent by mutableStateOf<EventEntity?>(null)
        private set
    var history by mutableStateOf<List<EventEntity>>(emptyList())
        private set
    var flashEnabled by mutableStateOf(false)
        private set

    /** Set when the calendar app should be opened; consumed by the UI (AppRoot). */
    var pendingCalendarIntent by mutableStateOf<Intent?>(null)
        private set

    private var scanJob: Job? = null
    private var screenBeforeSettings = Screen.CAMERA
    private var calendarOpenedFromHistory = false

    init {
        DebugLog.info("APP", "Prototype started | extractor=${extractor.name} | simulatedError=${AppConfig.SIMULATED_ERROR}")
        viewModelScope.launch { runSelfTest() }
        observeHistory()
    }

    /**
     * Room Flow: the History list updates itself whenever the table changes
     * (new scan, "saved" status, reseed) – no manual reloading needed.
     */
    private fun observeHistory() {
        viewModelScope.launch {
            try {
                eventDao.observeAll().collect { events ->
                    history = events
                    DebugLog.ok("HISTORY", "Room emitted ${events.size} events")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLog.fail("HISTORY", "Could not read history: ${e.message}", e)
            }
        }
    }

    // ---------------------------------------------------------------- self-test

    /** Runs once at start; results visible in Logcat (tag:EventScanner) and the debug overlay. */
    private suspend fun runSelfTest() {
        try {
            if (AppConfig.RESEED_DATABASE_ON_START) {
                eventDao.reseed(SeedData.ENTRIES)
                DebugLog.info("SELFTEST", "Database refilled from SeedData (RESEED_DATABASE_ON_START = true)")
            }
            val count = eventDao.count()
            DebugLog.ok("SELFTEST", "Room database ${EventDatabase.DB_NAME} OK ($count entries)")
        } catch (e: Exception) {
            DebugLog.fail("SELFTEST", "Database error: ${e.message}", e)
        }
        if (CalendarHelper.isCalendarAvailable(appContext)) {
            DebugLog.ok("SELFTEST", "Calendar app available (ACTION_INSERT)")
        } else {
            DebugLog.warn("SELFTEST", "No calendar app found for ACTION_INSERT")
        }
        if (NetworkChecker.isOnline(appContext)) {
            DebugLog.ok("SELFTEST", "Network available")
        } else {
            DebugLog.warn("SELFTEST", "Offline (mock extractor does not need network)")
        }
    }

    private fun checkOnline(): Boolean =
        if (extractor.requiresNetwork) {
            NetworkChecker.isOnline(appContext)
        } else {
            AppConfig.SIMULATED_ERROR != SimulatedError.NO_NETWORK
        }

    // ---------------------------------------------------------------- scanning

    fun onPhotoCaptured(bitmap: Bitmap) {
        if (screen == Screen.PROCESSING) return
        capturedBitmap = bitmap
        scanProgress = ScanPipeline.Progress.START
        screen = Screen.PROCESSING
        DebugLog.ok("CAPTURE", "Photo captured (${bitmap.width}x${bitmap.height})")

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val jpeg = withContext(Dispatchers.Default) { ImageCompressor.compress(bitmap) }
                DebugLog.ok(
                    "COMPRESS",
                    "JPEG ${jpeg.size / 1024} KB (max ${AppConfig.IMAGE_MAX_DIMENSION_PX}px, quality ${AppConfig.IMAGE_JPEG_QUALITY})",
                )
                scanProgress = ScanPipeline.Progress.COMPRESS
                val event = pipeline.process(jpeg) { progress -> scanProgress = progress }
                delay(300) // let the user see 100 %
                form = WrapUpForm.from(event)
                screen = Screen.WRAP_UP
                DebugLog.ok("PIPELINE", "Scan finished without errors -> Wrap up")
            } catch (e: CancellationException) {
                DebugLog.warn("PIPELINE", "Scan cancelled")
                throw e
            } catch (e: ScanException) {
                showError(e)
            } catch (e: Exception) {
                showError(ScanException.Unknown(e))
            }
        }
    }

    fun onCaptureFailed(error: Throwable) {
        showError(ScanException.CameraFailure("Could not take a photo: ${error.message}", error))
    }

    private fun cancelProcessing() {
        scanJob?.cancel()
        scanJob = null
        screen = Screen.CAMERA
    }

    // ---------------------------------------------------------------- wrap up

    fun updateForm(transform: (WrapUpForm) -> WrapUpForm) {
        form = transform(form)
    }

    /** "Add to calendar": validate -> save in DB (not saved yet) -> open system calendar. */
    fun addToCalendar() {
        if (isSaving) return
        val event = try {
            EventValidator.validate(form.toExtractedEvent())
        } catch (e: ScanException) {
            showError(e)
            return
        }
        DebugLog.ok("FORM", "Wrap up form is valid")

        isSaving = true
        val bitmap = capturedBitmap
        viewModelScope.launch {
            try {
                val thumbnailPath = withContext(Dispatchers.IO) { saveThumbnail(bitmap) }
                val draft = EventEntity.fromExtracted(event, thumbnailPath = thumbnailPath)
                val entity = draft.copy(id = eventDao.insert(draft))
                DebugLog.ok("DATABASE", "Event #${entity.id} stored (status: not saved in calendar yet)")
                lastSavedEvent = entity
                calendarOpenedFromHistory = false
                requestCalendarInsert(entity)
            } catch (e: ScanException) {
                showError(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(ScanException.Database("Could not store the event: ${e.message}", e))
            } finally {
                isSaving = false
            }
        }
    }

    // ---------------------------------------------------------------- calendar

    private fun requestCalendarInsert(event: EventEntity) {
        if (AppConfig.SIMULATED_ERROR == SimulatedError.NO_CALENDAR_APP) {
            throw ScanException.CalendarUnavailable("Simulated: no calendar app installed.")
        }
        pendingCalendarIntent = CalendarHelper.buildInsertIntent(event)
        DebugLog.info("CALENDAR", "Opening system calendar (ACTION_INSERT) for event #${event.id}")
    }

    fun onCalendarIntentConsumed() {
        pendingCalendarIntent = null
    }

    fun onCalendarLaunchFailed(error: Throwable) {
        showError(ScanException.CalendarUnavailable("No calendar app found on this device.", error))
    }

    /**
     * Called when the user comes back from the calendar app.
     * Note: ACTION_INSERT does not tell us whether the user pressed "Save",
     * so the prototype treats returning as "saved" (logged as INFO).
     */
    fun onCalendarResult(resultCode: Int) {
        val event = lastSavedEvent ?: return
        DebugLog.info("CALENDAR", "Back from calendar app (resultCode=$resultCode) – ACTION_INSERT gives no confirmation, assuming saved")
        viewModelScope.launch {
            try {
                val updated = eventDao.markSaved(event.id, true) > 0
                if (updated) {
                    DebugLog.ok("DATABASE", "Event #${event.id} marked as saved")
                } else {
                    DebugLog.warn("DATABASE", "Event #${event.id} not found when marking as saved")
                }
                lastSavedEvent = event.copy(saved = true)
            } catch (e: Exception) {
                DebugLog.fail("DATABASE", "Could not mark event as saved: ${e.message}", e)
            }
            if (!calendarOpenedFromHistory) {
                Haptics.vibrate(appContext, settings.vibrationEnabled, Haptics.Kind.SUCCESS)
                screen = Screen.SUCCESS
                DebugLog.ok("FLOW", "Full flow finished without errors (capture -> extract -> calendar -> DB)")
            }
        }
    }

    fun addToCalendarFromHistory(event: EventEntity) {
        lastSavedEvent = event
        calendarOpenedFromHistory = true
        try {
            requestCalendarInsert(event)
        } catch (e: ScanException) {
            showError(e)
        }
    }

    // ---------------------------------------------------------------- navigation

    fun openHistory() {
        // The list is already up to date thanks to observeHistory() (Room Flow).
        screen = Screen.HISTORY
    }

    fun openSettings() {
        if (screen != Screen.SETTINGS) screenBeforeSettings = screen
        screen = Screen.SETTINGS
    }

    fun goHome() {
        screen = Screen.CAMERA
    }

    fun scanNext() {
        capturedBitmap = null
        form = WrapUpForm.EMPTY
        screen = Screen.CAMERA
    }

    fun dismissError() {
        screen = Screen.CAMERA
    }

    fun toggleFlash() {
        flashEnabled = !flashEnabled
        DebugLog.info("CAMERA", "Flash ${if (flashEnabled) "ON" else "OFF"}")
    }

    fun goBack() {
        when (screen) {
            Screen.PROCESSING -> cancelProcessing()
            Screen.SETTINGS -> screen = screenBeforeSettings
            Screen.CAMERA -> Unit
            else -> screen = Screen.CAMERA
        }
    }

    // ---------------------------------------------------------------- debug (TEMPORARY)

    private var debugErrorIndex = 0

    /** Called by the temporary debug button. Each call shows the next error type. */
    fun triggerDebugError() {
        val errors = listOf(
            ScanException.NoNetwork(),
            ScanException.Llm("DEBUG: simulated model API error (HTTP 500)."),
            ScanException.Timeout(AppConfig.EXTRACTOR_TIMEOUT_MS),
            ScanException.Parse("DEBUG: the model response is not valid JSON."),
            ScanException.Validation("DEBUG: event name is missing."),
            ScanException.CalendarUnavailable("DEBUG: no calendar app found on this device."),
            ScanException.CameraFailure("DEBUG: could not take a photo."),
        )
        val error = errors[debugErrorIndex % errors.size]
        debugErrorIndex++
        DebugLog.warn("DEBUG", "Test error button pressed -> ${error.code}")
        showError(error)
    }

    // ---------------------------------------------------------------- helpers

    private fun showError(error: ScanException) {
        errorCode = error.code
        errorMessage = error.userMessage
        DebugLog.fail(error.code, error.userMessage, error)
        Haptics.vibrate(appContext, settings.vibrationEnabled, Haptics.Kind.ERROR)
        screen = Screen.ERROR
    }

    private fun saveThumbnail(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        return try {
            val dir = File(appContext.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                ImageCompressor.scaleDown(bitmap, AppConfig.THUMBNAIL_MAX_DIMENSION_PX)
                    .compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            DebugLog.ok("THUMBNAIL", "Saved ${file.name}")
            file.absolutePath
        } catch (e: Exception) {
            DebugLog.warn("THUMBNAIL", "Could not save thumbnail: ${e.message}")
            null
        }
    }
}
