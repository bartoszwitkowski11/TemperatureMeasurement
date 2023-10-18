package com.example.temperaturemeasurement

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.temperaturemeasurement.TemperaturesDatabase.Temperatures
import com.example.temperaturemeasurement.TemperaturesDatabase.TemperaturesDatabase
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensorsDatabase
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.*

class AlternativeTemperatureMeasurementService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val daoTemperatures = TemperaturesDatabase.getDatabase(this, scope).temperaturesDao()
    private val daoSensors = UniqueSensorsDatabase.getDatabase(this, scope).sensorsDao()

    private val delayMillis: Long = 1000

    private var wakeLock: PowerManager.WakeLock? = null
    private var startWakelock = false
    private var startThread = false
    private var isMinimalised = false

    lateinit var sharedPref : SharedPreferences
    lateinit var prefEditor : SharedPreferences.Editor
    var startDisplaying: Boolean = true

    override fun onCreate() {
        sharedPref = this.getSharedPreferences("sharedPreferences", AppCompatActivity.MODE_PRIVATE)
        prefEditor = sharedPref.edit()
        var notification = createNotification()
        startForeground(1, notification)
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (sharedPref.getBoolean("startWakelock", startWakelock)) {
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Service::lock").apply {
                    acquire()
                    Log.e("Service", "WAKELOCK is working")
                }
            }
        }

        if (sharedPref.getBoolean("startThread", startThread)) {
            Thread( Runnable {
                Log.e("Service", "Starting thread")
                val currentThread = Thread.currentThread()
                Log.e("Service THREAD", currentThread.name + " " + currentThread.priority)
                currentThread.priority = Thread.MAX_PRIORITY - 2
                Log.e("Service AFTER PRIORITY CHANGE THREAD",
                    currentThread.name + " " + currentThread.priority)

                scope.launch {
                    Log.e("Service", "THREAD is working")
                    temperaturesReader()
                }

            },"AlternativeTempReader").start()
        }
        else {
            scope.launch {
                Log.e("Service", "is working")
                temperaturesReader()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopSelf()
        } catch (e: Exception) {
            Log.e("service ","Service stopped without being started: ${e.message}")
        }
        job.cancel()
    }

    private suspend fun temperaturesReader() {
        while (true) {
            Log.e("Service", "startDisplaying: ${sharedPref.getBoolean("startDisplaying", true)}")
            val process = Runtime.getRuntime().exec("dumpsys thermalservice")
            val thermalService = BufferedReader(InputStreamReader(process.inputStream))
            var startRead = false
            var toArray = thermalService.lines().toArray()
            thermalService.close()
            for (item in toArray.indices) {
                if (toArray[item] == "Current cooling devices from HAL:") {
                    startRead = false
                }

                if (startRead) {
                    var thermalString = toArray[item].toString()
                    var temp = thermalString.substring(thermalString.indexOf("mValue=") + 7, thermalString.indexOf(","))
                    var name = thermalString.substring(thermalString.indexOf("mName=") + 6, thermalString.lastIndexOf(","))

                    Log.e("temp", "$thermalString, startRead: $startRead, $temp, $name")

                    val timestamp = System.currentTimeMillis() / 1000

                    val insert = Temperatures("ThermalService", temp, "ThermalService", name, timestamp)
                    daoTemperatures.insert(insert)
                    Log.e("Service Activity", "Minimalised: ${sharedPref.getBoolean("isMinimalised", isMinimalised)}")
                    if (sharedPref.getBoolean("startDisplaying", startDisplaying) && !sharedPref.getBoolean("isMinimalised", isMinimalised)) {
                        daoSensors.update(name, temp)
                    }
                }
                if (toArray[item] == "Current temperatures from HAL:") {
                    startRead = true
                }
            }

            otherParameters()
            delay(sharedPref.getLong("delayMillis", delayMillis))
        }
    }

    private suspend fun otherParameters() {
        val batteryStats = getBatteryPercentage().toString()
        val insertBattery = Temperatures("Battery", batteryStats, "Battery", "Battery percentage", (System.currentTimeMillis() / 1000))

        daoTemperatures.insert(insertBattery)

        for (item in cpu_freq.indices) {
            val pathFreq = cpu_freq[item]
            val file = File(pathFreq)
            if (file.exists()) {
                val freq_value = ((universalFileReader(pathFreq)).toDouble() / 1000).toString()
                val insert = Temperatures(pathFreq, freq_value, pathFreq, "CPU $item", (System.currentTimeMillis() / 1000))
                daoTemperatures.insert(insert)
            }
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        val nativeHeapSize = memoryInfo.totalMem / 1000000
        val nativeHeapFreeSize = memoryInfo.availMem / 1000000
        val usedMemInBytes = nativeHeapSize - nativeHeapFreeSize
        val usedMemInPercentage = usedMemInBytes * 100 / nativeHeapSize

        val insert = Temperatures("totalMem: $nativeHeapSize", "nativeHeapFreeSize: $nativeHeapFreeSize", "usedMemInBytes: $usedMemInBytes", "usedMemInPercentage: $usedMemInPercentage", (System.currentTimeMillis() / 1000))
        daoTemperatures.insert(insert)

        val process = Runtime.getRuntime().exec("top -bn1 -m 5")
        val top = BufferedReader(InputStreamReader(process.inputStream))
        var topArray = top.lines().toArray()
        top.close()

        for (iter in topArray.indices) {
            var topString = topArray[iter].toString()
            val insert = Temperatures("Top", topString, "Top", "Line: $iter", (System.currentTimeMillis() / 1000))
            daoTemperatures.insert(insert)
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = this.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun universalFileReader(path: String): String {
        var value: String = "0.0"
        val file = File(path)
        try {
            val fileReader = FileReader(file)
            val bufferedReader = BufferedReader(fileReader)
            value = bufferedReader.readLine().toString()
            fileReader.close()
            bufferedReader.close()
        } catch (e5: java.lang.Exception) {
            e5.printStackTrace()
        }
        return value
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                notificationChannelId,
                "Temperature Measurement Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Temperature Measurement Service"
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Temperature Measurement Service")
            .setContentText("This app is recording temperature of your device in the background.")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }

    private val cpu_freq = Arrays.asList(
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu5/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu8/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu9/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu10/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu11/cpufreq/scaling_cur_freq"
    )
}