package com.warehouse.rfid.edge

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        private val TRIGGER_KEY_CODES = setOf(139, 280, 293, 140)
    }

    private lateinit var repository: SupabaseRepository
    private lateinit var activityType: ActivityType

    private lateinit var llScanHeader: LinearLayout
    private lateinit var tvActivityTitle: TextView
    private lateinit var tvPowerBadge: TextView
    private lateinit var sbPower: SeekBar
    private lateinit var btnLocation: Button
    private lateinit var btnStartPause: Button
    private lateinit var btnSendScan: Button
    private lateinit var btnClearSession: Button
    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanSummary: TextView
    private lateinit var tvSendNotice: TextView
    private lateinit var llBulkRow: LinearLayout
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnBulkRegister: Button
    private lateinit var rvTags: RecyclerView

    private lateinit var tagAdapter: TagAdapter
    private var mReader: RFIDWithUHFUART? = null
    private var currentState = ScanState.INITIALIZING
    private var session: ScanSession? = null
    private var knownLocations: MutableList<String> = mutableListOf()
    private var selectedLocationValue: String? = null
    private var currentPowerDb: Int = POWER_DB_MIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        repository = SupabaseRepository()

        val typeName = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: ActivityType.STOCK_OPNAME.name
        activityType = ActivityType.valueOf(typeName)

        llScanHeader = findViewById(R.id.llScanHeader)
        tvActivityTitle = findViewById(R.id.tvActivityTitle)
        tvPowerBadge = findViewById(R.id.tvPowerBadge)
        sbPower = findViewById(R.id.sbPower)
        btnLocation = findViewById(R.id.btnLocation)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnSendScan = findViewById(R.id.btnSendScan)
        btnClearSession = findViewById(R.id.btnClearSession)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanSummary = findViewById(R.id.tvScanSummary)
        tvSendNotice = findViewById(R.id.tvSendNotice)
        llBulkRow = findViewById(R.id.llBulkRow)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        btnBulkRegister = findViewById(R.id.btnBulkRegister)
        rvTags = findViewById(R.id.rvTags)

        tvActivityTitle.text = activityType.label
        currentPowerDb = activityType.powerDb
        sbPower.max = POWER_DB_MAX - POWER_DB_MIN
        sbPower.progress = currentPowerDb - POWER_DB_MIN
        updatePowerBadge()
        sbPower.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPowerDb = POWER_DB_MIN + progress
                updatePowerBadge()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applyPower(currentPowerDb)
            }
        })
        btnLocation.text = locationButtonLabel()

        val mode = if (activityType == ActivityType.INBOUND) TagAdapterMode.REGISTRATION else TagAdapterMode.RECORD_ACTIVITY
        tagAdapter = TagAdapter(
            emptyList(),
            mode,
            locationProvider = { selectedLocationValue },
            onTagClick = { tag -> showTagInfoDialog(tag) },
            onSelectionChanged = { _, _ -> onSelectionChanged() }
        )
        rvTags.layoutManager = LinearLayoutManager(this)
        rvTags.adapter = tagAdapter

        session = ScanSession(activityType = activityType)
        tagAdapter.updateData(session!!.tagsList)

        llBulkRow.visibility = if (mode == TagAdapterMode.REGISTRATION) View.VISIBLE else View.GONE

        loadKnownLocations()
        btnLocation.setOnClickListener { showLocationMenu() }

        btnStartPause.setOnClickListener { toggleStartPause() }
        btnSendScan.setOnClickListener { sendBatch() }
        btnClearSession.setOnClickListener { resetSession() }

        cbSelectAll.setOnCheckedChangeListener { _, checked -> onSelectAllToggled(checked) }
        btnBulkRegister.setOnClickListener { showBulkRegisterDialog() }
        updateBulkRegisterButton(0)

        updateUiState(ScanState.INITIALIZING)
        initRfid()
    }

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

    private fun updatePowerBadge() {
        val rangeLabel = if (activityType == ActivityType.INBOUND) "Close range" else "Far range"
        tvPowerBadge.text = "RFID Power: $currentPowerDb dB · $rangeLabel"
    }

    private fun selectableTags(): List<TagRow> =
        session?.tagsList?.filter { it.status.isNullOrEmpty() } ?: emptyList()

    private fun selectedTags(): List<TagRow> = selectableTags().filter { it.isSelected }

    private fun updateBulkRegisterButton(selectedCount: Int) {
        btnBulkRegister.isEnabled = selectedCount > 0
        btnBulkRegister.alpha = if (selectedCount > 0) 1f else 0.5f
        btnBulkRegister.text = if (selectedCount > 0) "BULK REGISTER ($selectedCount)" else "BULK REGISTER"
    }

    private fun onSelectionChanged() {
        tagAdapter.setSelectionModeEnabled(true)
        updateBulkRegisterButton(selectedTags().size)
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = selectedTags().isNotEmpty() && selectedTags().size == selectableTags().size
        cbSelectAll.setOnCheckedChangeListener { _, checked -> onSelectAllToggled(checked) }
    }

    private fun onSelectAllToggled(checked: Boolean) {
        val tags = selectableTags()
        if (tags.isEmpty()) return
        tags.forEach { it.isSelected = checked }
        tagAdapter.setSelectionModeEnabled(checked || selectedTags().isNotEmpty())
        tagAdapter.updateData(session!!.tagsList)
        updateBulkRegisterButton(selectedTags().size)
    }

    private fun showBulkRegisterDialog() {
        val selected = selectedTags()
        if (selected.isEmpty()) return

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        container.setPadding(padding, padding, padding, padding)

        val infoLabel = TextView(this)
        infoLabel.text = "Applying to ${selected.size} selected tag(s)"
        infoLabel.setTextColor(getColor(R.color.text_secondary))
        infoLabel.textSize = 12f
        infoLabel.setPadding(0, 0, 0, padding)
        container.addView(infoLabel)

        val skuInput = EditText(this)
        skuInput.hint = "SKU (applied to all)"
        container.addView(skuInput)

        val nameInput = EditText(this)
        nameInput.hint = "Product Name (applied to all)"
        container.addView(nameInput)

        val qtyInput = EditText(this)
        qtyInput.hint = "Quantity per tag"
        qtyInput.inputType = InputType.TYPE_CLASS_NUMBER
        qtyInput.setText("1")
        container.addView(qtyInput)

        AlertDialog.Builder(this)
            .setTitle("Bulk Register ${selected.size} Tags")
            .setView(container)
            .setPositiveButton("Apply") { _, _ ->
                val sku = skuInput.text.toString().trim().ifEmpty { null }
                val name = nameInput.text.toString().trim().ifEmpty { null }
                val qty = qtyInput.text.toString().trim().toIntOrNull()?.takeIf { it > 0 } ?: 1
                if (name.isNullOrEmpty()) {
                    Toast.makeText(this, "Product Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                for (tag in selected) {
                    tag.sku = sku
                    tag.productName = name
                    tag.quantity = qty
                    tag.isSelected = false
                }
                tagAdapter.setSelectionModeEnabled(false)
                tagAdapter.updateData(session!!.tagsList)
                cbSelectAll.setOnCheckedChangeListener(null)
                cbSelectAll.isChecked = false
                cbSelectAll.setOnCheckedChangeListener { _, checked -> onSelectAllToggled(checked) }
                updateBulkRegisterButton(0)
                Toast.makeText(this, "Applied to ${selected.size} tag(s)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadKnownLocations() {
        lifecycleScope.launch {
            when (val result = repository.fetchLocations()) {
                is ApiResult.Success -> knownLocations = result.value.toMutableList()
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

    private fun showLocationMenu() {
        val items = mutableListOf("+ Add New Location")
        items.addAll(knownLocations)
        AlertDialog.Builder(this)
            .setTitle("Select ${locationWord()}")
            .setItems(items.toTypedArray()) { _, index ->
                if (index == 0) showNewLocationDialog() else setLocation(knownLocations[index - 1])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNewLocationDialog() {
        val input = EditText(this)
        input.hint = "e.g. Rack A1"
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this)
        container.setPadding(padding, padding, padding, padding)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Add New Location")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    if (!knownLocations.contains(value)) knownLocations.add(value)
                    setLocation(value)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setLocation(value: String) {
        selectedLocationValue = value
        btnLocation.text = locationButtonLabel()
        tagAdapter.refreshLocationColumn()
    }

    private fun selectedLocation(): String? = selectedLocationValue

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
                        tvScanStatus.text = "Status: RFID INIT FAILED"
                        updateUiState(ScanState.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvScanStatus.text = "Status: RFID INIT ERROR"
                    updateUiState(ScanState.ERROR)
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        val needsLocation = activityType == ActivityType.INBOUND || activityType == ActivityType.STOCK_OPNAME || activityType == ActivityType.TRANSFER
        if (needsLocation && selectedLocation().isNullOrEmpty()) {
            tvScanStatus.text = "Status: Location is required"
            Toast.makeText(this, "Select or add a location before scanning", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startScan() {
        if (mReader == null || currentState == ScanState.SCANNING || currentState == ScanState.SENDING) return
        if (!validateFields()) return

        val powerSet = try {
            mReader!!.setPower(currentPowerDb)
        } catch (e: Exception) {
            Log.e("ScanActivity", "setPower failed", e)
            tvScanStatus.text = "Status: FAILED TO SET POWER"
            return
        }
        if (!powerSet) {
            tvScanStatus.text = "Status: FAILED TO SET POWER"
            return
        }

        mReader!!.setInventoryCallback(object : IUHFInventoryCallback {
            override fun callback(info: UHFTAGInfo?) {
                if (info?.epc == null) return
                val epc = info.epc.trim().uppercase()
                if (epc.isEmpty()) return
                val rssi = try { (info.rssi ?: "0").toFloat().toInt() } catch (e: Exception) { 0 }
                runOnUiThread { onTagRead(epc, rssi) }
            }
        })

        if (mReader!!.startInventoryTag()) {
            updateUiState(ScanState.SCANNING)
        } else {
            tvScanStatus.text = "Status: FAILED TO START SCAN"
        }
    }

    private fun onTagRead(epc: String, rssi: Int) {
        val s = session ?: return
        s.totalRawReads += 1

        val existing = s.tagsMap[epc]
        if (existing == null) {
            val newTag = TagRow(epc = epc, readCount = 1, latestRssi = rssi, strongestRssi = rssi)
            s.tagsMap[epc] = newTag
            s.tagsList.add(newTag)
            tagAdapter.notifyItemInserted(s.tagsList.size - 1)

            if (activityType != ActivityType.INBOUND) {
                newTag.lookupState = LookupState.PENDING
                lookupTag(newTag)
            }
        } else {
            existing.readCount += 1
            existing.latestRssi = rssi
            if (rssi > existing.strongestRssi) existing.strongestRssi = rssi
            val index = s.tagsList.indexOf(existing)
            if (index != -1) tagAdapter.notifyItemChanged(index)
        }
        tvScanSummary.text = "Total Tags: ${s.tagsList.size}"
    }

    private fun lookupTag(tag: TagRow) {
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
            val s = session ?: return@launch
            recomputeSkuQuantities()
            tagAdapter.updateData(s.tagsList)
        }
    }

    /**
     * In record-activity mode, several distinct EPCs can share one SKU. The displayed and
     * submitted quantity for that SKU is the count of unique EPCs found for it in this
     * session, not each product's originally registered per-unit quantity.
     */
    private fun recomputeSkuQuantities() {
        val s = session ?: return
        if (activityType == ActivityType.INBOUND) return
        val countsBySku = s.tagsList
            .filter { it.lookupState == LookupState.FOUND && !it.sku.isNullOrEmpty() }
            .groupingBy { it.sku!! }
            .eachCount()
        for (tag in s.tagsList) {
            if (tag.lookupState == LookupState.FOUND && !tag.sku.isNullOrEmpty()) {
                tag.quantity = countsBySku[tag.sku] ?: 1
            }
        }
    }

    private fun toggleStartPause() {
        if (currentState == ScanState.SCANNING) pauseScan() else startScan()
    }

    private fun pauseScan() {
        if (mReader == null || currentState != ScanState.SCANNING) return
        if (mReader!!.stopInventory()) {
            updateUiState(ScanState.PAUSED)
        }
    }

    private fun showTagInfoDialog(tag: TagRow) {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        container.setPadding(padding, padding, padding, padding)

        val epcLabel = TextView(this)
        epcLabel.text = "EPC"
        epcLabel.textSize = 11f
        epcLabel.setTextColor(getColor(R.color.text_secondary))
        container.addView(epcLabel)

        val epcValue = TextView(this)
        epcValue.text = tag.epc
        epcValue.textSize = 14f
        epcValue.typeface = android.graphics.Typeface.MONOSPACE
        epcValue.setTextColor(getColor(R.color.text_primary))
        epcValue.setTextIsSelectable(true)
        epcValue.setPadding(0, 0, 0, padding)
        container.addView(epcValue)

        val skuInput = EditText(this)
        skuInput.hint = "SKU"
        skuInput.setText(tag.sku ?: "")
        container.addView(skuInput)

        val nameInput = EditText(this)
        nameInput.hint = "Product Name"
        nameInput.setText(tag.productName ?: "")
        container.addView(nameInput)

        val qtyInput = EditText(this)
        qtyInput.hint = "Quantity"
        qtyInput.inputType = InputType.TYPE_CLASS_NUMBER
        qtyInput.setText((tag.quantity ?: 1).toString())
        container.addView(qtyInput)

        AlertDialog.Builder(this)
            .setTitle("Register Tag")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                tag.sku = skuInput.text.toString().trim().ifEmpty { null }
                tag.productName = nameInput.text.toString().trim().ifEmpty { null }
                tag.quantity = qtyInput.text.toString().trim().toIntOrNull()?.takeIf { it > 0 } ?: 1
                val index = session?.tagsList?.indexOf(tag) ?: -1
                if (index != -1) tagAdapter.notifyItemChanged(index)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendBatch() {
        val s = session ?: return
        if (s.tagsList.isEmpty()) return
        if (!validateFields()) return

        if (activityType == ActivityType.INBOUND) {
            val missing = s.tagsList.any { it.sku.isNullOrEmpty() || it.productName.isNullOrEmpty() }
            if (missing) {
                tvScanStatus.text = "Status: Fill SKU/Name for all tags before sending"
                return
            }
        }

        updateUiState(ScanState.SENDING)
        val location = selectedLocation()
        val sendCount = s.tagsList.size

        showSendNotice("Sending $sendCount tag(s) to database...", getColor(R.color.brand_primary))
        Toast.makeText(this, "Sending $sendCount tag(s) to database...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            when (val result = repository.submitActivityBatch(activityType, location, s.tagsList)) {
                is ApiResult.Success -> {
                    applyResponse(result.value)
                    updateUiState(ScanState.SUCCESS)
                }
                is ApiResult.Failure -> {
                    tvScanStatus.text = "Status: SEND FAILED - ${result.message}"
                    showSendNotice("✗ Not sent to database — ${result.message}", getColor(R.color.status_rejected))
                    Toast.makeText(this@ScanActivity, "Send failed: ${result.message}", Toast.LENGTH_LONG).show()
                    updateUiState(ScanState.ERROR)
                }
            }
        }
    }

    private fun showSendNotice(message: String, backgroundColor: Int) {
        tvSendNotice.text = message
        tvSendNotice.setBackgroundColor(backgroundColor)
        tvSendNotice.setTextColor(getColor(R.color.text_on_brand))
        tvSendNotice.visibility = View.VISIBLE
    }

    private fun applyResponse(result: BatchSubmitResult) {
        tvScanSummary.text = "Accepted: ${result.acceptedCount} | Rejected: ${result.rejectedCount} | Unknown: ${result.unknownCount}"
        tvScanStatus.text = "Status: SUCCESS"
        showSendNotice(
            "✓ Sent to database — ${result.acceptedCount} accepted, ${result.rejectedCount} rejected, ${result.unknownCount} unknown",
            getColor(R.color.status_available)
        )
        Toast.makeText(
            this,
            "Sent to database: ${result.acceptedCount} accepted, ${result.rejectedCount} rejected, ${result.unknownCount} unknown",
            Toast.LENGTH_LONG
        ).show()

        val s = session ?: return
        for (item in result.items) {
            val tag = s.tagsMap[item.epc] ?: continue
            tag.status = item.status
            tag.reason = item.reason
            if (!item.productName.isNullOrEmpty()) {
                tag.productName = item.productName
            }
        }
        tagAdapter.updateData(s.tagsList)
    }

    private fun resetSession() {
        session = ScanSession(activityType = activityType)
        tagAdapter.setSelectionModeEnabled(false)
        tagAdapter.updateData(session!!.tagsList)
        tvScanSummary.text = "Total Tags: 0"
        tvSendNotice.visibility = View.GONE
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = false
        cbSelectAll.setOnCheckedChangeListener { _, checked -> onSelectAllToggled(checked) }
        updateBulkRegisterButton(0)
        updateUiState(ScanState.READY)
    }

    private fun updateUiState(state: ScanState) {
        currentState = state
        if (state != ScanState.ERROR) {
            tvScanStatus.text = "Status: ${state.name}"
        }
        sbPower.isEnabled = state != ScanState.SCANNING && state != ScanState.SENDING && state != ScanState.INITIALIZING

        val brandColor = getColor(R.color.brand_primary)
        val pausedStateColor = getColor(R.color.text_muted)
        when (state) {
            ScanState.INITIALIZING -> {
                btnStartPause.isEnabled = false
                btnStartPause.text = "Start"
                btnStartPause.backgroundTintList = android.content.res.ColorStateList.valueOf(brandColor)
                btnSendScan.isEnabled = false
                btnClearSession.visibility = View.GONE
            }
            ScanState.READY -> {
                btnStartPause.isEnabled = true
                btnStartPause.text = "Start"
                btnStartPause.backgroundTintList = android.content.res.ColorStateList.valueOf(brandColor)
                btnSendScan.isEnabled = false
                btnClearSession.visibility = View.GONE
            }
            ScanState.SCANNING -> {
                btnStartPause.isEnabled = true
                btnStartPause.text = "Pause"
                btnStartPause.backgroundTintList = android.content.res.ColorStateList.valueOf(pausedStateColor)
                btnSendScan.isEnabled = false
                btnClearSession.visibility = View.GONE
            }
            ScanState.PAUSED -> {
                btnStartPause.isEnabled = true
                btnStartPause.text = "Start"
                btnStartPause.backgroundTintList = android.content.res.ColorStateList.valueOf(brandColor)
                btnSendScan.isEnabled = (session?.tagsList?.isNotEmpty() == true)
                btnClearSession.visibility = View.VISIBLE
                btnClearSession.text = "Clear"
            }
            ScanState.SENDING -> {
                btnStartPause.isEnabled = false
                btnSendScan.isEnabled = false
                btnClearSession.visibility = View.GONE
            }
            ScanState.SUCCESS -> {
                btnStartPause.isEnabled = false
                btnSendScan.isEnabled = false
                btnClearSession.visibility = View.VISIBLE
                btnClearSession.text = "New"
            }
            ScanState.ERROR -> {
                btnStartPause.isEnabled = false
                btnSendScan.isEnabled = (session?.tagsList?.isNotEmpty() == true)
                btnClearSession.visibility = View.VISIBLE
                btnClearSession.text = "Clear"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (currentState == ScanState.SCANNING) {
            pauseScan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val reader = mReader
        if (reader != null) {
            if (currentState == ScanState.SCANNING) {
                reader.stopInventory()
            }
            reader.free()
            mReader = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in TRIGGER_KEY_CODES) {
            if (event?.repeatCount == 0 && currentState != ScanState.SCANNING && currentState != ScanState.SENDING) {
                startScan()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in TRIGGER_KEY_CODES) {
            if (currentState == ScanState.SCANNING) {
                pauseScan()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
