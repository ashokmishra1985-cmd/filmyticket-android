package com.filmyticket.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutOffline;
    private Button btnRetry;

    private ValueCallback<Uri[]> fileUploadCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;

    private static final String APP_URL = "https://filmyticket.com";
    private long backPressedTime = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        layoutOffline = findViewById(R.id.layoutOffline);
        btnRetry = findViewById(R.id.btnRetry);

        // Setup File Chooser launcher (for camera and file uploads in WebView)
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (fileUploadCallback != null) {
                        Uri[] results = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            if (result.getData().getData() != null) {
                                results = new Uri[]{result.getData().getData()};
                            } else if (result.getData().getClipData() != null) {
                                int count = result.getData().getClipData().getItemCount();
                                results = new Uri[count];
                                for (int i = 0; i < count; i++) {
                                    results[i] = result.getData().getClipData().getItemAt(i).getUri();
                                }
                            }
                        }
                        fileUploadCallback.onReceiveValue(results);
                        fileUploadCallback = null;
                    }
                }
        );

        setupWebView();
        setupSwipeRefresh();
        setupBackNavigation();

        btnRetry.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                layoutOffline.setVisibility(View.GONE);
                swipeRefresh.setVisibility(View.VISIBLE);
                webView.reload();
            } else {
                Toast.makeText(MainActivity.this, R.string.no_internet_msg, Toast.LENGTH_SHORT).show();
            }
        });

        if (isNetworkAvailable()) {
            webView.loadUrl(APP_URL);
        } else {
            showOfflineView();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setGeolocationEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Append custom app identifier to UserAgent
        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString(defaultUA + " FilmyTicketApp/1.0.0");

        // Enable Cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new FilmyticketWebViewClient());
        webView.setWebChromeClient(new FilmyticketWebChromeClient());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                layoutOffline.setVisibility(View.GONE);
                swipeRefresh.setVisibility(View.VISIBLE);
                webView.reload();
            } else {
                swipeRefresh.setRefreshing(false);
                showOfflineView();
            }
        });
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish();
                    } else {
                        backPressedTime = System.currentTimeMillis();
                        Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void showOfflineView() {
        swipeRefresh.setVisibility(View.GONE);
        layoutOffline.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private class FilmyticketWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrl(request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrl(url);
        }

        private boolean handleUrl(String url) {
            if (url == null) return false;

            // Keep internal filmyticket pages in WebView
            if (url.startsWith("https://filmyticket.com") || url.startsWith("http://filmyticket.com")) {
                return false;
            }

            // Handle UPI / Payment App schemes (PhonePe, Google Pay, Paytm, UPI)
            if (url.startsWith("upi://") || url.startsWith("phonepe://") ||
                url.startsWith("paytmmp://") || url.startsWith("tez://") || url.startsWith("bhim://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No UPI payment app found on this device", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            // Handle tel:, mailto:, sms:, whatsapp:
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") ||
                url.startsWith("whatsapp:") || url.contains("api.whatsapp.com")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }

            // Open external third-party links in default browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                if (!isNetworkAvailable()) {
                    showOfflineView();
                }
            }
        }
    }

    private class FilmyticketWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // Automatically allow geolocation for filmyticket to detect cinema city
            callback.invoke(origin, true, false);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (fileUploadCallback != null) {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                fileChooserLauncher.launch(intent);
            } catch (Exception e) {
                fileUploadCallback = null;
                return false;
            }
            return true;
        }
    }
}
