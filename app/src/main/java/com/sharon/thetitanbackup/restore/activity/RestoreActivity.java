package com.sharon.thetitanbackup.restore.activity;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sharon.thetitanbackup.AppProperties;
import com.sharon.thetitanbackup.PrefManager;
import com.sharon.thetitanbackup.R;
import com.sharon.thetitanbackup.restore.adapter.RestoreAdapter;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class RestoreActivity extends Fragment implements RestoreAdapter.InstallerAdapterListener, EasyPermissions.PermissionCallbacks {

    private static final int WRITE_PERMISSION_CALLBACK_CONSTANT = 111;
    String pathToScan = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Titan Backup/";
    PrefManager preferenceManager;
    //boolean layoutChange = false;
    //    AdView mAdView;
//    InterstitialAd mInterstitialAd;
//    AdRequest bannerAdRequest;
    boolean isPremium = false;
    private ArrayList<AppProperties> appList;
    private ArrayList<String> backedupAppList;
    private ArrayList<String> backedUpDataList;
    private RecyclerView recyclerView;
    private RestoreAdapter mAdapter;
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private TextView info_to_refresh;
    private int loadScreen = 0;
    PackageManager packageManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        appList = new ArrayList<>();
        backedupAppList = new ArrayList<>();
        backedUpDataList = new ArrayList<>();
        packageManager = getActivity().getPackageManager();

        // MobileAds.initialize(getActivity(), getString(R.string.ads_app_id));

        preferenceManager = new PrefManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_restore, container, false);

        isPremium = preferenceManager.getPremiumInfo("premium");
//        mAdView = (AdView) rootView.findViewById(R.id.adListViewbanner);
//        if (!isPremium) {
//            adsInitialise();
//            requestNewInterstitial();
//        } else {
//            mAdView.setVisibility(View.GONE);
//        }

        getActivity().setTitle(getString(R.string.restore_title));
        recyclerView = (RecyclerView) rootView.findViewById(R.id.installer_recycler_view);
        swipeRefreshLayout = (WaveSwipeRefreshLayout) rootView.findViewById(R.id.installer_backup_swipe_refresh_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        info_to_refresh = (TextView) rootView.findViewById(R.id.info_to_refresh);
        getReadPermission();
        swipeRefreshLayout.setWaveColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getReadPermission();
            }
        });
        return rootView;
    }

    private ArrayList<AppProperties> getAllApks(String pathToScan) {
        File file = new File(pathToScan);
        File fileLists[] = file.listFiles();
        if (fileLists != null && fileLists.length > 0) {
            for (File filename : fileLists) {
                if (filename.isDirectory()) {
                    getAllApks(filename.getPath());
                } else {
                    if (filename.getName().endsWith(".apk")) {
                        PackageInfo pi = packageManager.getPackageArchiveInfo(filename.getAbsolutePath(), 0);
                        if (pi == null)
                            continue;
                        pi.applicationInfo.sourceDir = filename.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = filename.getAbsolutePath();

                        AppProperties app = new AppProperties();
                        app.setAppname((String) pi.applicationInfo.loadLabel(packageManager));
                        app.setPname(pi.packageName);
                        app.setApkpath(filename.getAbsolutePath());
                        app.setIcon(pi.applicationInfo.loadIcon(packageManager));
                        app.setApksize((filename.length() / (1024 * 1024)) + "MB");
                        app.setVersionname(pi.versionName);
                        if (null != packageManager.getLaunchIntentForPackage(pi.applicationInfo.packageName)) {
                            app.setAlready_installed(true);
                        } else {
                            app.setAlready_installed(false);
                        }
                        appList.add(app);
                    }
                }
            }
        }
        return appList;
    }

    @Override
    public void onIconClicked(int position) {

    }

    @Override
    public void onIconImportantClicked(int position) {

    }

    @Override
    public void onMessageRowClicked(int position) {
        final AppProperties app = appList.get(position);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(app.getIcon());
        String message = "<b>Version</b><br>" + app.getVersionname() + "<br><br><b>Apk Size</b><br>" + app.getApksize()+ "<br><br><b>Path</b><br>" + app.getApkpath();

        builder.setTitle(app.getAppname())
                .setMessage(Html.fromHtml(message));
        if (backedUpDataList.contains(app.getPname()) && backedupAppList.contains(app.getAppname())) {
            builder.setPositiveButton("Restore APP+DATA", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setthings(app, 1);
                }
            });
        }
        builder.setNegativeButton("Restore APP Only", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setthings(app, 2);
            }
        });
        builder.create();
        builder.show();
    }

    private void setthings(AppProperties app, int i) {
        Intent intent = new Intent(getActivity(), RestoreScreen.class);
        intent.putExtra("app", app);
        intent.putExtra("condition", ((i == 1) ? "data" : "app"));
        startActivity(intent);
    }

    @AfterPermissionGranted(WRITE_PERMISSION_CALLBACK_CONSTANT)
    private void getReadPermission() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            proceedAfterPermission();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_storage),
                    WRITE_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void proceedAfterPermission() {
        new LoadApkFiles().execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            Toast.makeText(getActivity(), R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private class LoadApkFiles extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(Void... params) {
            appList = getAllApks(pathToScan);
            getBackedUpList(pathToScan);
            return null;
        }

        private void getBackedUpList(String pathToScan) {
            File file = new File(pathToScan);
            File fileLists[] = file.listFiles();
            if (fileLists != null && fileLists.length > 0) {
                for (File filename : fileLists) {
                    if (filename.getName().endsWith(".apk")) {
                        PackageInfo pi = packageManager.getPackageArchiveInfo(filename.getAbsolutePath(), 0);
                        if (pi == null)
                            continue;
                        pi.applicationInfo.sourceDir = filename.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = filename.getAbsolutePath();

                        backedupAppList.add((String) pi.applicationInfo.loadLabel(packageManager));
                    }
                    if (filename.getName().endsWith(".zip")) {
                        try {
                            ZipFile zip = new ZipFile(filename);
                            if (zip.isValidZipFile()) {
                                String newFilename = filename.getName().substring(0, filename.getName().lastIndexOf("."));
                                backedUpDataList.add(newFilename);
                            }else{
                                filename.delete();
                            }
                        } catch (ZipException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            appList.clear();
            progressDialog = new ProgressDialog(getActivity());
            if (loadScreen == 0) {
                progressDialog.setMessage(getString(R.string.app_loader_process_dialog_message));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            Collections.sort(appList, new Comparator<AppProperties>() {
                public int compare(AppProperties v1, AppProperties v2) {
                    return v1.getAppname().toLowerCase().compareTo(v2.getAppname().toLowerCase());
                }
            });
            mAdapter = new RestoreAdapter(getActivity(), appList, backedupAppList, backedUpDataList, RestoreActivity.this);
            recyclerView.setAdapter(mAdapter);
            if (progressDialog.isShowing() && loadScreen == 0) {
                progressDialog.dismiss();
                loadScreen = 1;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (appList.isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.no_apps_found), Toast.LENGTH_LONG).show();
                info_to_refresh.setVisibility(View.VISIBLE);
            } else {
                //Toast.makeText(getActivity(), getString(R.string.swipe_down_to_refresh), Toast.LENGTH_SHORT).show();
                info_to_refresh.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.dismiss();
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}