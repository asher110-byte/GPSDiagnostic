package com.xiaomi.gpsdiag;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class GpsDiagnosticEngine {

    private final Context context;
    private final List<GpsDiagnosticResult> results = new ArrayList<>();

    public GpsDiagnosticEngine(Context context) {
        this.context = context;
    }

    public List<GpsDiagnosticResult> runAllTests() {
        results.clear();
        checkGpsHardwareExists();
        checkGpsChipStatus();
        checkAntennaSignal();
        checkLocationServicesEnabled();
        checkLocationMode();
        checkMockLocationDisabled();
        checkLocationPermissions();
        checkBatterySaverMode();
        checkPowerSavingRestrictions();
        checkMiuiBackgroundRestrictions();
        checkMiuiAutoStart();
        checkMiuiBatteryOptimization();
        checkNetworkConnectivity();
        checkAGPS();
        checkWifiScanning();
        checkCompass();
        checkAccelerometer();
        checkGyroscope();
        checkSatelliteVisibility();
        checkTimeAndTimezone();
        checkAirplaneMode();
        return results;
    }

    private void checkGpsHardwareExists() {
        PackageManager pm = context.getPackageManager();
        boolean hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        if (hasGps) {
            results.add(new GpsDiagnosticResult("GPS Hardware", GpsDiagnosticResult.Status.PASS, "GPS chip exists in device", ""));
        } else {
            results.add(new GpsDiagnosticResult("GPS Hardware", GpsDiagnosticResult.Status.FAIL, "No GPS hardware found!", "GPS chip may be physically damaged. Visit a repair shop."));
        }
    }

    private void checkGpsChipStatus() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            results.add(new GpsDiagnosticResult("Location Service", GpsDiagnosticResult.Status.FAIL, "Location service unavailable", "Critical OS issue - try factory reset"));
            return;
        }
        LocationProvider gpsProvider = lm.getProvider(LocationManager.GPS_PROVIDER);
        if (gpsProvider != null) {
            results.add(new GpsDiagnosticResult("GPS Chip", GpsDiagnosticResult.Status.PASS, "GPS provider active | Hardware: " + Build.HARDWARE + " | Board: " + Build.BOARD, ""));
        } else {
            results.add(new GpsDiagnosticResult("GPS Chip", GpsDiagnosticResult.Status.FAIL, "GPS provider not responding - chip may be damaged", "May need GPS chip replacement. Try: restart / ROM update first"));
        }
    }

    private void checkAntennaSignal() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try { gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception e) {}
        if (!gpsEnabled) {
            results.add(new GpsDiagnosticResult("GPS Antenna", GpsDiagnosticResult.Status.WARNING, "Cannot test antenna - GPS is off", "Enable GPS and run again"));
        } else {
            results.add(new GpsDiagnosticResult("GPS Antenna", GpsDiagnosticResult.Status.PASS, "GPS enabled - satellite test will check antenna quality", "If 0 satellites in open sky - antenna may be damaged"));
        }
    }

    private void checkLocationServicesEnabled() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false, networkEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {}

        if (gpsEnabled && networkEnabled) {
            results.add(new GpsDiagnosticResult("Location Services", GpsDiagnosticResult.Status.PASS, "GPS and Network location enabled", ""));
        } else if (gpsEnabled) {
            results.add(new GpsDiagnosticResult("Location Services", GpsDiagnosticResult.Status.WARNING, "GPS on but Network location off", "Enable Network location: Settings > Location > Mode > High accuracy"));
        } else if (networkEnabled) {
            results.add(new GpsDiagnosticResult("Location Services", GpsDiagnosticResult.Status.FAIL, "GPS is OFF! Only network location active", "Enable GPS: Settings > Location > Enable GPS"));
        } else {
            results.add(new GpsDiagnosticResult("Location Services", GpsDiagnosticResult.Status.FAIL, "All location services are OFF!", "Enable location: Settings > Location > Enable"));
        }
    }

    private void checkLocationMode() {
        try {
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            switch (mode) {
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                    results.add(new GpsDiagnosticResult("Location Mode", GpsDiagnosticResult.Status.PASS, "High accuracy mode (GPS + Network + WiFi)", ""));
                    break;
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                    results.add(new GpsDiagnosticResult("Location Mode", GpsDiagnosticResult.Status.WARNING, "Sensors only mode (GPS only)", "Change to High accuracy: Settings > Location > Mode"));
                    break;
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    results.add(new GpsDiagnosticResult("Location Mode", GpsDiagnosticResult.Status.WARNING, "Battery saving mode - GPS limited", "Change to High accuracy: Settings > Location > Mode"));
                    break;
                default:
                    results.add(new GpsDiagnosticResult("Location Mode", GpsDiagnosticResult.Status.FAIL, "Location off", "Enable: Settings > Location"));
            }
        } catch (Settings.SettingNotFoundException e) {
            results.add(new GpsDiagnosticResult("Location Mode", GpsDiagnosticResult.Status.UNKNOWN, "Cannot read location mode setting", "Check manually: Settings > Location"));
        }
    }

    private void checkMockLocationDisabled() {
        boolean mockEnabled = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String mockApp = Settings.Secure.getString(context.getContentResolver(), "mock_location");
                mockEnabled = (mockApp != null && !mockApp.isEmpty() && !mockApp.equals("0"));
            } else {
                mockEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
            }
        } catch (Exception e) {}

        if (!mockEnabled) {
            results.add(new GpsDiagnosticResult("Mock Location", GpsDiagnosticResult.Status.PASS, "Mock location is disabled", ""));
        } else {
            results.add(new GpsDiagnosticResult("Mock Location", GpsDiagnosticResult.Status.WARNING, "Mock location is ENABLED - may cause issues", "Disable: Developer Options > Select mock location app > None"));
        }
    }

    private void checkLocationPermissions() {
        boolean hasFine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasFine && hasCoarse) {
            results.add(new GpsDiagnosticResult("Location Permissions", GpsDiagnosticResult.Status.PASS, "All location permissions granted", ""));
        } else if (hasCoarse) {
            results.add(new GpsDiagnosticResult("Location Permissions", GpsDiagnosticResult.Status.WARNING, "Only coarse location granted", "Grant precise location in app settings"));
        } else {
            results.add(new GpsDiagnosticResult("Location Permissions", GpsDiagnosticResult.Status.FAIL, "Location permissions missing!", "Grant permissions: Settings > Apps > GPS Diagnostic > Permissions"));
        }
    }

    private void checkBatterySaverMode() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isPowerSaveMode()) {
            results.add(new GpsDiagnosticResult("Battery Saver", GpsDiagnosticResult.Status.WARNING, "Battery saver is ON - restricts GPS!", "Disable: Settings > Battery > Battery Saver > Off"));
        } else {
            results.add(new GpsDiagnosticResult("Battery Saver", GpsDiagnosticResult.Status.PASS, "Battery saver is off", ""));
        }
    }

    private void checkPowerSavingRestrictions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                boolean isIgnoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
                if (isIgnoring) {
                    results.add(new GpsDiagnosticResult("Power Restriction", GpsDiagnosticResult.Status.PASS, "App not restricted by battery optimization", ""));
                } else {
                    results.add(new GpsDiagnosticResult("Power Restriction", GpsDiagnosticResult.Status.WARNING, "App restricted by battery optimization", "Settings > Apps > GPS Diagnostic > Battery > Unrestricted"));
                }
            }
        }
    }

    private void checkMiuiBackgroundRestrictions() {
        if (!isMiuiDevice()) {
            results.add(new GpsDiagnosticResult("MIUI Background", GpsDiagnosticResult.Status.PASS, "Not a MIUI device - not relevant", ""));
            return;
        }
        results.add(new GpsDiagnosticResult("MIUI Background", GpsDiagnosticResult.Status.WARNING,
            "MIUI restricts background activity by default - common GPS issue!",
            "Fix: Settings > Apps > Manage apps > [App] > Background activity > No restrictions\n" +
            "Also: Settings > Apps > Manage apps > [App] > Battery saver > No restrictions"));
    }

    private void checkMiuiAutoStart() {
        if (!isMiuiDevice()) {
            results.add(new GpsDiagnosticResult("MIUI AutoStart", GpsDiagnosticResult.Status.PASS, "Not MIUI - not relevant", ""));
            return;
        }
        results.add(new GpsDiagnosticResult("MIUI AutoStart", GpsDiagnosticResult.Status.WARNING,
            "MIUI blocks autostart - may harm background location",
            "Enable: Settings > Apps > AutoStart > Find app > Enable\nOr: Security > Manage apps > AutoStart"));
    }

    private void checkMiuiBatteryOptimization() {
        if (!isMiuiDevice()) {
            results.add(new GpsDiagnosticResult("MIUI Battery", GpsDiagnosticResult.Status.PASS, "Not MIUI - not relevant", ""));
            return;
        }
        results.add(new GpsDiagnosticResult("MIUI Battery", GpsDiagnosticResult.Status.WARNING,
            "MIUI aggressive battery optimization kills GPS in background",
            "Fix:\n1. Settings > Battery > App battery saver > [App] > No restrictions\n" +
            "2. Security App > Battery > App battery saver > [App] > No restrictions\n" +
            "3. Lock app in Recent Apps (swipe down on card)"));
    }

    private boolean isMiuiDevice() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
               manufacturer.contains("poco") || brand.contains("xiaomi") ||
               brand.contains("redmi") || brand.contains("poco");
    }

    private void checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            results.add(new GpsDiagnosticResult("Network", GpsDiagnosticResult.Status.PASS, "Connected: " + activeNetwork.getTypeName(), ""));
        } else {
            results.add(new GpsDiagnosticResult("Network", GpsDiagnosticResult.Status.WARNING, "No network - A-GPS won't work", "Connect WiFi or mobile data for faster GPS fix"));
        }
    }

    private void checkAGPS() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean hasInternet = (ni != null && ni.isConnected());
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = false;
        try { networkProvider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception e) {}

        if (hasInternet && networkProvider) {
            results.add(new GpsDiagnosticResult("A-GPS", GpsDiagnosticResult.Status.PASS, "A-GPS available - fast satellite lock", ""));
        } else {
            results.add(new GpsDiagnosticResult("A-GPS", GpsDiagnosticResult.Status.WARNING, "A-GPS unavailable - slow satellite lock", "Enable internet and network location for A-GPS"));
        }
    }

    private void checkWifiScanning() {
        try {
            boolean wifiScan = Settings.Global.getInt(context.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
            if (wifiScan) {
                results.add(new GpsDiagnosticResult("WiFi Scanning", GpsDiagnosticResult.Status.PASS, "WiFi scanning for location enabled", ""));
            } else {
                results.add(new GpsDiagnosticResult("WiFi Scanning", GpsDiagnosticResult.Status.WARNING, "WiFi scanning for location disabled", "Enable: Settings > Location > WiFi scanning"));
            }
        } catch (Exception e) {
            results.add(new GpsDiagnosticResult("WiFi Scanning", GpsDiagnosticResult.Status.UNKNOWN, "Cannot check WiFi scanning setting", ""));
        }
    }

    private void checkCompass() {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor compass = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (compass != null) {
            results.add(new GpsDiagnosticResult("Compass", GpsDiagnosticResult.Status.PASS, "Magnetic sensor OK: " + compass.getName(), ""));
        } else {
            results.add(new GpsDiagnosticResult("Compass", GpsDiagnosticResult.Status.WARNING, "Magnetic sensor not found", "Calibrate: rotate phone in figure-8 motion"));
        }
    }

    private void checkAccelerometer() {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            results.add(new GpsDiagnosticResult("Accelerometer", GpsDiagnosticResult.Status.PASS, "Accelerometer OK: " + accel.getName(), ""));
        } else {
            results.add(new GpsDiagnosticResult("Accelerometer", GpsDiagnosticResult.Status.WARNING, "Accelerometer not found", "This sensor helps location accuracy. May be damaged."));
        }
    }

    private void checkGyroscope() {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyro != null) {
            results.add(new GpsDiagnosticResult("Gyroscope", GpsDiagnosticResult.Status.PASS, "Gyroscope OK: " + gyro.getName(), ""));
        } else {
            results.add(new GpsDiagnosticResult("Gyroscope", GpsDiagnosticResult.Status.WARNING, "Gyroscope not found", "Not critical but helps accuracy"));
        }
    }

    private void checkSatelliteVisibility() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try { gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception e) {}

        if (!gpsEnabled) {
            results.add(new GpsDiagnosticResult("Satellites", GpsDiagnosticResult.Status.WARNING, "GPS off - cannot check satellites", "Enable GPS and test outdoors"));
        } else {
            results.add(new GpsDiagnosticResult("Satellites", GpsDiagnosticResult.Status.PASS, "GPS enabled - live satellite tracking active", "If 0 satellites after 2 min outdoors - antenna may be damaged"));
        }
    }

    private void checkTimeAndTimezone() {
        boolean autoTime = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0) == 1;
        boolean autoZone = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0) == 1;

        if (autoTime && autoZone) {
            results.add(new GpsDiagnosticResult("Time & Timezone", GpsDiagnosticResult.Status.PASS, "Auto time and timezone - OK", ""));
        } else {
            results.add(new GpsDiagnosticResult("Time & Timezone", GpsDiagnosticResult.Status.WARNING, "Manual time/timezone - may affect A-GPS", "Enable auto time: Settings > Date & Time > Automatic"));
        }
    }

    private void checkAirplaneMode() {
        boolean airplane = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        if (!airplane) {
            results.add(new GpsDiagnosticResult("Airplane Mode", GpsDiagnosticResult.Status.PASS, "Airplane mode off", ""));
        } else {
            results.add(new GpsDiagnosticResult("Airplane Mode", GpsDiagnosticResult.Status.FAIL, "Airplane mode ON! Blocks GPS and network", "Turn off airplane mode immediately"));
        }
    }

    public String getSummary() {
        int fails = 0, warnings = 0, passes = 0;
        for (GpsDiagnosticResult r : results) {
            switch (r.getStatus()) {
                case FAIL: fails++; break;
                case WARNING: warnings++; break;
                case PASS: passes++; break;
            }
        }
        if (fails == 0 && warnings == 0) return "\uD83C\uDF89 All tests passed! If GPS still not working - possible hardware (antenna) issue";
        else if (fails > 0) return "\uD83D\uDD34 Found " + fails + " critical issues and " + warnings + " warnings. See details below.";
        else return "\uD83D\uDFE1 Found " + warnings + " warnings. Fix them and test again.";
    }

    public String getDeviceInfo() {
        return "\uD83D\uDCF1 Manufacturer: " + Build.MANUFACTURER +
               "\n\uD83D\uDCF1 Model: " + Build.MODEL +
               "\n\uD83D\uDCF1 Device: " + Build.DEVICE +
               "\n\uD83E\uDD16 Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")" +
               "\n\uD83D\uDD27 Board: " + Build.BOARD +
               "\n\uD83D\uDD27 Hardware: " + Build.HARDWARE;
    }
}
