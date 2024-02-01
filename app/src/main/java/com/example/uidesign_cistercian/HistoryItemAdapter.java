package com.example.uidesign_cistercian;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class HistoryItemAdapter extends ArrayAdapter<Integer> {
    private Context context;
    private List<Integer> historyItems;

    public HistoryItemAdapter(@NonNull Context context, @NonNull List<Integer> objects) {
        super(context, 0, objects);
        this.context = context;
        this.historyItems = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
        }

        TextView tvArabicNumber = convertView.findViewById(R.id.tvArabicNumber);
        ImageView ivThumbnail = convertView.findViewById(R.id.ivThumbnail);

        Integer item = historyItems.get(position);
        tvArabicNumber.setText(String.valueOf(item));

        // Assuming you have a method to generate a Bitmap from the number
        Bitmap thumbnail = generateThumbnailForNumber(item);
        ivThumbnail.setImageBitmap(thumbnail);

        return convertView;
    }

    private Bitmap generateThumbnailForNumber(int number) {


    }
}