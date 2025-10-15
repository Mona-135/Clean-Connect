package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.models.RecentActivity;
import com.example.crowdcleaning.R; // Add this import

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<RecentActivity> activityList;

    public RecentActivityAdapter(List<RecentActivity> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (activityList == null || activityList.isEmpty()) return;

        RecentActivity activity = activityList.get(position);

        // Set data to views
        holder.textUserName.setText(activity.getUserName() != null ? activity.getUserName() : "Unknown User");
        holder.textDescription.setText(activity.getDescription() != null ? activity.getDescription() : "No description");
        holder.textStatus.setText(activity.getStatus() != null ? activity.getStatus() : "Reported");
        holder.textTimestamp.setText(activity.getTimestamp() != null ? activity.getTimestamp() : "Just now");

        // Set status color
        int statusColor;
        String status = activity.getStatus();
        if (status != null) {
            switch (status) {
                case "Completed":
                case "Cleaned":
                    statusColor = android.R.color.holo_green_dark;
                    break;
                case "In Progress":
                case "Assigned":
                    statusColor = android.R.color.holo_orange_dark;
                    break;
                default:
                    statusColor = android.R.color.holo_red_dark;
                    break;
            }
        } else {
            statusColor = android.R.color.holo_red_dark;
        }

        holder.textStatus.setTextColor(holder.itemView.getContext().getResources().getColor(statusColor));
    }

    @Override
    public int getItemCount() {
        return activityList == null ? 0 : activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textDescription, textStatus, textTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textDescription = itemView.findViewById(R.id.textDescription);
            textStatus = itemView.findViewById(R.id.textStatus);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }
    }

    // Method to update data
    public void updateData(List<RecentActivity> newList) {
        this.activityList = newList;
        notifyDataSetChanged();
    }
}