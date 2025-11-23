package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.crowdcleaning.R;
import com.example.crowdcleaning.models.RecentActivity;
import java.util.List;

// Remove the @NonNull import and use these alternatives:
import org.jetbrains.annotations.NotNull;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<RecentActivity> activityList;

    public RecentActivityAdapter(List<RecentActivity> activityList) {
        this.activityList = activityList;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        RecentActivity activity = activityList.get(position);
        holder.textUserName.setText(activity.getUserName());
        holder.textDescription.setText(activity.getDescription());
        holder.textStatus.setText(activity.getStatus());
        holder.textTimestamp.setText(activity.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textDescription, textStatus, textTimestamp;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textDescription = itemView.findViewById(R.id.textDescription);
            textStatus = itemView.findViewById(R.id.textStatus);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }
    }
}