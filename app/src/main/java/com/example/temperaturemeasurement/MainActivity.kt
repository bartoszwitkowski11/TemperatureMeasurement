package com.example.temperaturemeasurement

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.temperaturemeasurement.TemperaturesDatabase.Temperatures
import com.example.temperaturemeasurement.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val temperaturesViewModel: TemperaturesViewModel by viewModels {
        TemperaturesViewModelFactory((application as TemperaturesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        verifyStoragePermissions(this)
        verifyIfBatteryOptimizationIsOff()
    }

    fun settingsToDb(function: String) {
        val timestamp = System.currentTimeMillis() / 1000
        val insert = Temperatures(" ", "App state: $function", " ", "App_state", timestamp)
        temperaturesViewModel.insert(insert)
    }

    override fun onStop() {
        super.onStop()
        settingsToDb("Activity Stopped")
    }

    private fun verifyIfBatteryOptimizationIsOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val dialogClickListener =
                    DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                } else {
                                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    intent.data = Uri.parse("package:${packageName}")
                                }
                                startActivity(intent)
                                Toast.makeText(this, "Switching to Battery Settings", Toast.LENGTH_SHORT).show()
                            }
                            DialogInterface.BUTTON_NEGATIVE -> {
                                Toast.makeText(this, "Cancel!?", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                val builder = AlertDialog.Builder(this)
                builder
                    .setTitle("Battery Optimization")
                    .setMessage("You have to turn off battery optimization for this app to ensure that app works properly in the background. It may impact battery performance! You can change it later through settings.")
                    .setPositiveButton("To Battery settings", dialogClickListener)
                    .setNegativeButton("Cancel", dialogClickListener)
                    .show()
            }
        }
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun verifyStoragePermissions(activity: Activity?) {
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}