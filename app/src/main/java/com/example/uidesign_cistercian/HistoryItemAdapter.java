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
    private Set<Integer> selectedItems = new HashSet<>(); // Track selected items
    private Set<Integer> selectedPositions = new HashSet<>(); // Track selected positions


    public HistoryItemAdapter(@NonNull Context context, @NonNull List<Integer> objects) {
        super(context, 0, objects);
        this.context = context;
        this.historyItems = objects;
    }

    // Method to toggle selection state
    public void toggleItemSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged();
        }
    }

    // Method to get all selected positions
    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(selectedPositions);
    }

    // Method to clear selections
    public void clearSelections() {
        selectedPositions.clear();
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

        Integer item = getItem(position); // Get the item at the current position
        tvArabicNumber.setText(String.valueOf(item));

        // Set the number on the CistercianThumbnailView
        ivThumbnail.setNumber(item);

        // Highlight based on position selection
        convertView.setBackgroundColor(selectedPositions.contains(position) ? Color.LTGRAY : Color.TRANSPARENT);

        return convertView;
    }

    public interface OnItemSelectionChangedListener {
        void onSelectionChanged();
    }

    private OnItemSelectionChangedListener selectionChangedListener;

    public void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }
}