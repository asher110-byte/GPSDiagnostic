package com.xiaomi.gpsdiag;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private LinearLayout resultsContainer;
    private TextView tvSummary;
    private TextView tvDeviceInfo;
    private TextView tvSatelliteInfo;
    private Button btnRunDiagnostic;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    private GpsDiagnosticEngine diagnosticEngine;
    private GnssStatus.Callback gnssCallback;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultsContainer = findViewById(R.id.resultsContainer);
        tvSummary = findViewById(R.id.tvSummary);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvSatelliteInfo = findViewById(R.id.tvSatelliteInfo);
        btnRunDiagnostic = findViewById(R.id.btnRunDiagnostic);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);

        diagnosticEngine = new GpsDiagnosticEngine(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        btnRunDiagnostic.setOnClickListener(v -> startDiagnostic());
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        };
        boolean needRequest = false;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startDiagnostic() {
        btnRunDiagnostic.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();
        tvSummary.setText("Running diagnostic...");
        tvSatelliteInfo.setText("");

        new Thread(() -> {
            List<GpsDiagnosticResult> results = diagnosticEngine.runAllTests();
            String summary = diagnosticEngine.getSummary();
            String deviceInfo = diagnosticEngine.getDeviceInfo();
            runOnUiThread(() -> {
                displayResults(results, summary, deviceInfo);
                startSatelliteMonitor();
            });
        }).start();
    }

    private void displayResults(List<GpsDiagnosticResult> results, String summary, String deviceInfo) {
        progressBar.setVisibility(View.GONE);
        btnRunDiagnostic.setEnabled(true);
        btnRunDiagnostic.setText("\uD83D\uDD04 Run Again");
        tvSummary.setText(summary);
        tvDeviceInfo.setText(deviceInfo);
        resultsContainer.removeAllViews();

        for (GpsDiagnosticResult result : results) {
            View card = createResultCard(result);
            resultsContainer.addView(card);
        }
        scrollView.post(() -> scrollView.smoothScrollTo(0, tvSummary.getTop()));
    }

    private View createResultCard(GpsDiagnosticResult result) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(dpToPx(2));
        cardView.setContentPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        switch (result.getStatus()) {
            case FAIL: cardView.setCardBackgroundColor(0xFFFFEBEE); break;
            case WARNING: cardView.setCardBackgroundColor(0xFFFFF8E1); break;
            case PASS: cardView.setCardBackgroundColor(0xFFE8F5E9); break;
            default: cardView.setCardBackgroundColor(0xFFECEFF1);
        }

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(result.getStatusEmoji() + " " + result.getTestName());
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF212121);
        contentLayout.addView(tvTitle);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(result.getDetails());
        tvDetails.setTextSize(14);
        tvDetails.setTextColor(0xFF424242);
        tvDetails.setPadding(0, dpToPx(4), 0, 0);
        contentLayout.addView(tvDetails);

        if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
            TextView tvRec = new TextView(this);
            tvRec.setText("\uD83D\uDCA1 " + result.getRecommendation());
            tvRec.setTextSize(13);
            tvRec.setTextColor(0xFF1565C0);
            tvRec.setPadding(0, dpToPx(6), 0, 0);
            contentLayout.addView(tvRec);
        }

        cardView.addView(contentLayout);
        return cardView;
    }

    private void startSatelliteMonitor() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            tvSatelliteInfo.setText("No location permission - cannot monitor satellites");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    int totalSats = status.getSatelliteCount();
                    int usedSats = 0;
                    float maxSnr = 0;
                    int gps = 0, glonass = 0, beidou = 0, galileo = 0;

                    for (int i = 0; i < totalSats; i++) {
                        if (status.usedInFix(i)) usedSats++;
                        float snr = status.getCn0DbHz(i);
                        if (snr > maxSnr) maxSnr = snr;
                        switch (status.getConstellationType(i)) {
                            case GnssStatus.CONSTELLATION_GPS: gps++; break;
                            case GnssStatus.CONSTELLATION_GLONASS: glonass++; break;
                            case GnssStatus.CONSTELLATION_BEIDOU: beidou++; break;
                            case GnssStatus.CONSTELLATION_GALILEO: galileo++; break;
                        }
                    }

                    String info = "\uD83D\uDCE1 Satellites: " + usedSats + "/" + totalSats + " in use\n" +
                                  "\uD83D\uDCF6 Max signal: " + String.format("%.1f", maxSnr) + " dBHz\n" +
                                  "\uD83D\uDEF0 GPS: " + gps + " | GLONASS: " + glonass + " | BeiDou: " + beidou + " | Galileo: " + galileo;

                    if (usedSats == 0 && totalSats == 0) info += "\n\n\u26A0\uFE0F No satellites at all - possible antenna issue!";
                    else if (usedSats == 0) info += "\n\n\u26A0\uFE0F Satellites visible but none used - weak signal";
                    else if (maxSnr < 20) info += "\n\n\u26A0\uFE0F Very weak signal - check antenna or go outdoors";
                    else info += "\n\n\u2705 Signal OK";

                    runOnUiThread(() -> tvSatelliteInfo.setText(info));
                }
                @Override
                public void onStarted() {
                    runOnUiThread(() -> tvSatelliteInfo.setText("\uD83D\uDCE1 Searching for satellites..."));
                }
            };
            locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper()));
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, location -> {});
            } catch (Exception e) {
                tvSatelliteInfo.setText("Error monitoring satellites: " + e.getMessage());
            }
        } else {
            tvSatelliteInfo.setText("Satellite monitoring requires Android 7.0+");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gnssCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(gnssCallback);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
