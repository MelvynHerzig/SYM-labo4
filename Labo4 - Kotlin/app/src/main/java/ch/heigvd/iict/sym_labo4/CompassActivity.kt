package ch.heigvd.iict.sym_labo4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer
import android.hardware.SensorManager

/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * Edited by Berney Alec, Forestier Quentin, Herzig Mevyn on 21.12.2021
 * (C) 2016 - HEIG-VD, IICT
 */
class CompassActivity : AppCompatActivity() {

    //opengl
    private lateinit var opglr: OpenGLRenderer
    private lateinit var m3DView: GLSurfaceView

    // Sensors
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensorAccelerometer: Sensor
    private lateinit var mSensorMagnetic: Sensor

    /**
     * On create, setup view and get sensors
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // we initiate the view
        setContentView(R.layout.activity_compass)

        //we create the renderer
        opglr = OpenGLRenderer(applicationContext)

        // link to GUI
        m3DView = findViewById(R.id.compass_opengl)

        //init opengl surface view
        m3DView.setRenderer(opglr)

        // Get an instance of the SensorManager and sensor
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    /**
     * On resume, register listener to sensors.
     */
    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(mSensorListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI)
        mSensorManager.registerListener(mSensorListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * On pause, remove sensor listener
     */
    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(mSensorListener)
    }

    /**
     * Sensor listener to handle sensors event
     */
    val mSensorListener : SensorEventListener = (object : SensorEventListener {

        // Sensors data
        private var mAccelerometerData = FloatArray(3)
        private var mMagneticData      = FloatArray(3)
        private var mRotationMatrix    = FloatArray(16)

        /**
         * React to sensors
         */
        override fun onSensorChanged(event: SensorEvent?) {
            // Set sensors value
            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER  -> mAccelerometerData = event.values
                Sensor.TYPE_MAGNETIC_FIELD -> mMagneticData      = event.values
            }

            // Get rotation matrix
            SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerData, mMagneticData)

            // set new rotation
            mRotationMatrix = opglr.swapRotMatrix(mRotationMatrix)
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            // Not needed
        }

    })

}