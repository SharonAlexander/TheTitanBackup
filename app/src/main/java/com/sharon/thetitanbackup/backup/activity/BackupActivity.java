package com.sharon.thetitanbackup.backup.activity;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sharon.thetitanbackup.PrefManager;
import com.sharon.thetitanbackup.R;
import com.sharon.thetitanbackup.backup.adapter.BackupAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class BackupActivity extends Fragment implements BackupAdapter.BackupAdapterListener, EasyPermissions.PermissionCallbacks {

    private static final int WRITE_PERMISSION_CALLBACK_CONSTANT = 333;
    PrefManager preferenceManager;
    //boolean layoutChange = false;
    //    AdView mAdView;
//    InterstitialAd mInterstitialAd;
//    AdRequest bannerAdRequest;
//    boolean isPremium = false;
    int tryx = 0;
    private ArrayList<ApplicationInfo> appList;
    private ArrayList<String> backedupAppList;
    private ArrayList<String> backedupDataList;
    private RecyclerView recyclerView;
    private BackupAdapter mAdapter;
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private TextView info_to_refresh;
    private int loadScreen = 0;
    private PackageManager packageManager = null;
    private String zipSize;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        appList = new ArrayList<>();
        backedupAppList = new ArrayList<>();
        backedupDataList = new ArrayList<>();
        packageManager = getActivity().getPackageManager();
        preferenceManager = new PrefManager(getActivity());

        //MobileAds.initialize(getActivity(), getString(R.string.ads_app_id));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_backup, container, false);

//        isPremium = preferenceManager.getPremiumInfo("premium");
//        mAdView = (AdView) rootView.findViewById(R.id.adListViewbanner);
//        if (!isPremium) {
//            adsInitialise();
//            requestNewInterstitial();
//        } else {
//            mAdView.setVisibility(View.GONE);
//        }

        getActivity().setTitle(getString(R.string.backup_title));
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //getActivity().getMenuInflater().inflate(R.menu.menu_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIconClicked(int position) {

    }

    @Override
    public void onIconImportantClicked(int position) {

    }

    @Override
    public void onMessageRowClicked(int position) {

        showAppInfoDialog(position);
    }

    private void showAppInfoDialog(int position) {
        PackageInfo pInfo = null;
        final ApplicationInfo app = appList.get(position);
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(app.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(app.loadIcon(getActivity().getPackageManager()));
        String message = "<b>Version</b><br>" + pInfo.versionName + "<br><br><b>Package Name</b><br>" + app.packageName + "<br><br><b>Apk Size</b><br>" + (new File(app.publicSourceDir).length() / (1024 * 1024)) + "MB";
        builder.setTitle(app.loadLabel(getActivity().getPackageManager()))
                .setMessage(Html.fromHtml(message))
                .setPositiveButton("Backup APP+DATA", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setthings(app, "data");
                    }
                });
        builder.setNegativeButton("Backup APP Only", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setthings(app, "app");
            }
        });
        builder.create();
        builder.show();
    }

    private void setthings(ApplicationInfo app, String condition) {
        Intent intent = new Intent(getActivity(), BackupScreen.class);
        intent.putExtra("app", app);
        intent.putExtra("condition", condition);
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

    private ArrayList<ApplicationInfo> checkForLaunchIntent(
            List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<>();
        for (ApplicationInfo info : list) {
            try {
                if (null != packageManager
                        .getLaunchIntentForPackage(info.packageName)) {
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        applist.add(info);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return applist;
    }

    private class LoadApkFiles extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(Void... params) {
            appList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            getBackedUpList(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Titan Backup/");
            mAdapter = new BackupAdapter(getActivity(), appList, backedupAppList, backedupDataList, BackupActivity.this);
            Collections.sort(appList,
                    new ApplicationInfo.DisplayNameComparator(packageManager));

            return null;
        }

        private void getBackedUpList(String pathToScan) {
            File file = new File(pathToScan);
            File fileLists[] = file.listFiles();
            if (fileLists != null && fileLists.length > 0) {
                for (File filename : fileLists) {
                    if (filename.getName().endsWith(".apk")) {
                        PackageManager packageManager = getActivity().getPackageManager();
                        PackageInfo pi = packageManager.getPackageArchiveInfo(filename.getAbsolutePath(), 0);
                        if (pi == null)
                            continue;
                        pi.applicationInfo.sourceDir = filename.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = filename.getAbsolutePath();

                        backedupAppList.add((String) pi.applicationInfo.loadLabel(packageManager));
                    }
                    if (filename.getName().endsWith(".zip")) {
                        String newFilename = filename.getName().substring(0, filename.getName().lastIndexOf("."));
                        backedupDataList.add(newFilename);
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
            recyclerView.setAdapter(mAdapter);
            if (progressDialog.isShowing() && loadScreen == 0) {
                progressDialog.dismiss();
                loadScreen = 1;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (appList.isEmpty()) {
                info_to_refresh.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), R.string.swipe_down_to_refresh, Toast.LENGTH_SHORT).show();
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