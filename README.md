# TemperatureMeasurement for Android
TL;DR: App reads temperature values from sensors built-in the device.

**Functions description**

On the main screen:

Thread - turning on this option creates a thread for recording method. It may help with measuring in the background.

Displaying - displays data on the app main screen

Wakelock - suspends cpu from falling asleep while screen is off. It may help with measuring in the background.

Interval - changes intervals between temperatures readings. Format of the input should be in milliseconds (1000 ms = 15)

In settings that are hidden under three dots at the top of the screen:

Export db to .csv file - exports readings to csv. file. Typical location is Android/data/com.example.temperaturemeasurement/files.

Scan available sensors - It scans available sensors and methods.

Delete database - as name says, it deletes the entire database.

Battery Optimization - opens up battery optimization settings.

**How does the app work?**

There are implemented two different approaches to read temperatures from your device. Some devices have direct access to system files that stores temperatures. These devices are using something I call Direct Read Method. It reads temps from files and put them into database. As the device might have many sensors (or many places where data from sensors are stored, sometimes even the same parameters) that are not needed, there is implemented way to turn on and off reading from some sensors.

Other way to read temperatures is to use special command which is normally accessible on your device from Android Debug Bridge (ADB) in your computer. The purpose of the app is to be free from any other devices and additional cables etc. So I have developed another way of reading temps. I call it Alternative Read Method. It uses command dumpsys thermalservice which returns temperatures values from the most important sensors in the device. It returns them in string, so the app is searching in the string only for the data I am interested in, such as sensor name and temperature value. This way I am able to get temp readings from all devices that I am going to use for testing. It is possible that some devices won't work with both methods.

Unfortunately, there is a catch with the second method. It is needed to do steps below to achieve successful working.

1. Install ADB (https://developer.android.com/studio/releases /platform-tools) and then plug your device through USB cable.
More explanation here: https://www.xda-developers.com/install-adb
-windows-macos-linux/
2. Turn on debugging mode in Android device. If you don't know how to do it, look it up on the internet.
3. Open command line on Windows or Terminal on Linux/macOS.
4. Copy location to your adb.exe, for example:
C\Users\bartosz witkowski\desktop\adb\platform-tools\adb.exe
5. Paste it in command line and add devices, and then press enter.
6. If you get respond with numbers and letters, it means your phone is connected to the computer correctly
7. Repeat step 4 and also paste this:
shell pm grant com.example.temperaturemeasurement
android.permission.DUMP, and then press enter.
8. The second method should now work properly. For precaution, you should scan again sensors in the app.

**Is it safe?**
Yes!

**Is there any risk to damage my phone?**
No! There is 0 chance to damage your device. The above steps are not modifying software on your device in any way! Moreover, commands and software that are used there are intended for collecting data about cpu workload, temperatures, and also checking stability of new devices and apps.

**What is the line dumpsys thermalservice doing?**
You can check by yourself! Just input into your command line path to adb.exe and shell dumpsys thermalservice. Briefly, this command returns info about all parameter that are connected with thermal control of the device, for example temperatures of crucial sensors.

**But what is the line from point 7?**
It is needed to grant application proper permission that are not accessible in the System for third-party apps in Android. It means that system apps can use this permission, but apps that are not installed by the vendor can't. Fortunately, it is possible to grant that permission one-time only through the above method. The app is using it ONLY for accessing above command dumpsys thermalservice and nothing more.

Created by Bartosz Witkowski, Poznan University of Technology 2023
