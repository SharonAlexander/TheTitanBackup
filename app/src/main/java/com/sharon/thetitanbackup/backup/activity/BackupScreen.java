package com.sharon.thetitanbackup.backup.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.sharon.thetitanbackup.PrefManager;
import com.sharon.thetitanbackup.R;
import com.sharon.thetitanbackup.helper.Compressor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class BackupScreen extends AppCompatActivity {

    public static int NOTIFICATION_ID = 222;
    ApplicationInfo importedApp;
    String condition;
    NotificationManager mNotificationManager;
    Notification.Builder builder;
    Button stopButton;
    TextView notBackedupText, notBackedupTextHeading, progressOperationText;
    int incr = 0;
    int x = 0;
    ArcProgress arcProgress;
    BackupTask backupTask = new BackupTask();
    PrefManager prefManager;
    PackageManager pm;
    String zippath, apkpath;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_backup);

        prefManager = new PrefManager(this);
        pm = getApplicationContext().getPackageManager();
        //boolean isPremium = prefManager.getPremiumInfo("premium");
//        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.nativeAdViewInstaller);
//        if (!isPremium) {
//            AdRequest request = new AdRequest.Builder().addTestDevice("A3097AD8C3C34E010D834944ED9D0291").build();
//            nativeExpressAdView.loadAd(request);
//        }
        setTitle(R.string.backup_title);
        arcProgress = (ArcProgress) findViewById(R.id.arc_progress);
        stopButton = (Button) findViewById(R.id.stopButton);
        notBackedupText = (TextView) findViewById(R.id.not_installed_backedup_apps_list);
        notBackedupTextHeading = (TextView) findViewById(R.id.not_installed_backedup_apps_list_heading);
        progressOperationText = (TextView) findViewById(R.id.progress_operation_text);
        importedApp = getIntent().getParcelableExtra("app");
        condition = getIntent().getStringExtra("condition");


        backupTask.execute();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(BackupScreen.this)
                        .setTitle(R.string.backup_stop_button_error)
                        .setMessage(R.string.backup_stop_button_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                backupTask.cancel(true);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    private void startNotification(String contentTitle, String contentText) {

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                //.setAutoCancel(true)
                .setContentTitle(contentTitle)
                //.setContentText(contentText)
                .setPriority(Notification.PRIORITY_HIGH);

//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
//                new Intent(context, MainActivity.class), PendingIntent.FLAG_ONE_SHOT);

//        builder.setContentIntent(contentIntent);

        //Show the notification
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void backupAppProcess(final ApplicationInfo app) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                x = x + (100 / 1);
                arcProgress.setProgress(x);
                progressOperationText.setText(getString(R.string.notification_backing_up) + incr + "/" + "1" + "\t" + app.loadLabel(pm));
            }
        });
        String targetpath = "/data/data/" + app.packageName;
        zippath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Titan Backup/" + app.packageName + ".zip";
        apkpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Titan Backup/";

        Intent mainIntent;
        mainIntent = getPackageManager()
                .getLaunchIntentForPackage(app.packageName);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List pkgAppsList = getPackageManager()
                .queryIntentActivities(mainIntent, 0);
        for (Object object : pkgAppsList) {
            ResolveInfo info = (ResolveInfo) object;
            File f1 = new File(
                    info.activityInfo.applicationInfo.publicSourceDir);

            try {
                PackageInfo pi = null;
                try {
                    pi = getPackageManager().getPackageInfo(app.packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String file_name = (String) getPackageManager().getApplicationLabel(pi.applicationInfo);
                InputStream in = new FileInputStream(f1);
                OutputStream out = checkStoragePath(file_name, pi);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
            }
        }
        if (condition.equals("data"))
            if (Shell.SU.available()) {
                Shell.SU.run("chmod -R u+rwX,go+rwX,go+rwX " + targetpath);
                boolean success = Compressor.zip(targetpath, zippath, "");
                Shell.SU.run("chmod -R u+rwX,go-rwX " + targetpath);
            }
    }

    private OutputStream checkStoragePath(String file_name, PackageInfo pi) throws FileNotFoundException {
        OutputStream out = null;
        String path = apkpath;
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            File f2;
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED))
                f2 = new File(path);
            else
                f2 = getCacheDir();
            if (!f2.exists())
                f2.mkdirs();
            f2 = new File(f2.getPath() + "/" + file_name + ".apk");
            try {
                f2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = new FileOutputStream(f2);
        } else {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, prefManager.getTreeUri("treeuri"));
            int pos = path.indexOf("/");
            int n = 3;
            while (--n > 0 && pos != -1)
                pos = path.indexOf("/", pos + 1);
            path = path.substring(pos + 1);
//            path = path.replaceFirst("/storage/", "");
//            path = path.substring(path.indexOf("/") + 1);
            String[] folders = path.split("/");
            DocumentFile temp = pickedDir;
            for (String folder : folders) {
                temp = temp.findFile(folder);
                if (temp == null) {
                    temp.createDirectory(folder);
                    temp = temp.findFile(folder);
                }
            }
            pickedDir = temp;
            if (pickedDir.findFile(file_name + ".apk") != null) {
                pickedDir = pickedDir.findFile(file_name + ".apk");
                pickedDir.delete();
            }
            pickedDir = temp;
            pickedDir = pickedDir.createFile("application", file_name + ".apk");
            out = getContentResolver().openOutputStream(pickedDir.getUri());
        }
        return out;
    }

    private class BackupTask extends AsyncTask {

        @Override
        protected Void doInBackground(Object[] params) {
            startNotification(getString(R.string.app_name), getString(R.string.notification_backup_default));

            while (!isCancelled()) {
                incr += 1;
                builder.setContentTitle(getString(R.string.notification_backing_up));
                builder.setProgress(1, incr, false);
                builder.setContentText(incr + "/1" + "\t" + importedApp.loadLabel(pm));
                builder.setOngoing(true);
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                backupAppProcess(importedApp);
                break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressOperationText.setVisibility(View.GONE);
            {
                builder.setContentText(getString(R.string.success_backup))
                        .setContentTitle(getString(R.string.app_name))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                arcProgress.setProgress(100);
                stopButton.setText(R.string.success_backup);
                stopButton.setActivated(false);
                stopButton.setEnabled(false);
                stopButton.setBackground(ContextCompat.getDrawable(BackupScreen.this, R.drawable.stop_button_shape_success));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressOperationText.setVisibility(View.GONE);
            stopButton.setBackground(ContextCompat.getDrawable(BackupScreen.this, R.drawable.stop_button_shape_cancel));
            stopButton.setText(R.string.cancelled);
            stopButton.setActivated(false);
            stopButton.setEnabled(false);
            builder.setContentText(getString(R.string.task_cancelled))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    arcProgress.setProgress(0);
                }
            });
        }
    }
}
