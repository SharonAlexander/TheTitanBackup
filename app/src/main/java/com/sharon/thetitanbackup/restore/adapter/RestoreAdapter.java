package com.sharon.thetitanbackup.restore.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sharon.thetitanbackup.AppProperties;
import com.sharon.thetitanbackup.R;

import java.util.ArrayList;
import java.util.List;

public class RestoreAdapter extends RecyclerView.Adapter<RestoreAdapter.MyViewHolder> {

    private InstallerAdapterListener listener;
    private Context mContext;
    private List<AppProperties> appList = new ArrayList<>();
    private List<String> backedupAppList = new ArrayList<>();
    private List<String> backedupDataList = new ArrayList<>();

    public RestoreAdapter(Context mContext, List<AppProperties> appList, ArrayList<String> backedupAppList, ArrayList<String> backedUpDataList, InstallerAdapterListener listener) {
        this.listener = listener;
        this.mContext = mContext;
        this.appList = appList;
        this.backedupAppList = backedupAppList;
        this.backedupDataList = backedUpDataList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_view_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        AppProperties app = appList.get(position);
        holder.appname.setText(app.getAppname());
        if (backedupDataList.contains(app.getPname()) && backedupAppList.contains(app.getAppname())) {
            holder.backup_info.setText("APP + DATA Found");
            holder.backup_info.setTextColor(Color.GREEN);
        }else if(!backedupDataList.contains(app.getPname()) && backedupAppList.contains(app.getAppname())){
            holder.backup_info.setText("APK Found");
            holder.backup_info.setTextColor(Color.BLUE);
        }else{
            holder.backup_info.setText("Error");
            holder.backup_info.setTextColor(Color.RED);
        }
        holder.version.setText(app.getVersionname());
        holder.icon.setImageDrawable(app.getIcon());
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

    public interface InstallerAdapterListener {
        void onIconClicked(int position);

        void onIconImportantClicked(int position);

        void onMessageRowClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView appname, backup_info, version;
        ImageView icon;
        RelativeLayout relativeLayout;

        private MyViewHolder(View view) {
            super(view);
            appname = (TextView) view.findViewById(R.id.installer_backup_appname);
            backup_info = (TextView) view.findViewById(R.id.backup_info);
            version = (TextView) view.findViewById(R.id.version);
            icon = (ImageView) view.findViewById(R.id.installer_backup_appicon);
            relativeLayout = (RelativeLayout) view.findViewById(R.id.installer_backup_list_row_rlayout);
        }


    }
}
