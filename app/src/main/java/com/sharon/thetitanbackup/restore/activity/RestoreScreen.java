package com.sharon.thetitanbackup.restore.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.sharon.thetitanbackup.AppProperties;
import com.sharon.thetitanbackup.R;
import com.sharon.thetitanbackup.helper.Compressor;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

public class RestoreScreen extends AppCompatActivity {

    public static int NOTIFICATION_ID = 111;
    AppProperties importedApp;
    String zippath, condition;
    NotificationManager mNotificationManager;
    Notification.Builder builder;
    TextView notInstalledText, notInstalledTextHeading, progressOperationText;
    int incr = 0;
    Button stopButton;
    int x = 0;
    ArcProgress arcProgress;
    RestorationTask restorationTask = new RestorationTask();
    String pathToScan = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Titan Backup/";

    //NativeExpressAdView nativeExpressAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_installation);

//        prefManager = new PrefManager(this);
//        boolean isPremium = prefManager.getPremiumInfo("premium");
//        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.nativeAdViewInstaller);
//        if (!isPremium) {
//            AdRequest request = new AdRequest.Builder().addTestDevice("A3097AD8C3C34E010D834944ED9D0291").build();
//            nativeExpressAdView.loadAd(request);
//        }

        setTitle(getString(R.string.restore_title));
        arcProgress = (ArcProgress) findViewById(R.id.arc_progress);
        stopButton = (Button) findViewById(R.id.stopButton);
        notInstalledText = (TextView) findViewById(R.id.not_installed_backedup_apps_list);
        notInstalledTextHeading = (TextView) findViewById(R.id.not_installed_backedup_apps_list_heading);
        progressOperationText = (TextView) findViewById(R.id.progress_operation_text);
        importedApp = (AppProperties) getIntent().getSerializableExtra("app");
        condition = getIntent().getStringExtra("condition");

        restorationTask.execute();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(RestoreScreen.this)
                        .setTitle(getString(R.string.backup_stop_button_error))
                        .setMessage(R.string.restore_stop_button_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                restorationTask.cancel(true);
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
//
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, RestoreScreen.class), PendingIntent.FLAG_UPDATE_CURRENT);
//        builder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void installApkProcess(final AppProperties app) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                x = x + (100 / 100);
                arcProgress.setProgress(x);
                progressOperationText.setText(getString(R.string.notification_restoring) + incr + "/" + "1" + "\t" + app.getAppname());
            }
        });
        String apkPath = pathToScan + app.getAppname() + ".apk";
        String zippath = pathToScan + app.getPname() + ".zip";
        //String extractionpath = "/data/data/";
        String extractionpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Alarms/bla/";
        File apkfile = new File(apkPath);

        if (new File(zippath).exists() && condition.equals("data")) {
            if (Shell.SU.available()) {
                //Shell.SU.run("mount -o rw,remount /data");
                //Shell.SU.run("chmod -R 777 /data/data");
                String s = "copy "+apkPath+" "+extractionpath;
                Shell.SU.run(s);
                Log.d("installApkProcess: ", apkPath + "\n" + extractionpath + "\n" + zippath+"\n"+s);
               // boolean success = Compressor.unzip(zippath, extractionpath, "");
                //Shell.SU.run("chmod -R u+rwX,g+rwX,o-rw " + extractionpath);
//                if (success) {
//                    Log.d("installApkProcess: ", "data restored");
//                }else{
//                    Log.d("installApkProcess: ","not success");
//                    restorationTask.cancel(true);
//                }

            }
        }
//        if (apkfile.exists()) {
//            try {
//                if (Shell.SU.available()) {
//                    String command = "pm install " + apkPath;
//                    Shell.SU.run(command);
//                } else {
//                    Uri uri;
//                    Intent promptInstall = new Intent(Intent.ACTION_VIEW);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        promptInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkfile);
//                    } else {
//                        uri = Uri.fromFile(apkfile);
//                    }
//                    promptInstall.setDataAndType(uri,
//                            "application/vnd.android.package-archive");
//                    startActivity(promptInstall);
//                }
//                Log.d("installApkProcess: ","apk restored");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    private class RestorationTask extends AsyncTask {

        @Override
        protected Void doInBackground(Object[] params) {
            startNotification(getString(R.string.app_name), getString(R.string.notification_restore_default));
            while (!isCancelled()) {
                incr += 1;
                builder.setProgress(1, incr, false);
                builder.setContentTitle(getString(R.string.notification_restoring));
                builder.setContentText(incr + "/" + "1" + "\t" + importedApp.getAppname());
                builder.setOngoing(true);
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher));
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                }
                installApkProcess(importedApp);
                break;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressOperationText.setVisibility(View.GONE);
            builder.setContentText(getString(R.string.success_restore))
                    .setContentTitle(getString(R.string.app_name))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            arcProgress.setProgress(100);
            stopButton.setText(R.string.notification_restore_default);
            stopButton.setActivated(false);
            stopButton.setEnabled(false);
            stopButton.setBackground(ContextCompat.getDrawable(RestoreScreen.this, R.drawable.stop_button_shape_success));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            stopButton.setBackground(ContextCompat.getDrawable(RestoreScreen.this, R.drawable.stop_button_shape_cancel));
            stopButton.setText(R.string.cancelled);
            stopButton.setActivated(false);
            stopButton.setEnabled(false);
            progressOperationText.setVisibility(View.GONE);
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

