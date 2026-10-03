package io.github.konbini.market.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Locale;

public class DownloadService extends Service {
    public static final String ACTION_START = "io.github.konbini.market.DOWNLOAD_START";
    public static final String ACTION_CANCEL = "io.github.konbini.market.DOWNLOAD_CANCEL";
    public static final String ACTION_PROGRESS = "io.github.konbini.market.DOWNLOAD_PROGRESS";
    private static final String TAG = "DownloadService";
    private static final String PREFS = "download_state";

    private static class TaskInfo {
        int appId;
        String appName = "";
        String icon = "";
        volatile boolean cancel = false;
        int percent = 0;
        long speed = 0;
        boolean installing = false;
        String statusText = "0%";
        String fileName = "";
        String filePath = "";
        String errorMessage = "";
    }

    private static class HttpStatusException extends IOException {
        final int statusCode;

        HttpStatusException(int statusCode) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }

    private static final Class<?>[] START_FOREGROUND_SIG = new Class[] {
            int.class, Notification.class
    };

    private static final Class<?>[] STOP_FOREGROUND_SIG = new Class[] {
            boolean.class
    };

    private Method mStartForeground;
    private Method mStopForeground;
    private Method mSetForeground;

    private final Object[] mStartForegroundArgs = new Object[2];
    private final Object[] mStopForegroundArgs = new Object[1];
    private final Object[] mSetForegroundArgs = new Object[1];

