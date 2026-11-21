# Filmyticket Android App (Automated APK Builder)

This repository contains the complete Android application wrapper for [Filmyticket.com](https://filmyticket.com), pre-configured with:
- **Instant Web-Sync:** Every update on filmyticket.com is immediately visible in the app without re-releasing APKs.
- **Native Splash Screen** with official Filmyticket logo.
- **UPI & Payment Gateway Routing:** Supports native UPI intents (PhonePe, Google Pay, Paytm, BHIM).
- **Geolocation & City Detection:** Automatic location permission handling for cinema selection.
- **Pull-To-Refresh** (`SwipeRefreshLayout`).
- **Smooth Progress Bar** loading indicator.
- **Smart Back Navigation:** Navigates in WebView history with double-tap exit prevention.
- **Offline Fallback Screen:** Styled retry screen when device is disconnected.

---

## How to Get Your APK Using GitHub Actions (No Android Studio Needed!)

1. **Create a New GitHub Repository:**
   - Go to [GitHub.com](https://github.com) and click **New Repository**.
   - Name it `filmyticket-android` (can be **Private** or **Public**).

2. **Push this Project to your GitHub Repository:**
   Open PowerShell in this directory (`filmyticket-android`) and run:
   ```bash
   git init
   git add .
   git commit -m "Initial commit for Filmyticket Android App"
   git branch -M main
   git remote add origin https://github.com/<YOUR-GITHUB-USERNAME>/filmyticket-android.git
   git push -u origin main
   ```

3. **Download Your APK:**
   - Go to the **Actions** tab on your GitHub repository.
   - You will see the **Build Filmyticket Android APK** workflow running automatically.
   - It will take about 2 to 3 minutes to compile.
   - Once it turns green (Success), click on the run.
   - Under **Artifacts** at the bottom of the page, click **`filmyticket-debug-apk`**.
   - A `.zip` file containing **`app-debug.apk`** will download!
   - Transfer this `.apk` to your phone or send it via WhatsApp/Drive, tap to install, and enjoy your Filmyticket Android App!
