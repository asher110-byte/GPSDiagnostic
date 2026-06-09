# GPS Diagnostic for Xiaomi

Android app that diagnoses GPS issues on Xiaomi/MIUI devices.

## Tests performed (22 checks):
- GPS hardware existence
- GPS chip status
- Antenna signal
- Location services enabled
- Location mode
- Mock location
- Location permissions
- Battery saver mode
- Power restrictions
- MIUI background restrictions
- MIUI AutoStart
- MIUI battery optimization
- Network connectivity
- A-GPS availability
- WiFi scanning
- Compass sensor
- Accelerometer
- Gyroscope
- Satellite visibility (live tracking)
- Time & timezone
- Airplane mode

## Build
```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
