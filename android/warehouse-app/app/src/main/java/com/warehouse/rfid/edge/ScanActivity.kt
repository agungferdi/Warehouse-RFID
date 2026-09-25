package com.warehouse.rfid.edge

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback
import com.warehouse.rfid.edge.ui.components.ChipTone
import com.warehouse.rfid.edge.ui.screens.BulkRegisterSheet
import com.warehouse.rfid.edge.ui.screens.LocationPickerSheet
import com.warehouse.rfid.edge.ui.screens.ScanScreen
import com.warehouse.rfid.edge.ui.screens.ScanScreenCallbacks
import com.warehouse.rfid.edge.ui.screens.ScanScreenState
import com.warehouse.rfid.edge.ui.screens.SendNoticeUi
import com.warehouse.rfid.edge.ui.screens.TagEditSheet
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        private val TRIGGER_KEY_CODES = setOf(139, 280, 293, 140)
        private const val BEEP_MIN_INTERVAL_MS = 120L
    }

    private val repository = SupabaseRepository()
    private lateinit var activityType: ActivityType
    private var mReader: RFIDWithUHFUART? = null
    private var tagDetectedTone: ToneGenerator? = null
    private var lastBeepAtMs = 0L

    // Loaded once when the screen opens instead of one network round trip per tag — see
    // SupabaseRepository.fetchProductsByEpc for why per-tag lookups caused the lag.
    private var productCache: Map<String, ProductLookup> = emptyMap()
    private var productCacheReady = false

    // Compose-observable state. Mirrors the old ScanSession/notifyDataSetChanged model:
    // tagsState is a SnapshotStateList, and mutating a TagRow's `var` fields in place still
    // requires an explicit `touchTag`/`touchAllTags` (re-set at the same index) to notify
    // Compose, since plain field mutation on a non-State object isn't observed.
    private val tagsState = mutableStateListOf<TagRow>()
    private val tagsMap = mutableMapOf<String, TagRow>()

    // Written from the RFID reader's own callback thread (see setInventoryCallback below), so it
    // must be a thread-safe set, unlike tagsMap/tagsState which are UI-thread-only.
    private val seenEpcs = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var totalRawReads = 0

    private var scanState by mutableStateOf(ScanState.INITIALIZING)
    private var currentPowerDb by mutableStateOf(POWER_DB_MIN)
    private var selectedLocationValue by mutableStateOf<String?>(null)
    private val knownLocations = mutableStateListOf<String>()
    private var statusText by mutableStateOf("Status: INITIALIZING")
    private var totalTagsText by mutableStateOf("Total Tags: 0")
    private var sendNotice by mutableStateOf<SendNoticeUi?>(null)
    private var selectionModeEnabled by mutableStateOf(false)

    private var showLocationSheet by mutableStateOf(false)
    private var showBulkRegisterSheet by mutableStateOf(false)
    private var editingTag by mutableStateOf<TagRow?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typeName = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: ActivityType.STOCK_OPNAME.name
        activityType = ActivityType.valueOf(typeName)
        currentPowerDb = activityType.powerDb

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
        tagDetectedTone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)

        loadKnownLocations()
        if (activityType != ActivityType.INBOUND) {
            loadProductCache()
        }

        setContent {
            WarehouseAppTheme {
                val selectedCount = tagsState.count { it.status.isNullOrEmpty() && it.isSelected }

                ScanScreen(
                    state = ScanScreenState(
                        activityType = activityType,
                        scanState = scanState,
                        currentPowerDb = currentPowerDb,
                        powerMin = POWER_DB_MIN,
                        powerMax = POWER_DB_MAX,
                        locationLabel = locationButtonLabel(),
                        statusText = statusText,
                        totalTagsText = totalTagsText,
                        sendNotice = sendNotice,
                        tags = tagsState,
                        isRegistrationMode = activityType == ActivityType.INBOUND,
                        selectionModeEnabled = selectionModeEnabled,
                        selectedCount = selectedCount,
                        bulkRegisterEnabled = selectedCount > 0,
                    ),
                    callbacks = remember {
                        ScanScreenCallbacks(
                            onBack = { finish() },
                            onPowerChange = { currentPowerDb = it },
                            onPowerChangeFinished = { applyPower(currentPowerDb) },
                            onLocationClick = { showLocationSheet = true },
                            onStartPause = { toggleStartPause() },
                            onSend = { sendBatch() },
                            onClear = { resetSession() },
                            onSelectAllToggle = { checked -> onSelectAllToggled(checked) },
                            onBulkRegisterClick = { showBulkRegisterSheet = true },
                            onTagClick = { tag ->
                                if (activityType == ActivityType.INBOUND && tag.status.isNullOrEmpty()) {
                                    if (selectionModeEnabled) toggleTagSelection(tag, !tag.isSelected) else editingTag = tag
                                }
                            },
                            onTagLongPress = { tag -> toggleTagSelection(tag, true) },
                            onTagSelectToggle = { tag, checked -> toggleTagSelection(tag, checked) },
                        )
                    },
                )

                if (showLocationSheet) {
                    LocationPickerSheet(
                        locationWord = locationWord(),
                        knownLocations = knownLocations,
                        onSelect = { value ->
                            setLocation(value)
                            showLocationSheet = false
                        },
                        onDismiss = { showLocationSheet = false },
                    )
                }

                if (showBulkRegisterSheet) {
                    BulkRegisterSheet(
                        selectedCount = selectedCount,
                        onApply = { sku, name -> applyBulkRegister(sku, name) },
                        onDismiss = { showBulkRegisterSheet = false },
                    )
                }

                editingTag?.let { tag ->
                    TagEditSheet(
                        tag = tag,
                        onSave = { sku, name -> saveTagEdit(tag, sku, name) },
                        onDismiss = { editingTag = null },
                    )
                }
            }
        }

        updateUiState(ScanState.INITIALIZING)
        initRfid()
    }

    // ---- Tag list mutation helpers (Compose-visibility adapter over in-place TagRow mutation) ----

    private fun touchTag(tag: TagRow) {
        val index = tagsState.indexOf(tag)
        if (index != -1) tagsState[index] = tag
    }

    private fun touchAllTags() {
        for (i in tagsState.indices) tagsState[i] = tagsState[i]
    }

    private fun toggleTagSelection(tag: TagRow, checked: Boolean) {
        tag.isSelected = checked
        touchTag(tag)
        onSelectionChanged()
    }

    // ---- Power ----

    private fun applyPower(powerDb: Int, showToast: Boolean = true) {
        val reader = mReader ?: return
        try {
            val ok = reader.setPower(powerDb)
            if (showToast) {
                if (ok) {
                    Toast.makeText(this, "RFID power set to $powerDb dB", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to set power to $powerDb dB", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ScanActivity", "setPower failed", e)
            if (showToast) {
                Toast.makeText(this, "Failed to set power to $powerDb dB", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Selection / bulk register ----

    private fun selectableTags(): List<TagRow> = tagsState.filter { it.status.isNullOrEmpty() }

    private fun selectedTags(): List<TagRow> = selectableTags().filter { it.isSelected }

    private fun onSelectionChanged() {
        selectionModeEnabled = true
    }

    private fun onSelectAllToggled(checked: Boolean) {
        val tags = selectableTags()
        if (tags.isEmpty()) return
        tags.forEach { it.isSelected = checked }
        touchAllTags()
        selectionModeEnabled = checked || selectedTags().isNotEmpty()
    }

    // Every registered EPC is exactly one physical unit, so quantity is always 1 per tag —
    // never entered manually. A SKU's total on-hand count is just how many EPC rows share it.
    private fun applyBulkRegister(sku: String?, name: String) {
        val selected = selectedTags()
        if (selected.isEmpty()) return
        for (tag in selected) {
            tag.sku = sku
            tag.productName = name
            tag.quantity = 1
            tag.isSelected = false
        }
        touchAllTags()
        selectionModeEnabled = false
        showBulkRegisterSheet = false
        Toast.makeText(this, "Applied to ${selected.size} tag(s)", Toast.LENGTH_SHORT).show()
    }

    private fun saveTagEdit(tag: TagRow, sku: String?, name: String?) {
        tag.sku = sku
        tag.productName = name
        tag.quantity = 1
        touchTag(tag)
        editingTag = null
    }

    // ---- Location ----

    private fun loadKnownLocations() {
        lifecycleScope.launch {
            when (val result = repository.fetchLocations()) {
                is ApiResult.Success -> {
                    knownLocations.clear()
                    knownLocations.addAll(result.value)
                }
                is ApiResult.Failure -> { /* selection list just stays empty; user can still add a new location */ }
            }
        }
    }

    private fun locationWord(): String = when (activityType) {
        ActivityType.TRANSFER -> "Destination"
        else -> "Location"
    }

    private fun locationButtonLabel(): String {
        val value = selectedLocationValue
        return if (value.isNullOrEmpty()) "Set ${locationWord().lowercase()}" else "${locationWord()}: $value"
    }

    private fun setLocation(value: String) {
        if (value.isBlank()) return
        selectedLocationValue = value
        if (!knownLocations.contains(value)) knownLocations.add(value)
    }

    private fun selectedLocation(): String? = selectedLocationValue

    // ---- RFID lifecycle ----

    private fun initRfid() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mReader = RFIDWithUHFUART.getInstance()
                val ok = mReader?.init(this@ScanActivity) ?: false
                withContext(Dispatchers.Main) {
                    if (ok) {
                        updateUiState(ScanState.READY)
                        applyPower(currentPowerDb, showToast = false)
                    } else {
                        statusText = "Status: RFID INIT FAILED"
                        updateUiState(ScanState.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText = "Status: RFID INIT ERROR"
                    updateUiState(ScanState.ERROR)
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        val needsLocation = activityType == ActivityType.INBOUND || activityType == ActivityType.STOCK_OPNAME || activityType == ActivityType.TRANSFER
        if (needsLocation && selectedLocation().isNullOrEmpty()) {
            statusText = "Status: Location is required"
            Toast.makeText(this, "Select or add a location before scanning", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startScan() {
        if (mReader == null || scanState == ScanState.SCANNING || scanState == ScanState.SENDING) return
        if (!validateFields()) return

        val powerSet = try {
            mReader!!.setPower(currentPowerDb)
        } catch (e: Exception) {
            Log.e("ScanActivity", "setPower failed", e)
            statusText = "Status: FAILED TO SET POWER"
            return
        }
        if (!powerSet) {
            statusText = "Status: FAILED TO SET POWER"
            return
        }

        mReader!!.setInventoryCallback(object : IUHFInventoryCallback {
            // Runs on the SDK's own reader thread, not the UI thread. A tag sitting in range
            // gets read tens of times a second, so this must stay off the main thread entirely —
            // posting every raw read via runOnUiThread was flooding the main looper's queue and
            // starving the async product-lookup coroutine's resumption behind it, which is why
            // SKU/Product Name/Qty only "caught up" once scanning paused and the queue drained.
            // The beep (an audio call, not a UI op) and the dedup check both happen right here;
            // only a genuinely new EPC is ever handed to the UI thread.
            override fun callback(info: UHFTAGInfo?) {
                if (info?.epc == null) return
                val epc = info.epc.trim().uppercase()
                if (epc.isEmpty()) return
                val rssi = try { (info.rssi ?: "0").toFloat().toInt() } catch (e: Exception) { 0 }

                totalRawReads += 1
                val now = SystemClock.elapsedRealtime()
                if (now - lastBeepAtMs >= BEEP_MIN_INTERVAL_MS) {
                    lastBeepAtMs = now
                    tagDetectedTone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 60)
                }

                if (seenEpcs.add(epc)) {
                    runOnUiThread { onNewTagDetected(epc, rssi) }
                }
            }
        })

        if (mReader!!.startInventoryTag()) {
            updateUiState(ScanState.SCANNING)
        } else {
            statusText = "Status: FAILED TO START SCAN"
        }
    }

    /** Called on the UI thread, and only once per EPC (the dedup gate lives on the reader thread). */
    private fun onNewTagDetected(epc: String, rssi: Int) {
        if (tagsMap.containsKey(epc)) return
        val newTag = TagRow(epc = epc, readCount = 1, latestRssi = rssi, strongestRssi = rssi)
        tagsMap[epc] = newTag
        tagsState.add(newTag)

        if (activityType != ActivityType.INBOUND) {
            newTag.lookupState = LookupState.PENDING
            lookupTag(newTag)
        }
        totalTagsText = "Total Tags: ${tagsState.size}"
    }

    private fun loadProductCache() {
        lifecycleScope.launch {
            when (val result = repository.fetchProductsByEpc()) {
                is ApiResult.Success -> {
                    productCache = result.value
                    productCacheReady = true
                    // Tags already read before the cache landed were left PENDING via the
                    // per-tag fallback below — resolve them instantly now that it's here.
                    for (tag in tagsState) {
                        if (tag.lookupState == LookupState.PENDING) applyCachedLookup(tag)
                    }
                    touchAllTags()
                }
                is ApiResult.Failure -> Unit // per-tag fallback in lookupTag still works
            }
        }
    }

    private fun applyCachedLookup(tag: TagRow) {
        val product = productCache[tag.epc]
        if (product != null) {
            tag.lookupState = LookupState.FOUND
            tag.sku = product.sku
            tag.productName = product.productName
            tag.quantity = product.quantity
            tag.location = product.location
        } else {
            tag.lookupState = LookupState.NOT_FOUND
        }
    }

    private fun lookupTag(tag: TagRow) {
        if (productCacheReady) {
            applyCachedLookup(tag)
            touchAllTags()
            return
        }
        lifecycleScope.launch {
            when (val result = repository.lookupProduct(tag.epc)) {
                is ApiResult.Success -> {
                    val product = result.value
                    if (product != null) {
                        tag.lookupState = LookupState.FOUND
                        tag.sku = product.sku
                        tag.productName = product.productName
                        tag.quantity = product.quantity
                        tag.location = product.location
                    } else {
                        tag.lookupState = LookupState.NOT_FOUND
                    }
                }
                is ApiResult.Failure -> {
                    tag.lookupState = LookupState.NOT_FOUND
                }
            }
            touchAllTags()
        }
    }

    private fun toggleStartPause() {
        if (scanState == ScanState.SCANNING) pauseScan() else startScan()
    }

    private fun pauseScan() {
        if (mReader == null || scanState != ScanState.SCANNING) return
        if (mReader!!.stopInventory()) {
            updateUiState(ScanState.PAUSED)
        }
    }

    // ---- Send ----

    private fun sendBatch() {
        if (tagsState.isEmpty()) return
        if (!validateFields()) return

        if (activityType == ActivityType.INBOUND) {
            val missing = tagsState.any { it.sku.isNullOrEmpty() || it.productName.isNullOrEmpty() }
            if (missing) {
                statusText = "Status: Fill SKU/Name for all tags before sending"
                return
            }
        }

        updateUiState(ScanState.SENDING)
        val location = selectedLocation()
        val sendCount = tagsState.size

        sendNotice = SendNoticeUi("Sending $sendCount tag(s) to database...", ChipTone.Info)
        Toast.makeText(this, "Sending $sendCount tag(s) to database...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            when (val result = repository.submitActivityBatch(activityType, location, tagsState.toList())) {
                is ApiResult.Success -> {
                    applyResponse(result.value)
                    updateUiState(ScanState.SUCCESS)
                }
                is ApiResult.Failure -> {
                    statusText = "Status: SEND FAILED - ${result.message}"
                    sendNotice = SendNoticeUi("Not sent to database — ${result.message}", ChipTone.Error)
                    Toast.makeText(this@ScanActivity, "Send failed: ${result.message}", Toast.LENGTH_LONG).show()
                    updateUiState(ScanState.ERROR)
                }
            }
        }
    }

    private fun applyResponse(result: BatchSubmitResult) {
        totalTagsText = "Accepted: ${result.acceptedCount} | Rejected: ${result.rejectedCount} | Unknown: ${result.unknownCount}"
        statusText = "Status: SUCCESS"
        sendNotice = SendNoticeUi(
            "Sent to database — ${result.acceptedCount} accepted, ${result.rejectedCount} rejected, ${result.unknownCount} unknown",
            ChipTone.Success,
        )
        Toast.makeText(
            this,
            "Sent to database: ${result.acceptedCount} accepted, ${result.rejectedCount} rejected, ${result.unknownCount} unknown",
            Toast.LENGTH_LONG,
        ).show()

        for (item in result.items) {
            val tag = tagsMap[item.epc] ?: continue
            tag.status = item.status
            tag.reason = item.reason
            if (!item.productName.isNullOrEmpty()) {
                tag.productName = item.productName
            }
        }
        touchAllTags()
    }

    private fun resetSession() {
        tagsState.clear()
        tagsMap.clear()
        seenEpcs.clear()
        totalRawReads = 0
        selectionModeEnabled = false
        totalTagsText = "Total Tags: 0"
        sendNotice = null
        updateUiState(ScanState.READY)
    }

    // ---- State machine ----

    private fun updateUiState(state: ScanState) {
        scanState = state
        if (state != ScanState.ERROR) {
            statusText = "Status: ${state.name}"
        }
    }

    override fun onPause() {
        super.onPause()
        if (scanState == ScanState.SCANNING) {
            pauseScan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val reader = mReader
        if (reader != null) {
            if (scanState == ScanState.SCANNING) {
                reader.stopInventory()
            }
            reader.free()
            mReader = null
        }
        tagDetectedTone?.release()
        tagDetectedTone = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in TRIGGER_KEY_CODES) {
            if (event?.repeatCount == 0 && scanState != ScanState.SENDING) {
                toggleStartPause()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in TRIGGER_KEY_CODES) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
