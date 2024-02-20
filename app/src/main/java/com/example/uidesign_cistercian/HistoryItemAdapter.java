package com.example.uidesign_cistercian;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.opencv.core.Rect;

import java.util.List;

public class HistoryItemAdapter extends ArrayAdapter<Integer> {
    private Context context;
    private List<Integer> historyItems;
    // Track selected items
    private Set<Integer> selectedItems = new HashSet<>();

    public HistoryItemAdapter(@NonNull Context context, @NonNull List<Integer> objects) {
        super(context, 0, objects);
        this.context = context;
        this.historyItems = objects;
    }

    // Method to toggle selection state
    public void toggleItemSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyDataSetChanged();
    }

    // Method to clear selections
    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // Get all selected items
    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
        }

        TextView tvArabicNumber = convertView.findViewById(R.id.tvArabicNumber);
        CistercianThumbnailView ivThumbnail = convertView.findViewById(R.id.ivThumbnail);

        Integer item = historyItems.get(position);
        tvArabicNumber.setText(String.valueOf(item));

        // Set the number on the CistercianThumbnailView
        ivThumbnail.setNumber(item);

        // Highlight selected items
        if (selectedItems.contains(position)) {
            convertView.setBackgroundColor(Color.LTGRAY); // Example highlight
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }
}