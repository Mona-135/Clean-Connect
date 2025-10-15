package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.models.NotificationModel;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        holder.textTitle.setText(notification.getTitle());
        holder.textMessage.setText(notification.getMessage());
        holder.textAudience.setText("To: " + notification.getAudience());
        holder.textTimestamp.setText(notification.getTimestamp());

        // Only set sentBy if the TextView exists
        if (holder.textSentBy != null) {
            holder.textSentBy.setText("By: " + notification.getSentBy());
        }

        // Set audience color
        switch (notification.getAudience()) {
            case "Volunteers Only":
                holder.textAudience.setTextColor(0xFFFF9800); // Orange
                break;
            case "Citizens Only":
                holder.textAudience.setTextColor(0xFF2196F3); // Blue
                break;
            default:
                holder.textAudience.setTextColor(0xFF4CAF50); // Green for All Users
                break;
        }
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textMessage, textAudience, textTimestamp, textSentBy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMessage = itemView.findViewById(R.id.textMessage);
            textAudience = itemView.findViewById(R.id.textAudience);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);

            // Safe initialization - will be null if view doesn't exist
            textSentBy = itemView.findViewById(R.id.textSentBy);
        }
    }

    public void updateData(List<NotificationModel> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }
}