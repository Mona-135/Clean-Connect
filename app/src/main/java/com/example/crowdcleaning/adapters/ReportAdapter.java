package com.example.crowdcleaning.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.models.ReportModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<ReportModel> reportList;
    private String userType;
    private String reportType;
    private OnReportActionListener actionListener;

    public interface OnReportActionListener {
        void onAcceptTask(ReportModel report);
        void onViewDetails(ReportModel report);
        void onMarkComplete(ReportModel report);
        void onAdminAction(ReportModel report);
    }

    public ReportAdapter(List<ReportModel> reportList, String userType) {
        this.reportList = reportList;
        this.userType = userType;
        this.reportType = "available";
    }

    public void setOnReportActionListener(OnReportActionListener listener) {
        this.actionListener = listener;
    }

    public void setReportType(String type) {
        this.reportType = type;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (reportList == null || position >= reportList.size()) return;

        ReportModel report = reportList.get(position);

        holder.textTitle.setText(report.getTitle() != null ? report.getTitle() : "No Title");
        holder.textDescription.setText(report.getDescription() != null ? report.getDescription() : "No Description");
        holder.textAddress.setText(report.getAddress() != null ? report.getAddress() : "No Address");
        holder.textStatus.setText("Status: " + formatStatusForDisplay(report.getStatus()));

        // Format timestamp safely
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String timestampStr = sdf.format(report.getTimestamp());
            holder.textTimestamp.setText(timestampStr);
        } catch (Exception e) {
            holder.textTimestamp.setText("Unknown date");
        }

        // Show volunteer name if assigned
        if (report.getVolunteerName() != null && !report.getVolunteerName().isEmpty()) {
            holder.textVolunteer.setVisibility(View.VISIBLE);
            holder.textVolunteer.setText("Assigned to: " + report.getVolunteerName());
        } else {
            holder.textVolunteer.setVisibility(View.GONE);
        }

        // Set status color
        setStatusColor(holder.textStatus, report.getStatus());

        // Load image using improved method
        loadReportImage(holder.imageViewReport, report);

        // Setup action buttons
        setupActionButtons(holder, report);
    }

    private void loadReportImage(ImageView imageView, ReportModel report) {
        String imageUrl = getImageUrl(report);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .resize(300, 300)
                    .centerCrop()
                    .onlyScaleDown() // Only scale down, never up
                    .networkPolicy(NetworkPolicy.OFFLINE) // Try cache first
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            // Image loaded from cache
                        }

                        @Override
                        public void onError(Exception e) {
                            // Try loading from network if cache fails
                            Picasso.get()
                                    .load(imageUrl)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .resize(300, 300)
                                    .centerCrop()
                                    .into(imageView);
                        }
                    });
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private String getImageUrl(ReportModel report) {
        // First try to get from imageUrls list
        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            return report.getImageUrls().get(0);
        }
        // Fallback to single imageUrl
        return report.getImageUrl();
    }

    private String formatStatusForDisplay(String status) {
        if (status == null) return "REPORTED";
        switch (status.toLowerCase()) {
            case "in_progress": return "IN PROGRESS";
            case "cleaned": return "COMPLETED";
            case "completed": return "COMPLETED";
            case "reported": return "AVAILABLE";
            case "assigned": return "ASSIGNED";
            case "pending": return "PENDING";
            default: return status.toUpperCase();
        }
    }

    private void setStatusColor(TextView statusView, String status) {
        int color;
        if (status == null) {
            color = 0xFFFF9800; // Orange for null status
        } else {
            switch (status.toLowerCase()) {
                case "reported":
                case "pending":
                    color = 0xFFFF9800; // Orange
                    break;
                case "in_progress":
                case "assigned":
                    color = 0xFF2196F3; // Blue
                    break;
                case "cleaned":
                case "completed":
                    color = 0xFF4CAF50; // Green
                    break;
                default:
                    color = 0xFF666666; // Gray
                    break;
            }
        }
        statusView.setTextColor(color);
    }

    private void setupActionButtons(ViewHolder holder, ReportModel report) {
        // Reset all buttons
        holder.buttonPrimary.setVisibility(View.GONE);
        holder.buttonSecondary.setVisibility(View.GONE);

        if ("volunteer".equals(userType)) {
            setupVolunteerActions(holder, report);
        } else if ("admin".equals(userType)) {
            setupAdminActions(holder, report);
        } else {
            setupCitizenActions(holder, report);
        }
    }

    private void setupVolunteerActions(ViewHolder holder, ReportModel report) {
        if ("available".equals(reportType)) {
            // Available reports - show Accept button
            holder.buttonPrimary.setVisibility(View.VISIBLE);
            holder.buttonPrimary.setText("Accept Task");
            holder.buttonPrimary.setBackgroundColor(0xFF4CAF50);
            holder.buttonPrimary.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAcceptTask(report);
            });

            holder.buttonSecondary.setVisibility(View.VISIBLE);
            holder.buttonSecondary.setText("View Details");
            holder.buttonSecondary.setBackgroundColor(0xFF2196F3);
            holder.buttonSecondary.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onViewDetails(report);
            });

        } else if ("myTasks".equals(reportType)) {
            // My tasks - show appropriate buttons based on status
            String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "";

            if ("assigned".equals(status) || "in_progress".equals(status)) {
                holder.buttonPrimary.setVisibility(View.VISIBLE);
                holder.buttonPrimary.setText("Mark Complete");
                holder.buttonPrimary.setBackgroundColor(0xFF4CAF50);
                holder.buttonPrimary.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onMarkComplete(report);
                });
            }

            holder.buttonSecondary.setVisibility(View.VISIBLE);
            holder.buttonSecondary.setText("View Details");
            holder.buttonSecondary.setBackgroundColor(0xFF2196F3);
            holder.buttonSecondary.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onViewDetails(report);
            });
        }
    }

    private void setupAdminActions(ViewHolder holder, ReportModel report) {
        holder.buttonPrimary.setVisibility(View.VISIBLE);
        holder.buttonPrimary.setText("Manage");
        holder.buttonPrimary.setBackgroundColor(0xFF9C27B0);
        holder.buttonPrimary.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAdminAction(report);
        });
    }

    private void setupCitizenActions(ViewHolder holder, ReportModel report) {
        holder.buttonPrimary.setVisibility(View.VISIBLE);
        holder.buttonPrimary.setText("View Details");
        holder.buttonPrimary.setBackgroundColor(0xFF2196F3);
        holder.buttonPrimary.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewDetails(report);
        });
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    public void updateData(List<ReportModel> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription, textAddress, textStatus, textTimestamp, textVolunteer;
        Button buttonPrimary, buttonSecondary;
        ImageView imageViewReport; // ImageView for report images

        public ViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            textAddress = itemView.findViewById(R.id.textAddress);
            textStatus = itemView.findViewById(R.id.textStatus);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textVolunteer = itemView.findViewById(R.id.textVolunteer);
            buttonPrimary = itemView.findViewById(R.id.buttonPrimary);
            buttonSecondary = itemView.findViewById(R.id.buttonSecondary);
            imageViewReport = itemView.findViewById(R.id.imageViewReport);
        }
    }
}