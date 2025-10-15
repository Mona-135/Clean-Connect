package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.crowdcleaning.R;
import com.example.crowdcleaning.models.ReportModel;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<ReportModel> reportList;
    private String userType; // "volunteer" or "citizen"

    public ReportAdapter(List<ReportModel> reportList, String userType) {
        this.reportList = reportList;
        this.userType = userType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportModel report = reportList.get(position);

        holder.textTitle.setText(report.getTitle());
        holder.textDescription.setText(report.getDescription());
        holder.textAddress.setText(report.getAddress());
        holder.textStatus.setText("Status: " + report.getStatus().toUpperCase());

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.textTimestamp.setText(sdf.format(report.getTimestamp()));

        // Set status color
        switch (report.getStatus()) {
            case "reported":
                holder.textStatus.setTextColor(0xFFFF9800); // Orange
                break;
            case "in_progress":
                holder.textStatus.setTextColor(0xFF2196F3); // Blue
                break;
            case "cleaned":
                holder.textStatus.setTextColor(0xFF4CAF50); // Green
                break;
            default:
                holder.textStatus.setTextColor(0xFF666666); // Gray
        }

        // Show/hide buttons based on user type
        if ("volunteer".equals(userType)) {
            holder.buttonAction.setVisibility(View.VISIBLE);
            if ("reported".equals(report.getStatus())) {
                holder.buttonAction.setText("Accept Task");
                holder.buttonAction.setBackgroundColor(0xFF4CAF50); // Green
            } else {
                holder.buttonAction.setText("View Details");
                holder.buttonAction.setBackgroundColor(0xFF2196F3); // Blue
            }
        } else {
            holder.buttonAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription, textAddress, textStatus, textTimestamp;
        Button buttonAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            textAddress = itemView.findViewById(R.id.textAddress);
            textStatus = itemView.findViewById(R.id.textStatus);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            buttonAction = itemView.findViewById(R.id.buttonAction);
        }
    }
}