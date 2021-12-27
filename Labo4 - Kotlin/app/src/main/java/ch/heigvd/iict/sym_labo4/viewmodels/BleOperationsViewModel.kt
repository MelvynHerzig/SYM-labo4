package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.io.ByteArrayInputStream
import java.time.Year
import java.util.*

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 18.10.2021
 * Updated by Alec Berney & Quentin Forestier & Melvyn Herzig on 26.12.2021
 * (C) 2019 - HEIG-VD, IICT
 */
class BleOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private var ble = SYMBleManager(application.applicationContext)
    private var mConnection: BluetoothGatt? = null

    //live data - observer
    val isConnected = MutableLiveData(false)
    val temperatureValue = MutableLiveData("Not available")
    val buttonClickValue = MutableLiveData(0)
    val currenttimeValue = MutableLiveData("DD / MM / YY hh:mm:ss")

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    // Services UUID
    private var symServiceUUID = UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")
    private var integerUUID = UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f")
    private var temperatureUUID = UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f")
    private var buttonClickUUID = UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f")

    private var timeServiceUUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    private var currentTimeUUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb")


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        ble.disconnect()
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                .retry(1, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        ble.disconnect()
        mConnection?.disconnect()
    }

    /*
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */

    /**
     * Read the temperature from the ble device using ble manager
     */
    fun readTemperature(): Boolean {
        if (!isConnected.value!! || temperatureChar == null)
            return false
        else
            return ble.readTemperature()
    }

    /**
     * Send an integer using the ble manager
     * @param integer integer to send
     */
    fun sendInteger(integer: Int): Boolean {
        if (!isConnected.value!! || integerChar == null)
            return false
        else
            return ble.sendInteger(integer)
    }

    /**
     * Set date using the ble manager
     * @param calendar Date to set
     */
    fun setDate(calendar: Calendar): Boolean {
        if (!isConnected.value!! || currentTimeChar == null)
            return false
        else
            return ble.setDate(calendar)
    }

    private val bleConnectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnecting")
            isConnected.value = false
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnected")
            isConnected.value = true
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceDisconnecting")
            isConnected.value = false
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceReady")
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(TAG, "onDeviceFailedToConnect")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if (reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported")
                Toast.makeText(
                    getApplication(),
                    "Device not supported - implement method isRequiredServiceSupported()",
                    Toast.LENGTH_LONG
                ).show()
            } else
                Log.d(TAG, "onDeviceDisconnected")
            isConnected.value = false
        }

    }

    private inner class SYMBleManager(applicationContext: Context) :
        BleManager(applicationContext) {
        /**
         * BluetoothGatt callbacks object.
         */
        private var mGattCallback: BleManagerGattCallback? = null

        public override fun getGattCallback(): BleManagerGattCallback {
            //we initiate the mGattCallback on first call, singleton
            if (mGattCallback == null) {
                mGattCallback = object : BleManagerGattCallback() {

                    public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                        mConnection = gatt //trick to force disconnection

                        /*
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 39 à 44)
                        */

                        initServices(mConnection!!.services)

                        return (timeService != null && currentTimeChar != null &&
                                symService != null && temperatureChar != null && buttonClickChar != null && integerChar != null
                                )

                    }

                    override fun initialize() {
                        /*
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */

                        setNotificationCallback(buttonClickChar).with { _: BluetoothDevice, data: Data ->
                            buttonClickValue.value = data.getIntValue(Data.FORMAT_UINT8, 0)
                        }


                        setNotificationCallback(currentTimeChar).with { _: BluetoothDevice, data: Data ->

                            currenttimeValue.value = String.format(
                                "%02d/%02d/%02d %02d:%02d:%02d",
                                data.getIntValue(Data.FORMAT_UINT8, 3),  // Day
                                data.getIntValue(Data.FORMAT_UINT8, 2),  // Month
                                data.getIntValue(Data.FORMAT_UINT16, 0), // Year
                                data.getIntValue(Data.FORMAT_UINT8, 4),  // Hour
                                data.getIntValue(Data.FORMAT_UINT8, 5),  // Second
                                data.getIntValue(Data.FORMAT_UINT8, 6)   // Minute
                            )
                        }

                        enableNotifications(buttonClickChar).enqueue()
                        enableNotifications(currentTimeChar).enqueue()

                        readTemperature()
                    }

                    override fun onServicesInvalidated() {
                        //we reset services and characteristics
                        timeService = null
                        currentTimeChar = null
                        symService = null
                        integerChar = null
                        temperatureChar = null
                        buttonClickChar = null
                    }
                }
            }
            return mGattCallback!!
        }

        /**
         * Init the supported services using their UUID
         * @param services List of services available
         */
        private fun initServices(services: List<BluetoothGattService>) {
            for (service in services) {

                when (service.uuid) {
                    timeServiceUUID -> {
                        timeService = service
                        initTimeCharacteristics(timeService!!.characteristics)
                    }
                    symServiceUUID -> {
                        symService = service
                        initSYMCharacteristics(symService!!.characteristics)
                    }
                }

            }
        }

        /**
         * Init the supported characteristics of Time service using their UUID
         * @param characteristics List of characteristics of Time service
         */
        private fun initTimeCharacteristics(characteristics: List<BluetoothGattCharacteristic>) {
            for (char in characteristics) {
                when (char.uuid) {
                    currentTimeUUID -> currentTimeChar = char
                }
            }
        }

        /**
         * Init the supported characteristics of SYM service using their UUID
         * @param characteristics List of characteristics of SYM service
         */
        private fun initSYMCharacteristics(characteristics: List<BluetoothGattCharacteristic>) {
            for (char in characteristics) {
                when (char.uuid) {
                    integerUUID -> integerChar = char
                    buttonClickUUID -> buttonClickChar = char
                    temperatureUUID -> temperatureChar = char
                }
            }
        }

        /**
         * Convert calendar object to byte array for the value supported by the ble device
         * @param calendar Calendar to convert
         * @return Byte array who contain the date
         */
        private fun calendarToByteArray(calendar: Calendar): ByteArray {


            val year = calendar.get(Calendar.YEAR)

            // Months are between 0 and 12 in Calendar, but need 1-12 for the device.
            val month = calendar.get(Calendar.MONTH) % 12 + 1

            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val hours = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)

            // 1 = Sunday in Calendar class.... This things rotate to adjust to the right value.
            val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1

            return byteArrayOf(
                year.toByte(),
                (year shr 8).toByte(), // year >> 8 en kotlin, on décale les bits
                month.toByte(),
                day.toByte(),
                hours.toByte(),
                minutes.toByte(),
                seconds.toByte(),
                dayOfWeek.toByte(),
                0,
                0
            )
        }

        /**
         * Read the temperature on the ble device connected
         * @return true if everything went well
         */
        fun readTemperature(): Boolean {
            /*
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            if (temperatureChar != null) {
                readCharacteristic(temperatureChar).with { _: BluetoothDevice, data: Data ->
                    temperatureValue.postValue(
                        (data.getIntValue(Data.FORMAT_UINT16, 0)?.div(10)).toString()
                    )
                }.enqueue()
                return true
            } else {
                return false
            }
        }

        /**
         * Send an integer to the ble device connected
         * @param integer integer to send
         * @return true if everything went well
         */
        fun sendInteger(integer: Int): Boolean {
            return if (integerChar != null) {
                integerChar!!.setValue(integer, Data.FORMAT_UINT32, 0)
                writeCharacteristic(integerChar, integerChar!!.value, WRITE_TYPE_DEFAULT).enqueue()
                true
            } else {
                false
            }
        }

        /**
         * Set the current time in the ble device connected
         * @param calendar Date to set
         * @return true if everything went well
         */
        fun setDate(calendar: Calendar): Boolean {
            return if (currentTimeChar != null) {

                currentTimeChar!!.value = calendarToByteArray(calendar)

                writeCharacteristic(
                    currentTimeChar,
                    currentTimeChar!!.value,
                    WRITE_TYPE_DEFAULT
                ).enqueue()
                true
            } else {

                false
            }
        }

    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}