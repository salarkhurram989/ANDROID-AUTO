package com.salardev.autodash;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 1001;
    private static final int MEDIA_REQUEST = 1002;
    private static final String DASHBOARD_URL = "https://salarkhurram989.github.io/ANDROID-AUTO/";
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternalUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(Uri.parse(url));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback) {
                if (origin.startsWith("https://salarkhurram989.github.io/")) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        callback.invoke(origin, true, false);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
                        callback.invoke(origin, false, false);
                    }
                } else {
                    callback.invoke(origin, false, false);
                }
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        setContentView(webView);
        webView.loadUrl(DASHBOARD_URL);
        requestMediaPermissionIfNeeded();
    }

    private void requestMediaPermissionIfNeeded() {
        String permission;
        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, MEDIA_REQUEST);
        }
    }

    private boolean handleExternalUrl(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return false;

        if ("geo".equalsIgnoreCase(scheme)) {
            openNavigation(uri);
            return true;
        }

        if ("spotify".equalsIgnoreCase(scheme)) {
            openSpotifyOrMusicApp();
            return true;
        }

        return false;
    }

    private void openNavigation(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(Intent.createChooser(intent, "Choose navigation app"));
        } catch (Exception e) {
            Toast.makeText(this, "No compatible navigation app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSpotifyOrMusicApp() {
        try {
            Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"));
            startActivity(spotify);
            return;
        } catch (Exception ignored) {
            // Spotify is not installed; use the Android music-app selector.
        }

        try {
            Intent music = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            startActivity(music);
        } catch (Exception e) {
            Toast.makeText(this, "No compatible music app installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location enabled. Press GPS again.", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == MEDIA_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Local music library enabled.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