    private static final ConcurrentHashMap<Integer, TaskInfo> TASKS =
            new ConcurrentHashMap<Integer, TaskInfo>();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mStartForeground = getClass().getMethod("startForeground", START_FOREGROUND_SIG);
            mStopForeground = getClass().getMethod("stopForeground", STOP_FOREGROUND_SIG);
        } catch (NoSuchMethodException e) {
            mStartForeground = null;
            mStopForeground = null;
        }

        try {
            mSetForeground = getClass().getMethod("setForeground", new Class[] { boolean.class });
        } catch (NoSuchMethodException e) {
            mSetForeground = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocaleHelper.applySavedLocale(this);

        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if (ACTION_CANCEL.equals(action)) {
            int id = intent.getIntExtra("app_id", -1);
            TaskInfo t = TASKS.get(id);
            if (t != null) t.cancel = true;
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            final int appId = intent.getIntExtra("app_id", -1);
            final String url = intent.getStringExtra("url");
            final String fileName = intent.getStringExtra("file_name");
            final String appPackage = intent.getStringExtra("app_package");
            final String appName = intent.getStringExtra("app_name");
            final String icon = intent.getStringExtra("icon");

            if (appId < 0 || url == null || url.length() == 0) return START_NOT_STICKY;
            if (TASKS.containsKey(appId)) return START_STICKY;

            final TaskInfo t = new TaskInfo();
            t.appId = appId;
            t.appName = appName == null ? "" : appName;
            t.icon = icon == null ? "" : icon;
            t.fileName = fileName == null ? ("konbini_" + appId + ".apk") : fileName;

            TASKS.put(appId, t);
            persistTasks();
            sendStateBroadcast(t, false, false, false);

            new Thread(new Runnable() {
                public void run() {
                    runDownload(t, url, appPackage);
                }
            }).start();
        }

        return START_STICKY;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int getCompleteNotifIcon() {
        int id = getResources().getIdentifier("stat_sys_install_complete", "drawable", getPackageName());
        return id != 0 ? id : android.R.drawable.stat_sys_download_done;
    }

    private synchronized void persistTasks() {
        JSONArray arr = new JSONArray();
        try {
            for (Map.Entry<Integer, TaskInfo> e : TASKS.entrySet()) {
                TaskInfo t = e.getValue();
                JSONObject o = new JSONObject();
                o.put("app_id", t.appId);
                o.put("app_name", t.appName);
                o.put("icon", t.icon);
                o.put("percent", t.percent);
                o.put("speed_bps", t.speed);
                o.put("installing", t.installing);
                o.put("status_text", t.statusText);
                o.put("file_name", t.fileName);
                o.put("file_path", t.filePath);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        SharedPreferences.Editor fdjfk = prefs().edit().putString("tasks_json", arr.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            fdjfk.apply();
            return;
        }
        fdjfk.commit();
    }

    private void sendStateBroadcast(TaskInfo t, boolean done, boolean error, boolean cancelled) {
        Intent p = new Intent(ACTION_PROGRESS);
        p.putExtra("app_id", t.appId);
        p.putExtra("percent", t.percent);
        p.putExtra("speed_bps", t.speed);
        p.putExtra("done", done);
        p.putExtra("error", error);
        p.putExtra("error_message", t.errorMessage);
        p.putExtra("cancelled", cancelled);
        p.putExtra("active", !(done || error || cancelled));
        p.putExtra("installing", t.installing);
        p.putExtra("app_name", t.appName);
        p.putExtra("status_text", t.statusText);
        p.putExtra("icon", t.icon);
        if (t.filePath != null && t.filePath.length() > 0) {
            p.putExtra("file_path", t.filePath);
        }
        sendBroadcast(p);
    }

    private File outFile(String fileName) {
        File dir = null;
        if (Build.VERSION.SDK_INT >= 8) {
            dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        if (dir == null) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                dir = new File(Environment.getExternalStorageDirectory(), "Konbini");
            } else {
                dir = getCacheDir();
            }
        }
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }

    private static int pendingIntentFlags(int baseFlags) {
        if (Build.VERSION.SDK_INT >= 23) {
            return baseFlags | PendingIntent.FLAG_IMMUTABLE;
        }
        return baseFlags;
    }

    private PendingIntent progressPendingIntent(int appId) {
        Intent open = new Intent(this, AppDetailActivity.class);
        open.putExtra("app_id", appId);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, appId, open, pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private PendingIntent completePendingIntent(String filePath, int appId) {
        File file = new File(filePath);
        Uri apkUri = getApkUri(this, file);
        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setDataAndType(apkUri, "application/vnd.android.package-archive");
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= 24) {
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(open, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName != null) {
                    grantUriPermission(resolveInfo.activityInfo.packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }

        return PendingIntent.getActivity(this, 10000 + appId, open, pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private Notification makeProgressNotification(TaskInfo t) {
        Notification n = new Notification(android.R.drawable.stat_sys_download, null, System.currentTimeMillis());
        n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
        n.contentIntent = progressPendingIntent(t.appId);

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_download);
        rv.setTextViewText(R.id.notifTitle,
                t.appName == null || t.appName.length() == 0 ? getString(R.string.downloading) : t.appName);
        rv.setTextViewText(R.id.notifText, t.statusText);
        rv.setProgressBar(R.id.notifProgress, 100, Math.max(0, Math.min(100, t.percent)), t.installing);
        n.contentView = rv;
        return n;
    }

    private Notification makeSimpleNotification(String title, String text, PendingIntent pi, int iconRes) {
        Notification n = new Notification(iconRes, null, System.currentTimeMillis());
        n.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        n.contentIntent = pi;

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_simple);
        rv.setTextViewText(R.id.notifTitle, title);
        rv.setTextViewText(R.id.notifText, text);
        n.contentView = rv;
        return n;
    }

    private void compatStartForeground(int id, Notification notification) {
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
                return;
            } catch (Exception e) {
            }
        }

        if (mSetForeground != null) {
            try {
                mSetForegroundArgs[0] = Boolean.TRUE;
                mSetForeground.invoke(this, mSetForegroundArgs);
            } catch (Exception e) {
            }
        }

        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private void compatStopForeground(int id) {
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
                return;
            } catch (Exception e) {
            }
        }

        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(id);

        if (mSetForeground != null) {
            try {
                mSetForegroundArgs[0] = Boolean.FALSE;
                mSetForeground.invoke(this, mSetForegroundArgs);
            } catch (Exception e) {
            }
        }
    }

    private void notifyProgress(TaskInfo t) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = makeProgressNotification(t);
        nm.notify(1000 + t.appId, n);
        compatStartForeground(1000 + t.appId, n);
    }

    private void notifyComplete(TaskInfo t) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(
                1000 + t.appId,
                makeSimpleNotification(
                        getString(R.string.download_complete),
                        getString(R.string.download_complete),
                        completePendingIntent(t.filePath, t.appId),
                        getCompleteNotifIcon()
                )
        );
    }

    private void notifyInstalled(TaskInfo t) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String text = (t.appName == null || t.appName.length() == 0)
                ? getString(R.string.app_installed)
                : getString(R.string.app_installed_named, t.appName);

        nm.notify(
                1000 + t.appId,
                makeSimpleNotification(
                        getString(R.string.app_installed),
                        text,
                        progressPendingIntent(t.appId),
                        getCompleteNotifIcon()
                )
        );
    }

    public static Uri getApkUri(Context context, File file) {
        return (Build.VERSION.SDK_INT >= 24) ?
                Uri.parse("content://" + APKFileProvider.AUTHORITY + file.getAbsolutePath()) :
                Uri.fromFile(file);
    }

    public static void installApk(Context context, File file) {
        Uri apkUri = getApkUri(context, file);
        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setDataAndType(apkUri, "application/vnd.android.package-archive");
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= 24) {
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(open, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName != null) {
                    context.grantUriPermission(resolveInfo.activityInfo.packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }

        context.startActivity(open);
    }

    private void launchPackageInstaller(String filePath) {
        try {
            installApk(this, new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runDownload(TaskInfo t, String urlStr, String appPackage) {
        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream out = null;
        File f = outFile(t.fileName);

        long lastNotifyTime = 0L;
        int lastNotifyPercent = -1;
        int lastBroadcastPercent = -1;

        try {
            notifyProgress(t);

            URL url = new URL(urlStr);
            int redirectCount = 0;
            while (true) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(30000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                        || responseCode == 307 || responseCode == 308) {
                    if (redirectCount >= 10) throw new IOException("Too many redirects");
                    String location = conn.getHeaderField("Location");
                    if (location == null || location.trim().length() == 0) {
                        throw new IOException("Redirect response missing Location header");
                    }
                    URL redirectUrl = new URL(url, location);
                        Log.i(TAG, "Following HTTP " + responseCode + " redirect to "
                            + redirectUrl.getProtocol() + "://" + redirectUrl.getHost());
                    conn.disconnect();
                    conn = null;
                    url = redirectUrl;
                    redirectCount++;
                    continue;
                }
                if (responseCode < 200 || responseCode >= 300) {
                    throw new HttpStatusException(responseCode);
                }
                break;
            }

            int len = conn.getContentLength();
            in = conn.getInputStream();
            out = new FileOutputStream(f);

            byte[] buf = new byte[8192];
            long total = 0;
            long speedWindowStartTime = System.currentTimeMillis();
            long speedWindowStartBytes = 0;

            int r;
            while ((r = in.read(buf)) != -1) {
                if (t.cancel) throw new RuntimeException("cancel");

                out.write(buf, 0, r);
                total += r;

                int percent = (len > 0) ? (int) (total * 100 / len) : 0;
                if (percent < t.percent) percent = t.percent;
                else t.percent = percent;

                long now = System.currentTimeMillis();
                long dt = now - speedWindowStartTime;
                long db = total - speedWindowStartBytes;
                if (dt > 0) t.speed = (db * 1000L) / dt;

                String speedText = (t.speed >= 1024 * 1024)
                        ? String.format(Locale.ENGLISH, "%.1f MB/s", (t.speed / 1024f / 1024f))
                        : Math.max(1, (t.speed / 1024)) + " KB/s";
                t.statusText = t.percent + "%  -  " + speedText;

                if (t.percent >= 100 || ((now - lastNotifyTime) >= 2500 && t.percent >= lastNotifyPercent + 5)) {
                    lastNotifyTime = now;
                    lastNotifyPercent = t.percent;
                    notifyProgress(t);
                }

                if (t.percent >= 100 || t.percent >= lastBroadcastPercent + 1) {
                    lastBroadcastPercent = t.percent;
                    persistTasks();
                    sendStateBroadcast(t, false, false, false);
                }

                if (dt >= 1000) {
                    speedWindowStartTime = now;
                    speedWindowStartBytes = total;
                }
            }

            t.filePath = f.getAbsolutePath();
            logDownloadIfAllowed(t, appPackage);

            if (Prefs.isAutoInstallRoot(this)) {
                t.installing = true;
                t.statusText = getString(R.string.installing);
                persistTasks();
                notifyProgress(t);
                sendStateBroadcast(t, false, false, false);

                boolean installed = installSilently(t.filePath);
                TASKS.remove(t.appId);
                persistTasks();

                if (installed) {
                    t.filePath = "";
                    notifyInstalled(t);
                    sendStateBroadcast(t, true, false, false);
                } else {
                    notifyComplete(t);
                    launchPackageInstaller(t.filePath);
                    sendStateBroadcast(t, true, false, false);
                }
            } else {
                TASKS.remove(t.appId);
                persistTasks();
                notifyComplete(t);
                launchPackageInstaller(t.filePath);
                sendStateBroadcast(t, true, false, false);
            }
        } catch (Exception e) {
            if (t.cancel) {
                Log.i(TAG, "Download cancelled for app " + t.appId, e);
            } else {
                Log.e(TAG, "Download failed for app " + t.appId, e);
                t.errorMessage = getDownloadErrorMessage(e);
            }
            TASKS.remove(t.appId);
            persistTasks();
            try {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(1000 + t.appId);
            } catch (Exception ex) {
            }
            sendStateBroadcast(t, false, !t.cancel, t.cancel);
        } finally {
            try { if (out != null) out.close(); } catch (Exception e) { }
            try { if (in != null) in.close(); } catch (Exception e) { }
            try { if (conn != null) conn.disconnect(); } catch (Exception e) { }
            try { compatStopForeground(1000 + t.appId); } catch (Exception e) { }
            stopSelf();
        }
    }

    private String getDownloadErrorMessage(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpStatusException) {
                int statusCode = ((HttpStatusException) cause).statusCode;
                String statusClass = statusCode >= 400 && statusCode < 500 ? "4xx"
                        : statusCode >= 500 && statusCode < 600 ? "5xx" : "HTTP";
                return "HTTP error " + statusCode + " (" + statusClass + ")";
            }
            if (cause instanceof SSLException) return "SSL failure";
            if (cause instanceof UnknownHostException) return "DNS error: host not found";
            if (cause instanceof SocketTimeoutException) return "Connection timeout";
            if (cause instanceof ConnectException) return "Connection error: " + cause.getMessage();
        }

        String message = error.getMessage();
        return message == null || message.length() == 0 ? "Unknown download error" : message;
    }

    private void logDownloadIfAllowed(final TaskInfo task, final String appPackage) {
        if (appPackage == null || appPackage.length() == 0
                || !Boolean.TRUE.equals(Prefs.getAnalyticsConsent(this))) return;

        final Context appContext = getApplicationContext();
        new Thread(() -> {
            if (!Boolean.TRUE.equals(Prefs.getAnalyticsConsent(appContext))) return;
            try {
                JSONObject payload = new JSONObject();
                payload.put("app_package", appPackage);
                payload.put("package_id", task.appId);
                payload.put("file", task.fileName);
                payload.put("device_manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
                payload.put("device_model", Build.MODEL == null ? "" : Build.MODEL);
                payload.put("device_api", Build.VERSION.SDK_INT);
                payload.put("device_locale", Locale.getDefault().toString());

                String server = Prefs.getSocialServer(appContext);
                while (server.endsWith("/")) server = server.substring(0, server.length() - 1);
                Http.postJson(appContext, server + "/api/download", payload.toString());
            } catch (Exception e) {
                Log.w(TAG, "Failed to log download", e);
            }
        }).start();
    }

    private boolean installSilently(String apkPath) {
        Process p = null;
        BufferedReader reader = null;
        BufferedReader errReader = null;
        try {
            String safePath = apkPath.replace("'", "'\\''");
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm install -r '" + safePath + "'"});

            StringBuilder output = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder errorOutput = new StringBuilder();
            errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int rc = p.waitFor();
            String outStr = output.toString().trim();
            String errStr = errorOutput.toString().trim();

            if (rc != 0 || (outStr.toLowerCase().contains("failure") || errStr.toLowerCase().contains("failure"))) {
                Log.e("installSilently@DlServ", "Failed to install app silently, exit code: " + rc +
                        ", stdout: " + outStr + ", stderr: " + errStr);
                return false;
            } else {
                Log.i("installSilently@DlServ", "Installed app silently successfully, stdout: " + outStr);
                return true;
            }
        } catch (Exception e) {
            Log.e("installSilently@DlServ", "Failed to install app silently: ", e);
            return false;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception e) {
                Log.e("installSilently@DlServ", "Failed to close BufferedReader: ", e);
            }
            try { if (errReader != null) errReader.close(); } catch (Exception e) {
                Log.e("installSilently@DlServ", "Failed to close BufferedReader for errors: ", e);
            }
            try { if (p != null) p.destroy(); } catch (Exception e) {
                Log.e("installSilently@DlServ", "Failed to destroy Process: ", e);
            }
        }
    }
}