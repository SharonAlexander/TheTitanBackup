package com.sharon.thetitanbackup.backup.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sharon.thetitanbackup.R;

import java.util.ArrayList;
import java.util.List;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder> {

    private BackupAdapterListener listener;
    private Context mContext;
    private List<ApplicationInfo> appList = new ArrayList<>();
    private List<String> backedupAppList = new ArrayList<>();
    private List<String> backedupDataList = new ArrayList<>();
    private PackageManager packageManager;

    public BackupAdapter(Context mContext, List<ApplicationInfo> appList, ArrayList<String> backedupAppList, ArrayList<String> backedupDataList, BackupAdapterListener listener) {
        this.listener = listener;
        this.mContext = mContext;
        this.appList = appList;
        this.packageManager = mContext.getPackageManager();
        this.backedupAppList = backedupAppList;
        this.backedupDataList = backedupDataList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_view_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        ApplicationInfo app = appList.get(position);
        holder.appname.setText(app.loadLabel(packageManager));
        PackageInfo pi = packageManager.getPackageArchiveInfo(app.publicSourceDir, 0);
        holder.version.setText(pi.versionName);
        holder.backedup.setText("No Backup");
        holder.backedup.setTextColor(Color.RED);
        if (backedupDataList.contains(app.packageName) && backedupAppList.contains(app.loadLabel(packageManager))) {
            holder.backedup.setText("App + Data backup found");
            holder.backedup.setTextColor(Color.GREEN);
        } else if (!backedupDataList.contains(app.packageName) && backedupAppList.contains(app.loadLabel(packageManager))) {
            holder.backedup.setText("App backup found");
            holder.backedup.setTextColor(Color.BLUE);
        }
        holder.icon.setImageDrawable(app.loadIcon(packageManager));
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMessageRowClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }


    public interface BackupAdapterListener {
        void onIconClicked(int position);

        void onIconImportantClicked(int position);

        void onMessageRowClicked(int position);

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView appname, backedup, version;
        ImageView icon;
        RelativeLayout relativeLayout;

        private MyViewHolder(View view) {
            super(view);
            appname = (TextView) view.findViewById(R.id.installer_backup_appname);
            backedup = (TextView) view.findViewById(R.id.backup_info);
            version = (TextView) view.findViewById(R.id.version);
            icon = (ImageView) view.findViewById(R.id.installer_backup_appicon);
            relativeLayout = (RelativeLayout) view.findViewById(R.id.installer_backup_list_row_rlayout);
        }
    }
}
