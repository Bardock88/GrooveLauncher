package web.bmdominatezz.gravy;

import static web.bmdominatezz.gravy.DefaultApps.*;
import static web.bmdominatezz.gravy.GrooveExperience.*;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarException;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class WebInterface {
    private final MainActivity mainActivity;
    private final GrooveWebView webView;

    WebInterface(MainActivity mainActivity, GrooveWebView webView) {
        this.mainActivity = mainActivity;
        this.webView = webView;
    }

    public float getDevicePixelRatio() {
        DisplayMetrics displayMetrics = mainActivity.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

    // Show a toast from the web page.
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mainActivity, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void showToastHelloWorld() {
        Toast.makeText(mainActivity, "Hello world! Oh and hello Groove!", Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public String getSystemInsets() throws JSONException {
        JSONObject systemInsets = new JSONObject();
        systemInsets.put("left", (mainActivity.webView.lastInsets == null) ? 0
                : mainActivity.webView.lastInsets.left / getDevicePixelRatio());
        systemInsets.put("top", (mainActivity.webView.lastInsets == null) ? 0
                : mainActivity.webView.lastInsets.top / getDevicePixelRatio());
        systemInsets.put("right", (mainActivity.webView.lastInsets == null) ? 0
                : mainActivity.webView.lastInsets.right / getDevicePixelRatio());
        systemInsets.put("bottom", (mainActivity.webView.lastInsets == null) ? 0
                : mainActivity.webView.lastInsets.bottom / getDevicePixelRatio());
        return systemInsets.toString();
    }

    @JavascriptInterface
    public String retrieveApps() throws JSONException {
        mainActivity.webView.retrieveApps();
        Log.d("groovelauncher", "retrieveApps: " + mainActivity.webView.retrievedApps.toString());
        JSONArray retrievedApps = new JSONArray();

        // First, count how many intents each package has
        Map<String, Integer> packageIntentCount = new HashMap<>();
        for (ResolveInfo resolveInfo : mainActivity.webView.retrievedApps) {
            String packageName = resolveInfo.activityInfo.packageName;
            packageIntentCount.put(packageName, packageIntentCount.getOrDefault(packageName, 0) + 1);
        }

        // Now process each app
        for (ResolveInfo resolveInfo : mainActivity.webView.retrievedApps) {
            if (!resolveInfo.activityInfo.packageName.equals("web.bmdominatezz.gravy")) {
                JSONObject appInfo = new JSONObject();
                String packageName = resolveInfo.activityInfo.packageName;
                String packageNameWithIntent;

                // Only add activity name if package has multiple intents
                if (packageIntentCount.get(packageName) > 1) {
                    packageNameWithIntent = packageName + "/" + resolveInfo.activityInfo.name;
                } else {
                    packageNameWithIntent = packageName;
                }

                appInfo.put("packageName", packageNameWithIntent);
                appInfo.put("label", resolveInfo.loadLabel(mainActivity.packageManager).toString());
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    appInfo.put("type", 0);
                } else {
                    appInfo.put("type", 1);
                }
                retrievedApps.put(appInfo);
            }
        }
        JSONObject grooveSettings = new JSONObject();
        grooveSettings.put("packageName", "groove.internal.settings");
        grooveSettings.put("label", "Groove Settings");
        grooveSettings.put("type", 0);
        retrievedApps.put(grooveSettings);
        return retrievedApps.toString();
    }

    @JavascriptInterface
    public String retrieveContacts() throws JSONException {
        JSONArray contactsArray = new JSONArray();
        android.content.ContentResolver contentResolver = mainActivity.getContentResolver();

        // Columns to retrieve
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        };

        // Query contacts
        android.database.Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                // Get contact ID
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);

                String contactId = cursor.getString(idIndex);
                String contactName = cursor.getString(nameIndex);

                // Create JSON object for contact
                JSONObject contactObject = new JSONObject();
                contactObject.put("id", contactId);
                contactObject.put("name", contactName != null ? contactName : "");

                contactsArray.put(contactObject);
            }
            cursor.close();
        }

        return contactsArray.toString();
    }

    @JavascriptInterface
    public String getAppLabel(String packageName) {
        try {
            ApplicationInfo appInfo = mainActivity.packageManager.getApplicationInfo(packageName, 0);
            return mainActivity.packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // Or return a default value
        }
    }

    @JavascriptInterface
    public String getAppIconURL(String packageName) throws JSONException {
        PackageNameInfo packageNameInfo = parsePackageName(packageName);

        // '{"foreground":"http://localhost:5500/www/mock/icons/default/.png","background":"data:image/svg+xml,<svg
        // xmlns=\\"http://www.w3.org/2000/svg\\"/>"}
        JSONObject appicon = new JSONObject();
        appicon.put("foreground", "https://appassets.androidplatform.net/assets/icons/"
                + (packageNameInfo.packageName == null ? "undefined"
                : packageNameInfo.packageName + "|" + packageNameInfo.intentId)
                + ".webp");
        appicon.put("background", "https://appassets.androidplatform.net/assets/icons-bg/"
                + (packageNameInfo.packageName == null ? "undefined"
                : packageNameInfo.packageName + "|" + packageNameInfo.intentId)
                + ".webp");
        return appicon.toString();
        // return "https://appassets.androidplatform.net/assets/icons/" + (packageName
        // == null ? "undefined" : packageName) + ".webp";
    }

    // Inner class to hold package name and intent ID
    public static class PackageNameInfo {
        public final String packageName;
        public final String intentId;

        public PackageNameInfo(String packageName, String intentId) {
            this.packageName = packageName;
            this.intentId = intentId;
        }
    }

    public PackageNameInfo parsePackageName(String packageNameWithIntent) {
        String packageName, intentId;

        // Check if packageNameWithIntent contains '/'
        if (packageNameWithIntent.contains("/")) {
            String[] parts = packageNameWithIntent.split("/", 2);
            packageName = parts[0];
            intentId = parts[1];
        } else {
            packageName = packageNameWithIntent;
            intentId = null;
        }

        return new PackageNameInfo(packageName, intentId);
    }

    @JavascriptInterface
    public boolean launchApp(String packageNameWithIntent) {
        String packageName, intentId;

        // Check if packageNameWithIntent contains '/'
        if (packageNameWithIntent.contains("/")) {
            String[] parts = packageNameWithIntent.split("/", 2);
            packageName = parts[0];
            intentId = parts[1];
        } else {
            packageName = packageNameWithIntent;
            intentId = null;
        }

        if (packageName.startsWith("groove.internal")) {
            mainActivity.webView.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.webView.evaluateJavascript(
                            "window.GrooveBoard.backendMethods.launchInternalApp(\"" + packageName + "\")", null);
                }
            });
            return true;
        } else {
            Intent intent = null;

            // If intentId is null, use default launch intent
            if (intentId == null) {
                intent = mainActivity.packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    mainActivity.startActivity(intent);
                    return true;
                }
                return false;
            }

            // Otherwise, search for specific intent
            List<ResolveInfo> activities = mainActivity.packageManager.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                    PackageManager.MATCH_ALL);

            for (ResolveInfo resolveInfo : activities) {
                if (resolveInfo.activityInfo.packageName.equals(packageName) &&
                        resolveInfo.activityInfo.name.equals(intentId)) {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setClassName(packageName, resolveInfo.activityInfo.name);
                    break;
                }
            }

            if (intent != null) {
                mainActivity.startActivity(intent);
                return true;
            }
            return false;
        }
    }

    String TAG = "groovelauncher";

    public boolean uninstallAppWithShizuku(String packageName) {
        String command = "su pm uninstall " + packageName;
        Log.d(TAG, "Uninstall command: " + command);
        try {
            ShizukuBinderWrapper binder = new ShizukuBinderWrapper(Shizuku.getBinder());
            binder.transact(IBinder.FIRST_CALL_TRANSACTION, Parcel.obtain(), Parcel.obtain(), 0);
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "Command output: " + line);
            }

            int exitCode = process.waitFor();
            Log.d(TAG, "Command exit code: " + exitCode);
        } catch (RemoteException | IOException | InterruptedException e) {
            Log.e(TAG, "Error executing uninstall command", e);
        }
        return true;
    }

    public boolean uninstallAppWithRoot(String packageName) {
        try {
            // Run the uninstall command as root
            Process process = Runtime.getRuntime().exec(new String[]{"pm uninstall " + packageName});

            // Optional: read output if you need to handle success or failure
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            reader.close();

            // Wait for the process to finish
            process.waitFor();

            // Check for success (exit code 0 means success)
            if (process.exitValue() == 0) {
                Log.d("RootUninstall", "Uninstall successful for " + packageName);
                return true;
            } else {
                Log.e("RootUninstall", "Uninstall failed for " + packageName);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Log.e("RootUninstall", "Error executing root command: " + e.getMessage());
        }
        return false;
    }

    @JavascriptInterface
    public boolean uninstallApp(String packageNameWithIntent) {
        String packageName = packageNameWithIntent;
        if (packageNameWithIntent.contains("/")) {
            packageName = packageNameWithIntent.split("/", 2)[0];
        }
        return uninstallApp(packageName, 0);
    }

    @JavascriptInterface
    public boolean uninstallApp(String packageNameWithIntent, int packageManagerProvider) {
        String packageName = packageNameWithIntent;
        if (packageNameWithIntent.contains("/")) {
            packageName = packageNameWithIntent.split("/", 2)[0];
        }
        Log.d("groovelaauncher", "uninstallApp pm provider: " + packageManagerProvider);
        switch (packageManagerProvider) {
            case 0:
                Intent appIntent = mainActivity.packageManager.getLaunchIntentForPackage(packageName);
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + packageName));
                if (appIntent != null) {
                    mainActivity.startActivity(intent);
                    return true;
                }
                return false;
            case 1:
                return uninstallAppWithRoot(packageName);
            case 2:
                return uninstallAppWithShizuku(packageName);
        }
        return false;
    }

    @JavascriptInterface
    public boolean launchAppInfo(String packageNameWithIntent) {
        String packageName = packageNameWithIntent;
        if (packageNameWithIntent.contains("/")) {
            packageName = packageNameWithIntent.split("/", 2)[0];
        }

        Intent appIntent = mainActivity.packageManager.getLaunchIntentForPackage(packageName);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        if (appIntent != null) {
            mainActivity.startActivity(intent);
            return true;
        }
        return false;
    }

    String statusBarAppearance = "light";

    @JavascriptInterface
    public void setStatusBarAppearance(String appearance) {
        statusBarAppearance = appearance;
        Window window = mainActivity.getWindow();
        View decorView = window.getDecorView();

        mainActivity.runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    WindowInsetsController insetsController = window.getInsetsController();
                    if (insetsController != null) {
                        switch (appearance) {
                            case "hide":
                                insetsController.hide(WindowInsets.Type.statusBars());
                                break;
                            case "dark":
                                insetsController.show(WindowInsets.Type.statusBars());
                                insetsController.setSystemBarsAppearance(
                                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                                break;
                            case "light":
                                insetsController.show(WindowInsets.Type.statusBars());
                                insetsController.setSystemBarsAppearance(
                                        0,
                                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                                break;
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0 to Android 10 (API 23-29)
                    int flags = decorView.getSystemUiVisibility();
                    switch (appearance) {
                        case "hide":
                            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                            break;
                        case "dark":
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            flags &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
                            break;
                        case "light":
                            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_FULLSCREEN);
                            break;
                    }
                    decorView.setSystemUiVisibility(flags);
                }
            } catch (Exception e) {
                Log.e("WebInterface", "Error setting status bar appearance", e);
            }
        });
    }

    @JavascriptInterface
    public String getStatusBarAppearance() {
        return statusBarAppearance;
    }

    String navigationBarAppearance = "light";

    @JavascriptInterface
    public void setNavigationBarAppearance(String appearance) {
        navigationBarAppearance = appearance;
        Window window = mainActivity.getWindow();
        View decorView = window.getDecorView();

        mainActivity.runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    WindowInsetsController insetsController = window.getInsetsController();
                    if (insetsController != null) {
                        switch (appearance) {
                            case "hide":
                                insetsController.hide(WindowInsets.Type.navigationBars());
                                break;
                            case "dark":
                                insetsController.show(WindowInsets.Type.navigationBars());
                                window.setNavigationBarColor(android.graphics.Color.WHITE);
                                insetsController.setSystemBarsAppearance(
                                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                                break;
                            case "light":
                                insetsController.show(WindowInsets.Type.navigationBars());
                                window.setNavigationBarColor(android.graphics.Color.BLACK);
                                insetsController.setSystemBarsAppearance(
                                        0,
                                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                                break;
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0 to Android 10 (API 26-29)
                    int flags = decorView.getSystemUiVisibility();
                    switch (appearance) {
                        case "hide":
                            flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                            break;
                        case "dark":
                            window.setNavigationBarColor(android.graphics.Color.WHITE);
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                            flags &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                            break;
                        case "light":
                            window.setNavigationBarColor(android.graphics.Color.BLACK);
                            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                            break;
                    }
                    decorView.setSystemUiVisibility(flags);
                }
            } catch (Exception e) {
                Log.e("WebInterface", "Error setting navigation bar appearance", e);
            }
        });
    }

    @JavascriptInterface
    public String getNavigationBarAppearance() {
        return navigationBarAppearance;
    }

    @JavascriptInterface
    public void searchStore(String appName) {
        Uri uri = Uri.parse("market://search?q=" + appName);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (intent.resolveActivity(mainActivity.packageManager) != null) {
            mainActivity.startActivity(intent);
        } else {
        }
    }

    @JavascriptInterface
    public void setUIScale(float scale) {
        mainActivity.webView.post(new Runnable() {
            @Override
            public void run() {
                mainActivity.webView.setInitialScale(Math.round(scale * 100 * getDevicePixelRatio()));
                mainActivity.webView.evaluateJavascript("document.body.style.setProperty('--ui-scale'," + scale + ")",
                        null);
            }
        });
    }

    @JavascriptInterface
    public void openURL(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        mainActivity.startActivity(intent);
    }

    @JavascriptInterface
    public String getAppVersion() {
        try {
            PackageInfo packageInfo = mainActivity.packageManager.getPackageInfo(mainActivity.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    @JavascriptInterface
    public String getWebViewVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API Level 26
            PackageInfo webViewPackageInfo = mainActivity.webView.getCurrentWebViewPackage();
            if (webViewPackageInfo != null) {
                String versionName = webViewPackageInfo.versionName;
                int versionCode = webViewPackageInfo.versionCode;
                // Use versionName and versionCode as needed
                System.out.println("WebView version: " + versionName + " (code: " + versionCode + ")");
                return versionName + " (code: " + versionCode + ")";
            } else {
                System.out.println("WebView package info is not available");
                return "Unknown";
            }
        } else {
            // Fallback for older Android versions
            System.out.println("API level is lower than 26. WebView version not directly accessible.");
            return "Unknown";
        }
    }

    @JavascriptInterface
    public boolean isShizukuAvailable() {
        // Check if Shizuku service is available (Shizuku is running on the device)
        if (!Shizuku.pingBinder()) {
            // Shizuku is not available
            return false;
        }
        // Check if the app has permission to use Shizuku
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            // Request Shizuku permission
            int shizukuRequestCode = 1234;
            Shizuku.requestPermission(shizukuRequestCode);
            return false;
        }
        // Shizuku is available and permission is granted
        return true;
    }

    @JavascriptInterface
    public boolean isDeviceRooted() {
        try {
            // Try executing 'su' command to check for root access
            Process process = Runtime.getRuntime().exec("su");
            // Wait for the process to complete
            process.waitFor();
            // If the exit value is 0, root access is available
            return (process.exitValue() == 0);
        } catch (Exception e) {
            // Root access denied or unavailable
            return false;
        }
    }


    @JavascriptInterface
    public String getDefaultApps() throws JSONException {
        JSONObject defaultApps = new JSONObject();
        defaultApps.put("phoneApp", getDefaultPhoneAppPackageName(mainActivity));
        defaultApps.put("messageApp", getDefaultMessagingAppPackageName(mainActivity));
        defaultApps.put("browserApp", getDefaultBrowserPackageName(mainActivity));
        defaultApps.put("mailApp", getDefaultMailAppPackageName(mainActivity));
        defaultApps.put("storeApp", getDefaultStoreAppPackageName(mainActivity));
        defaultApps.put("contactsApp", getDefaultContactsAppPackageName(mainActivity));
        defaultApps.put("musicApp", getDefaultMusicPlayerAppPackageName(mainActivity));
        defaultApps.put("galleryApp", getDefaultGalleryAppPackageName(mainActivity));
        return defaultApps.toString();
    }

    @JavascriptInterface
    public String copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) mainActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", text);
            clipboard.setPrimaryClip(clip);
            return "true";
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getDisplayOrientation() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mainActivity.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        if (metrics.widthPixels > metrics.heightPixels) {
            return "landscape";
        } else {
            return "portrait";
        }
    }

    @JavascriptInterface
    public void setDisplayOrientationLock(String key) {
        if (key.equals("auto")) {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else if (key.equals("landscape")) {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (key.equals("portrait")) {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @JavascriptInterface
    public String getContacts() throws JSONException {
        return GrooveExperience.getContacts(mainActivity);
    }
    @JavascriptInterface
    public String getPhotos() throws JSONException {
        return GrooveExperience.getPhotos(mainActivity);
    }
    @JavascriptInterface
    public String getContactAvatarURL(String contactId) {
        return "https://appassets.androidplatform.net/assets/contact-icon/" + contactId + ".webp";
    }
    @JavascriptInterface
    public String getPhotoURL(String photoId){
        return "https://appassets.androidplatform.net/assets/photos/" + photoId + ".webp";

    }
}