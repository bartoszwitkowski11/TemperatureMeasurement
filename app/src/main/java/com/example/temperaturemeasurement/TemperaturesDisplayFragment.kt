package com.example.temperaturemeasurement

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.temperaturemeasurement.TemperaturesDatabase.Temperatures
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensors
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensorsViewModel
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensorsViewModelFactory
import com.example.temperaturemeasurement.databinding.TemperaturesReadListBinding
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class TemperaturesDisplayFragment : Fragment() {
    lateinit var binding: TemperaturesReadListBinding
    var df = DecimalFormat("####0.00")

    private val delayMillis : Long = 1000
    private var startDisplaying = true
    private var isMinimalised = false
    private var startThread = false
    private var areSensorsAvailable = false
    private var isServiceStarted = false
    private var startWakelock = false
    private var counter = 0

    lateinit var temperaturesList : MutableList<Temperatures>
    lateinit var sensorsList : List<UniqueSensors>
    lateinit var sharedPref : SharedPreferences
    lateinit var prefEditor : SharedPreferences.Editor

    private val uniqueSensorsViewModel: UniqueSensorsViewModel by viewModels {
        UniqueSensorsViewModelFactory((requireActivity().application as TemperaturesApplication).repositoryS)
    }

    private val temperaturesViewModel: TemperaturesViewModel by viewModels {
        TemperaturesViewModelFactory((requireActivity().application as TemperaturesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("sharedPreferences", AppCompatActivity.MODE_PRIVATE)
        prefEditor = sharedPref.edit()

        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.scan) {
            sensorsReader()
        }

        if (id == R.id.export) {
            if (sharedPref.getBoolean("isServiceStarted", isServiceStarted)) {
                stopMeasurement()
            }

            Thread(Runnable {
                Log.e("EXPORTING TO CSV THREAD", "start")
                temperaturesList = temperaturesViewModel.getTemperaturesList().toMutableList()
                exportDatabaseToCSVFile()
            }, "ExportCSV").start()

            Toast.makeText(requireContext(), "Exporting to .csv file has been started. Location of file: ${requireContext().getExternalFilesDir(null).toString()}.", Toast.LENGTH_LONG).show()
        }

        if (id == R.id.delete) {
            val dialogClickListener =
                DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            if (sharedPref.getBoolean("isServiceStarted", isServiceStarted)) {
                                stopMeasurement()
                            }
                            temperaturesViewModel.deleteAll()
                            temperaturesList.clear()
                            Toast.makeText(requireContext(), "Database is erased", Toast.LENGTH_LONG).show()
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {}
                    }
                }

            val builder = AlertDialog.Builder(context)
            builder.setMessage("Do you want to delete all records from database?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show()
        }

        if (id == R.id.battery) {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            startActivity(intent)
            Toast.makeText(requireContext(), "Opening Battery Settings", Toast.LENGTH_SHORT).show()
        }

        if (id == R.id.about) {
            findNavController().navigate(R.id.action_temperaturesDisplayFragment_to_aboutFragment)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
       inflater.inflate(R.menu.quick_settings, menu)

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPause() {
        super.onPause()
        isMinimalised = true
        prefEditor.apply {
            putBoolean("isMinimalised", isMinimalised)
            apply()
        }
    }

    override fun onStart() {
        super.onStart()

        if (sharedPref.getBoolean("firstLaunch", true)) {
            prefEditor.apply {
                putBoolean("firstLaunch", false)
                putInt("counter", 0)
                apply()
            }

            Log.e("Only first time", "AAA")
            sensorsReader()
        }
    }

    override fun onResume() {
        super.onResume()
        isMinimalised = false
        prefEditor.apply {
            putBoolean("isMinimalised", isMinimalised)
            apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = TemperaturesReadListBinding.inflate(layoutInflater)
        val view = binding.root

        return view
    }

    private fun sensorsReader() {
        for (item in values.indices) {
            val pathTemp = values[item]
            val pathType = names[item]
            val file = File(pathTemp)
            if (file.exists()) {
                val temp = ((universalFileReader(pathTemp)).toDouble() / 1000).toString()
                val type = universalFileReader(pathType)
                if (type != "0.0") {
                    val insert = UniqueSensors(pathTemp, temp, pathType, true, type)
                    uniqueSensorsViewModel.insert(insert)
                    Log.e("sensorsReader IF", "File " + pathTemp + " exist!!!")
                    counter = -1;
                    areSensorsAvailable = true
                    prefEditor.apply{
                        putBoolean("areSensorsAvailable", areSensorsAvailable)
                    }
                }
            }
            else {
                Log.e("sensorsReader ELSE", "File $pathTemp does not exist, ${sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)} ")
            }
        }
        if(!sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)) {
            counter = 0;
            Log.e("sensorsReader AFTER",
                "areSensorsAvailable: ${
                    sharedPref.getBoolean("areSensorsAvailable",
                        areSensorsAvailable)
                } ")
            val process = Runtime.getRuntime().exec("dumpsys thermalservice")
            val thermalService = BufferedReader(InputStreamReader(process.inputStream))
            var startRead = false
            val toArray = thermalService.lines().toArray()
            thermalService.close()
            for (item in toArray.indices) {
                if (toArray[item] == "Current cooling devices from HAL:") {
                    startRead = false
                }

                if (startRead) {
                    val thermalString = toArray[item].toString()
                    val temp = thermalString.substring(thermalString.indexOf("mValue=") + 7,
                        thermalString.indexOf(","))
                    val name = thermalString.substring(thermalString.indexOf("mName=") + 6,
                        thermalString.lastIndexOf(","))

                    Log.e("temp", "$thermalString, startRead: $startRead, $temp, $name")

                    val insert = UniqueSensors("ThermalService",
                        (df.format(temp.toDouble())).toString(),
                        "ThermalService",
                        true,
                        name)
                    prefEditor.apply {
                        putInt("counter", counter++)
                    }
                    uniqueSensorsViewModel.insert(insert)
                }

                if (toArray[item] == "Current temperatures from HAL:") {
                    startRead = true
                }
            }
        }
        showHowManySensors()
    }

    private fun showHowManySensors() {
        if(!sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)) {
        Toast.makeText(requireContext(),
            "The app uses alternative reading method.",
            Toast.LENGTH_LONG).show()
            Log.e("sensorsReader! alt", "sensors: $counter")
            additionalPermission()
        }
        else {
            Toast.makeText(requireContext(), "The app uses direct reading method.", Toast.LENGTH_LONG).show()
            Log.e("sensorsReader!", "areSensorsAvailable: ${sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)} ")
        }
    }

    private fun additionalPermission() {
        if(!sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)) {
            if (sharedPref.getInt("counter", counter) == 0) {
                val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            sensorsReader()
                        }
                    }
                }
                val builder = AlertDialog.Builder(context)
                builder
                    .setTitle("The measurement method needs additional permission!")
                    .setMessage("The app without DUMP permission won't work at all. How to grant DUMP permission is explained in About section")
                    .setPositiveButton("Ok", dialogClickListener)
                    .setNegativeButton("Scan sensors", dialogClickListener)
                    .show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.TempList)
            val adapter = TemperaturesListAdapter(uniqueSensorsViewModel)
            adapter.setHasStableIds(true)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

            temperaturesViewModel.allTemperatures.observe(viewLifecycleOwner) { temperatures ->
                //temperatures.let { adapter.submitList(it) }
                temperaturesList = temperatures.toMutableList()
            }

            uniqueSensorsViewModel.allSensors.observe(viewLifecycleOwner) { sensors ->
                sensors.let { adapter.submitList(it) }
                sensorsList = sensors
            }

        val fab = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)

        fab.setOnClickListener() {
            if (!sharedPref.getBoolean("isServiceStarted", isServiceStarted)) {
                startMeasurement()
            }
            else {
                stopMeasurement()
            }
        }

        val intervalButton = view.findViewById<MaterialCardView>(R.id.IntervalCard)
        val wakelockButton = view.findViewById<MaterialCardView>(R.id.WakelockCard)
        val displayButton = view.findViewById<MaterialCardView>(R.id.DisplayCard)
        val threadButton = view.findViewById<MaterialCardView>(R.id.ThreadCard)

        val intervalValue = view.findViewById<TextView>(R.id.intervalValue)
        val wakelockValue = view.findViewById<TextView>(R.id.wakelockValue)
        val displayValue = view.findViewById<TextView>(R.id.displayValue)
        val threadValue = view.findViewById<TextView>(R.id.threadValue)

        if (sharedPref.getBoolean("startWakelock", startWakelock)) {
            wakelockValue.text = "On"
        }
        else {
            wakelockValue.text = "Off"
        }

        if (sharedPref.getBoolean("startDisplaying", startDisplaying)) {
            displayValue.text = "On"
        }
        else {
            displayValue.text = "Off"
        }

        if (sharedPref.getBoolean("startThread", startThread)) {
            threadValue.text = "On"
        }
        else {
            threadValue.text = "Off"
        }

        intervalValue.text = sharedPref.getLong("delayMillis", delayMillis).toString() + " ms"

        intervalButton.setOnClickListener(View.OnClickListener {
            val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.dialog_layout, getView() as ViewGroup?, false)
            val dialogClickListener =
                DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val value = viewInflated.findViewById<EditText>(R.id.intervalInput).text.toString().toLong()
                            prefEditor.apply {
                                putLong("delayMillis", value)
                                apply()
                            }
                            intervalValue.text = sharedPref.getLong("delayMillis", delayMillis).toString() + " ms"
                            Toast.makeText(requireContext(), "Interval has been changed to ${sharedPref.getLong("delayMillis", delayMillis)} ms", Toast.LENGTH_SHORT).show()
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            Toast.makeText(requireContext(), "Interval is still set to ${sharedPref.getLong("delayMillis", delayMillis)} ms", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            val builder = AlertDialog.Builder(context)
            builder
                .setView(viewInflated)
                .setTitle("Change time interval between temperature reading")
                .setMessage("Please input value in milliseconds (1 second is 1000 milliseconds)")
                .setPositiveButton("Confirm", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener).show()
        })

        wakelockButton.setOnClickListener(View.OnClickListener {
            if (!sharedPref.getBoolean("startWakelock", startWakelock)) {
                startWakelock = true
                wakelockValue.text = "On"
                prefEditor.apply {
                    putBoolean("startWakelock", startWakelock)
                    apply()
                }
                Toast.makeText(requireContext(), "Wakelock is turned ON", Toast.LENGTH_SHORT).show()
            }
            else {
                startWakelock = false
                wakelockValue.text = "Off"
                prefEditor.apply {
                    putBoolean("startWakelock", startWakelock)
                    apply()
                }
                Toast.makeText(requireContext(), "Wakelock is turned OFF", Toast.LENGTH_SHORT).show()
            }
        })

        displayButton.setOnClickListener(View.OnClickListener {
            if (!sharedPref.getBoolean("startDisplaying", startDisplaying)) {
                startDisplaying = true
                prefEditor.apply {
                    putBoolean("startDisplaying", startDisplaying)
                    apply()
                }
                displayValue.text = "On"
                Log.e("startDisplay", "startDisplaying: ${sharedPref.getBoolean("startDisplaying", startDisplaying)}")
                Toast.makeText(requireContext(), "The display of current temperatures is turned ON", Toast.LENGTH_SHORT).show()
            }
            else {
                startDisplaying = false
                prefEditor.apply {
                    putBoolean("startDisplaying", startDisplaying)
                    apply()
                }
                displayValue.text = "Off"
                Log.e("stopDisplay", "startDisplaying: ${sharedPref.getBoolean("startDisplaying", startDisplaying)}")
                Toast.makeText(requireContext(), "The display of current temperatures is turned OFF", Toast.LENGTH_SHORT).show()
            }
        })

        threadButton.setOnClickListener(View.OnClickListener {
            if (!sharedPref.getBoolean("startThread", startThread)) {
                startThread = true
                threadValue.text = "On"
                prefEditor.apply {
                    putBoolean("startThread", startThread)
                    apply()
                }
                Toast.makeText(requireContext(), "Thread is turned ON", Toast.LENGTH_SHORT).show()
            }
            else {
                threadValue.text = "Off"
                startThread = false
                prefEditor.apply {
                    putBoolean("startThread", startThread)
                    apply()
                }
                Toast.makeText(requireContext(), "Thread is turned OFF", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun settingsToDb() {
        val timestamp = System.currentTimeMillis() / 1000
        val insert = Temperatures("Wakelock: ${sharedPref.getBoolean("startWakelock", startWakelock)}", "Service: ${sharedPref.getBoolean("isServiceStarted", isServiceStarted)}", "Thread: ${sharedPref.getBoolean("startThread", startThread)}", "Flags", timestamp)
        temperaturesViewModel.insert(insert)
    }

    private fun startMeasurement() {
        if (sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)) {
            isServiceStarted = true
            prefEditor.apply {
                putBoolean("isServiceStarted", isServiceStarted)
                apply()
            }
            settingsToDb()
            val intent = Intent(requireContext(), DirectTemperatureMeasurementService::class.java)
            requireContext().startService(intent)
            Log.e("start DirectTemperatureMeasurementService", "   ")
        }
        else {
            isServiceStarted = true
            prefEditor.apply {
                putBoolean("isServiceStarted", isServiceStarted)
                apply()
            }
            settingsToDb()
            val intent = Intent(requireContext(), AlternativeTemperatureMeasurementService::class.java)
            requireContext().startService(intent)
            Log.e("start AlternativeTemperatureMeasurementService", "   ")
        }
        Toast.makeText(requireContext(),
            "Start temperature recording!",
            Toast.LENGTH_LONG).show()
    }
    private fun stopMeasurement() {
        if (sharedPref.getBoolean("areSensorsAvailable", areSensorsAvailable)) {
            isServiceStarted = false
            prefEditor.apply {
                putBoolean("isServiceStarted", isServiceStarted)
                apply()
            }
            settingsToDb()
            val intent = Intent(requireContext(), DirectTemperatureMeasurementService::class.java)
            requireContext().stopService(intent)
        }
        else {
            isServiceStarted = false
            prefEditor.apply {
                putBoolean("isServiceStarted", isServiceStarted)
                apply()
            }
            settingsToDb()
            val intent = Intent(requireContext(), AlternativeTemperatureMeasurementService::class.java)
            requireContext().stopService(intent)
        }
        Toast.makeText(requireContext(),
            "Temperature recording is stopped!",
            Toast.LENGTH_LONG).show()
        Log.e("stopSerivce", "   ")
    }

    private fun exportDatabaseToCSVFile() {
        val folderName = android.os.Build.BRAND + "_" + android.os.Build.MODEL.toString() + "_" + (System.currentTimeMillis() / 1000).toString()
        for (item in temperaturesViewModel.getName.indices) {
            val filename: String = temperaturesViewModel.getName[item] + ".csv"
            val file = File(requireContext().getExternalFilesDir(folderName), filename)
            if (!file.exists()) {
                Log.e("Generating CSV file", "${temperaturesViewModel.getName[item]}, $item")
                val csvFile = generateFile(requireContext(), filename, folderName)
                if (csvFile != null) {
                    exportToCSVFile(file, temperaturesViewModel.getName[item])
                }
            }
        }
        Log.e("End export for all", "")
    }

    private fun generateFile(context: Context, fileName: String, folderName: String): File? {
        val csvFile = File(context.getExternalFilesDir(folderName), fileName)
        csvFile.createNewFile()

        return if (csvFile.exists()) {
            csvFile
        } else {
            null
        }
    }

    private fun exportToCSVFile(csvFile: File, sensor: String) {
        csvWriter().open(csvFile, append = false) {
            // Header
            writeRow(listOf("path_temp", "value_temp", "path_name", "value_name", "time_stamp", "id"))
            temperaturesList.forEachIndexed { index, temperatures ->
                if (temperatures.valueName == sensor) {
                    writeRow(listOf(temperatures.pathTemp,
                        temperatures.valueTemp,
                        temperatures.pathName,
                        temperatures.valueName,
                        temperatures.timeStamp,
                        temperatures.id))
                        //Log.e("Export to file", "Sensor $sensor, ID: ${temperatures.id}")
                }
            }
        }
        Log.e("Export to file", "Ended for $sensor")
    }
/*
    private fun exportDatabaseToCSVFile() {
        val filename : String = android.os.Build.BRAND + "_" + android.os.Build.MODEL.toString() + "_" + Calendar.getInstance().time.toString() + ".csv"
        val csvFile = generateFile(requireContext(), filename)
        if (csvFile != null) {
            exportToCSVFile(csvFile)
            //Toast.makeText(requireContext(), "File has been saved to: " + requireContext().getExternalFilesDir(null).toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun generateFile(context: Context, fileName: String): File? {
        val csvFile = File(context.getExternalFilesDir(null), fileName)
        csvFile.createNewFile()

        return if (csvFile.exists()) {
            csvFile
        } else {
            null
        }
    }

    private fun exportToCSVFile(csvFile: File) {
        csvWriter().open(csvFile, append = false) {
            // Header
            writeRow(listOf("path_temp", "value_temp", "path_name", "value_name", "time_stamp", "id"))
            temperaturesList.forEachIndexed { index, temperatures ->
                writeRow(listOf(temperatures.pathTemp, temperatures.valueTemp, temperatures.pathName, temperatures.valueName, temperatures.timeStamp, temperatures.id))
                Log.e("EXPORTING TO CSV THREAD", "ID: ${temperatures.id}")
            }
        }
        Log.e("EXPORTING TO CSV THREAD", "stop")
    }
    */

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

    private val values = Arrays.asList(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/class/thermal/thermal_zone3/temp",
        "/sys/class/thermal/thermal_zone4/temp",
        "/sys/class/thermal/thermal_zone5/temp",
        "/sys/class/thermal/thermal_zone6/temp",
        "/sys/class/thermal/thermal_zone7/temp",
        "/sys/class/thermal/thermal_zone8/temp",
        "/sys/class/thermal/thermal_zone9/temp",
        "/sys/class/thermal/thermal_zone10/temp",
        "/sys/class/thermal/thermal_zone11/temp",
        "/sys/class/thermal/thermal_zone12/temp",
        "/sys/class/thermal/thermal_zone13/temp",
        "/sys/class/thermal/thermal_zone14/temp",
        "/sys/class/thermal/thermal_zone15/temp",
        "/sys/class/thermal/thermal_zone16/temp",
        "/sys/class/thermal/thermal_zone17/temp",
        "/sys/class/thermal/thermal_zone18/temp",
        "/sys/class/thermal/thermal_zone19/temp",
        "/sys/class/thermal/thermal_zone20/temp",
        "/sys/class/thermal/thermal_zone21/temp",
        "/sys/class/thermal/thermal_zone22/temp",
        "/sys/class/thermal/thermal_zone23/temp",
        "/sys/class/thermal/thermal_zone24/temp",
        "/sys/class/thermal/thermal_zone25/temp",
        "/sys/class/thermal/thermal_zone26/temp",
        "/sys/class/thermal/thermal_zone27/temp",
        "/sys/class/thermal/thermal_zone28/temp",
        "/sys/class/thermal/thermal_zone29/temp",
        "/sys/class/thermal/thermal_zone30/temp",
        "/sys/class/thermal/thermal_zone31/temp",
        "/sys/class/thermal/thermal_zone32/temp",
        "/sys/class/thermal/thermal_zone33/temp",
        "/sys/class/thermal/thermal_zone34/temp",
        "/sys/class/thermal/thermal_zone35/temp",
        "/sys/class/thermal/thermal_zone36/temp",
        "/sys/class/thermal/thermal_zone37/temp",
        "/sys/class/thermal/thermal_zone38/temp",
        "/sys/class/thermal/thermal_zone39/temp",
        "/sys/class/thermal/thermal_zone40/temp",
        "/sys/class/thermal/thermal_zone41/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp",
        "/sys/devices/virtual/thermal/thermal_zone1/temp",
        "/sys/devices/virtual/thermal/thermal_zone2/temp",
        "/sys/devices/virtual/thermal/thermal_zone3/temp",
        "/sys/devices/virtual/thermal/thermal_zone4/temp",
        "/sys/devices/virtual/thermal/thermal_zone5/temp",
        "/sys/devices/virtual/thermal/thermal_zone6/temp",
        "/sys/devices/virtual/thermal/thermal_zone7/temp",
        "/sys/devices/virtual/thermal/thermal_zone8/temp",
        "/sys/devices/virtual/thermal/thermal_zone9/temp",
        "/sys/devices/virtual/thermal/thermal_zone10/temp",
        "/sys/devices/virtual/thermal/thermal_zone11/temp",
        "/sys/devices/virtual/thermal/thermal_zone12/temp",
        "/sys/devices/virtual/thermal/thermal_zone13/temp",
        "/sys/devices/virtual/thermal/thermal_zone14/temp",
        "/sys/devices/virtual/thermal/thermal_zone15/temp",
        "/sys/devices/virtual/thermal/thermal_zone16/temp",
        "/sys/devices/virtual/thermal/thermal_zone17/temp",
        "/sys/devices/virtual/thermal/thermal_zone18/temp",
        "/sys/devices/virtual/thermal/thermal_zone19/temp",
        "/sys/devices/virtual/thermal/thermal_zone20/temp",
        "/sys/devices/virtual/thermal/thermal_zone21/temp",
        "/sys/devices/virtual/thermal/thermal_zone22/temp",
        "/sys/devices/virtual/thermal/thermal_zone23/temp",
        "/sys/devices/virtual/thermal/thermal_zone24/temp",
        "/sys/devices/virtual/thermal/thermal_zone25/temp",
        "/sys/devices/virtual/thermal/thermal_zone26/temp",
        "/sys/devices/virtual/thermal/thermal_zone27/temp",
        "/sys/devices/virtual/thermal/thermal_zone28/temp",
        "/sys/devices/virtual/thermal/thermal_zone29/temp",
        "/sys/devices/virtual/thermal/thermal_zone30/temp",
        "/sys/devices/virtual/thermal/thermal_zone31/temp",
        "/sys/devices/virtual/thermal/thermal_zone32/temp",
        "/sys/devices/virtual/thermal/thermal_zone33/temp",
        "/sys/devices/virtual/thermal/thermal_zone34/temp",
        "/sys/devices/virtual/thermal/thermal_zone35/temp",
        "/sys/devices/virtual/thermal/thermal_zone36/temp",
        "/sys/devices/virtual/thermal/thermal_zone37/temp",
        "/sys/devices/virtual/thermal/thermal_zone38/temp",
        "/sys/devices/virtual/thermal/thermal_zone39/temp",
        "/sys/devices/virtual/thermal/thermal_zone40/temp",
        "/sys/devices/virtual/thermal/thermal_zone41/temp",
        "sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
        "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
        "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
        "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
        "/sys/devices/platform/tegra_tmon/temp1_input",
        "/sys/devices/platform/s5p-tmu/temperature",
        "/sys/devices/platform/s5p-tmu/curr_temp",
        "/sys/class/hwmon/hwmon0/temp1_input",
        "/sys/class/hwmon/hwmon1/temp1_input",
        "/sys/class/hwmon/hwmon0/device/temp",
        "/sys/class/hwmon/hwmon1/device/temp",
        "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
        "/sys/kernel/debug/tegra_thermal/temp_tj",
        "/sys/htc/cpu_temp",
        "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/ext_temperature",
        "/sys/devices/platform/tegra-tsensor/tsensor_temperature",
        "sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
    )

    private val names = Arrays.asList(
        "/sys/class/thermal/thermal_zone0/type",
        "/sys/class/thermal/thermal_zone1/type",
        "/sys/class/thermal/thermal_zone2/type",
        "/sys/class/thermal/thermal_zone3/type",
        "/sys/class/thermal/thermal_zone4/type",
        "/sys/class/thermal/thermal_zone5/type",
        "/sys/class/thermal/thermal_zone6/type",
        "/sys/class/thermal/thermal_zone7/type",
        "/sys/class/thermal/thermal_zone8/type",
        "/sys/class/thermal/thermal_zone9/type",
        "/sys/class/thermal/thermal_zone10/type",
        "/sys/class/thermal/thermal_zone11/type",
        "/sys/class/thermal/thermal_zone12/type",
        "/sys/class/thermal/thermal_zone13/type",
        "/sys/class/thermal/thermal_zone14/type",
        "/sys/class/thermal/thermal_zone15/type",
        "/sys/class/thermal/thermal_zone16/type",
        "/sys/class/thermal/thermal_zone17/type",
        "/sys/class/thermal/thermal_zone18/type",
        "/sys/class/thermal/thermal_zone19/type",
        "/sys/class/thermal/thermal_zone20/type",
        "/sys/class/thermal/thermal_zone21/type",
        "/sys/class/thermal/thermal_zone22/type",
        "/sys/class/thermal/thermal_zone23/type",
        "/sys/class/thermal/thermal_zone24/type",
        "/sys/class/thermal/thermal_zone25/type",
        "/sys/class/thermal/thermal_zone26/type",
        "/sys/class/thermal/thermal_zone27/type",
        "/sys/class/thermal/thermal_zone28/type",
        "/sys/class/thermal/thermal_zone29/type",
        "/sys/class/thermal/thermal_zone30/type",
        "/sys/class/thermal/thermal_zone31/type",
        "/sys/class/thermal/thermal_zone32/type",
        "/sys/class/thermal/thermal_zone33/type",
        "/sys/class/thermal/thermal_zone34/type",
        "/sys/class/thermal/thermal_zone35/type",
        "/sys/class/thermal/thermal_zone36/type",
        "/sys/class/thermal/thermal_zone37/type",
        "/sys/class/thermal/thermal_zone38/type",
        "/sys/class/thermal/thermal_zone39/type",
        "/sys/class/thermal/thermal_zone40/type",
        "/sys/class/thermal/thermal_zone41/type",
        "/sys/devices/virtual/thermal/thermal_zone0/type",
        "/sys/devices/virtual/thermal/thermal_zone1/type",
        "/sys/devices/virtual/thermal/thermal_zone2/type",
        "/sys/devices/virtual/thermal/thermal_zone3/type",
        "/sys/devices/virtual/thermal/thermal_zone4/type",
        "/sys/devices/virtual/thermal/thermal_zone5/type",
        "/sys/devices/virtual/thermal/thermal_zone6/type",
        "/sys/devices/virtual/thermal/thermal_zone7/type",
        "/sys/devices/virtual/thermal/thermal_zone8/type",
        "/sys/devices/virtual/thermal/thermal_zone9/type",
        "/sys/devices/virtual/thermal/thermal_zone10/type",
        "/sys/devices/virtual/thermal/thermal_zone11/type",
        "/sys/devices/virtual/thermal/thermal_zone12/type",
        "/sys/devices/virtual/thermal/thermal_zone13/type",
        "/sys/devices/virtual/thermal/thermal_zone14/type",
        "/sys/devices/virtual/thermal/thermal_zone15/type",
        "/sys/devices/virtual/thermal/thermal_zone16/type",
        "/sys/devices/virtual/thermal/thermal_zone17/type",
        "/sys/devices/virtual/thermal/thermal_zone18/type",
        "/sys/devices/virtual/thermal/thermal_zone19/type",
        "/sys/devices/virtual/thermal/thermal_zone20/type",
        "/sys/devices/virtual/thermal/thermal_zone21/type",
        "/sys/devices/virtual/thermal/thermal_zone22/type",
        "/sys/devices/virtual/thermal/thermal_zone23/type",
        "/sys/devices/virtual/thermal/thermal_zone24/type",
        "/sys/devices/virtual/thermal/thermal_zone25/type",
        "/sys/devices/virtual/thermal/thermal_zone26/type",
        "/sys/devices/virtual/thermal/thermal_zone27/type",
        "/sys/devices/virtual/thermal/thermal_zone28/type",
        "/sys/devices/virtual/thermal/thermal_zone29/type",
        "/sys/devices/virtual/thermal/thermal_zone30/type",
        "/sys/devices/virtual/thermal/thermal_zone31/type",
        "/sys/devices/virtual/thermal/thermal_zone32/type",
        "/sys/devices/virtual/thermal/thermal_zone33/type",
        "/sys/devices/virtual/thermal/thermal_zone34/type",
        "/sys/devices/virtual/thermal/thermal_zone35/type",
        "/sys/devices/virtual/thermal/thermal_zone36/type",
        "/sys/devices/virtual/thermal/thermal_zone37/type",
        "/sys/devices/virtual/thermal/thermal_zone38/type",
        "/sys/devices/virtual/thermal/thermal_zone39/type",
        "/sys/devices/virtual/thermal/thermal_zone40/type",
        "/sys/devices/virtual/thermal/thermal_zone41/type",
        "sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
        "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
        "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
        "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
        "/sys/devices/platform/tegra_tmon/temp1_input",
        "/sys/devices/platform/s5p-tmu/temperature",
        "/sys/devices/platform/s5p-tmu/curr_temp",
        "/sys/class/hwmon/hwmon0/name",
        "/sys/class/hwmon/hwmon1/name",
        "/sys/class/hwmon/hwmon0/device/type",
        "/sys/class/hwmon/hwmon1/device/type",
        "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
        "/sys/kernel/debug/tegra_thermal/temp_tj",
        "/sys/htc/cpu_temp",
        "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/ext_temperature",
        "/sys/devices/platform/tegra-tsensor/tsensor_temperature",
        "sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
    )
}