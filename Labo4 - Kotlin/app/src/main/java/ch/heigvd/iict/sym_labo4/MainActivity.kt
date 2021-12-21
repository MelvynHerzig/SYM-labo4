package ch.heigvd.iict.sym_labo4

import android.Manifest
import permissions.dispatcher.RuntimePermissions
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import permissions.dispatcher.NeedsPermission

/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2016 - HEIG-VD, IICT
 */
@RuntimePermissions
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //events
        findViewById<View>(R.id.nav_4).setOnClickListener {
            val i = Intent(this@MainActivity, CompassActivity::class.java)
            startActivity(i)
        }
        findViewById<View>(R.id.nav_5).setOnClickListener { startBleActivityWithPermissionCheck() }
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    fun startBleActivity() {
        val i = Intent(this@MainActivity, BleActivity::class.java)
        startActivity(i)
    }

}