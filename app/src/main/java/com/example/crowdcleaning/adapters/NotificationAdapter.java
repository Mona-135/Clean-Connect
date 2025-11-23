package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.crowdcleaning.R;
import com.example.crowdcleaning.models.NotificationModel;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        if (notification != null) {
            holder.textTitle.setText(notification.getTitle());
            holder.textMessage.setText(notification.getMessage());
            holder.textAudience.setText("To: " + notification.getAudience());
            holder.textTimestamp.setText(notification.getFormattedTimestamp());
            holder.textSentBy.setText("By: " + notification.getSentBy());
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public void updateList(List<NotificationModel> newList) {
        if (newList != null) {
            notificationList.clear();
            notificationList.addAll(newList);
            notifyDataSetChanged();
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textMessage, textAudience, textTimestamp, textSentBy;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMessage = itemView.findViewById(R.id.textMessage);
            textAudience = itemView.findViewById(R.id.textAudience);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textSentBy = itemView.findViewById(R.id.textSentBy);
        }
    }
}