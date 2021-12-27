package ch.heigvd.iict.sym_labo4

import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity
import android.bluetooth.BluetoothAdapter
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter
import android.os.Bundle
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import androidx.lifecycle.ViewModelProvider
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import java.util.*

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * Updated by Berney Alec & Forestier Quentin & Herzig Melvyn on 21.12.2021
 * (C) 2019 - HEIG-VD, IICT
 */
class BleActivity : BaseTemplateActivity() {
    //system services
    private lateinit var bluetoothAdapter: BluetoothAdapter

    //view model
    private lateinit var bleViewModel: BleOperationsViewModel

    //gui elements
    private lateinit var operationPanel: View
    private lateinit var scanPanel: View
    private lateinit var scanResults: ListView
    private lateinit var emptyScanResults: TextView

    private lateinit var temperatureText: TextView
    private lateinit var temperatureButton: Button

    private lateinit var clickCounterText: TextView

    private lateinit var currenttimeText: TextView
    private lateinit var currenttimeButton: Button

    private lateinit var sendvalueInput: EditText
    private lateinit var sendvalueButton: Button

    //menu elements
    private var scanMenuBtn: MenuItem? = null
    private var disconnectMenuBtn: MenuItem? = null

    //adapters
    private lateinit var scanResultsAdapter: ResultsAdapter

    //states
    private var handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        //enable and start bluetooth - initialize bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        //link GUI
        operationPanel = findViewById(R.id.ble_operation)
        scanPanel = findViewById(R.id.ble_scan)
        scanResults = findViewById(R.id.ble_scanresults)
        emptyScanResults = findViewById(R.id.ble_scanresults_empty)

        temperatureText = findViewById(R.id.ble_temperature_text)
        temperatureButton = findViewById(R.id.ble_temperature_button)

        clickCounterText = findViewById(R.id.ble_clickcounter_text)

        currenttimeText = findViewById(R.id.ble_currenttime_text)
        currenttimeButton = findViewById(R.id.ble_currenttime_button)

        sendvalueInput = findViewById(R.id.ble_sendvalue_input)
        sendvalueButton = findViewById(R.id.ble_sendvalue_button)

        //manage scanned item
        scanResultsAdapter = ResultsAdapter(this)
        scanResults.adapter = scanResultsAdapter
        scanResults.emptyView = emptyScanResults

        //connect to view model
        bleViewModel = ViewModelProvider(this).get(BleOperationsViewModel::class.java)

        updateGui()

        //events
        scanResults.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            runOnUiThread {
                //we stop scanning
                scanLeDevice(false)
                //we connect
                bleViewModel.connect(scanResultsAdapter.getItem(position).device)
            }
        }

        currenttimeButton.setOnClickListener {
            bleViewModel.setDate(Calendar.getInstance())
        }

        temperatureButton.setOnClickListener {
            bleViewModel.readTemperature()
        }

        sendvalueButton.setOnClickListener {
            bleViewModel.sendInteger(sendvalueInput.text.toString().toInt())
        }

        //ble events
        bleViewModel.isConnected.observe(this, { updateGui() })

        bleViewModel.temperatureValue.observe(this, {updateValue()})
        bleViewModel.buttonClickValue.observe(this, {updateValue()})
        bleViewModel.currenttimeValue.observe(this, {updateValue()})
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ble_menu, menu)
        //we link the two menu items
        scanMenuBtn = menu.findItem(R.id.menu_ble_search)
        disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect)
        //we update the gui
        updateGui()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_ble_search) {
            if (isScanning) scanLeDevice(false) else scanLeDevice(true)
            return true
        } else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) scanLeDevice(false)
        if (isFinishing) bleViewModel.disconnect()
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result list
     */
    private fun updateGui() {
        val isConnected = bleViewModel.isConnected.value
        if (isConnected != null && isConnected) {

            scanPanel.visibility = View.GONE
            operationPanel.visibility = View.VISIBLE

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                scanMenuBtn!!.isVisible = false
                disconnectMenuBtn!!.isVisible = true
            }
        } else {
            operationPanel.visibility = View.GONE
            scanPanel.visibility = View.VISIBLE

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                disconnectMenuBtn!!.isVisible = false
                scanMenuBtn!!.isVisible = true
            }
        }
    }

    //this method needs user grant localisation and/or bluetooth permissions, our demo app is requesting them on MainActivity
    private fun scanLeDevice(enable: Boolean) {
        val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (enable) {
            //config
            val builderScanSettings = ScanSettings.Builder()
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            builderScanSettings.setReportDelay(0)

            //we scan for any BLE device
            //we filter them based on advertised service:
            //"SYM" (UUID: "3c0a1000-281d-4b48-b2a7-f15579a1c38f")
            val uuidFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(("3c0a1000-281d-4b48-b2a7-f15579a1c38f")))
                .build()

            //reset display
            scanResultsAdapter.clear()
            bluetoothScanner.startScan(
                listOf(uuidFilter),
                builderScanSettings.build(),
                leScanCallback
            )
            Log.d(TAG, "Start scanning...")
            isScanning = true

            //we scan only for 15 seconds
            handler.postDelayed({ scanLeDevice(false) }, 15 * 1000L)
        } else {
            bluetoothScanner.stopScan(leScanCallback)
            isScanning = false
            Log.d(TAG, "Stop scanning (manual)")
        }
    }

    private fun updateValue(){
        temperatureText.text = bleViewModel.temperatureValue.value
        currenttimeText.text = bleViewModel.currenttimeValue.value
        clickCounterText.text = bleViewModel.buttonClickValue.value.toString()
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread { scanResultsAdapter.addDevice(result) }
        }
    }

    companion object {
        private val TAG = BleActivity::class.java.simpleName
    }
}