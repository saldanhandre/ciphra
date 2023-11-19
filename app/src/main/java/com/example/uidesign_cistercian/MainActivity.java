package com.example.uidesign_cistercian;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.graphics.Rect;
import android.view.TouchDelegate;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to your XML layout
        setContentView(R.layout.activity_main);

        // Set up the click listener for each segment
        setupSegmentClickListener(R.id.segment1);
        setupSegmentClickListener(R.id.segment2);
        setupDiagonalSegmentClickListener(R.id.segment3_1, R.id.segment3_2);
        setupDiagonalSegmentClickListener(R.id.segment4_1, R.id.segment4_2);
        setupSegmentClickListener(R.id.segment5);

        setupSegmentClickListener(R.id.segment6);
        setupSegmentClickListener(R.id.segment7);
        setupDiagonalSegmentClickListener(R.id.segment8_1, R.id.segment8_2);
        setupDiagonalSegmentClickListener(R.id.segment9_1, R.id.segment9_2);
        setupSegmentClickListener(R.id.segment10);

        setupSegmentClickListener(R.id.segment11);
        setupSegmentClickListener(R.id.segment12);
        setupDiagonalSegmentClickListener(R.id.segment13_1, R.id.segment13_2);
        setupDiagonalSegmentClickListener(R.id.segment14_1, R.id.segment14_2);
        setupSegmentClickListener(R.id.segment15);

        setupSegmentClickListener(R.id.segment16);
        setupSegmentClickListener(R.id.segment17);
        setupDiagonalSegmentClickListener(R.id.segment18_1, R.id.segment18_2);
        setupDiagonalSegmentClickListener(R.id.segment19_1, R.id.segment19_2);
        setupSegmentClickListener(R.id.segment20);
    }

    private void setupSegmentClickListener(int imageViewId) {
        final View imageView = findViewById(imageViewId);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the selected state of the ImageView
                imageView.setSelected(!imageView.isSelected());
            }
        });
    }


    private void setupDiagonalSegmentClickListener(int segmentHalf1Id, int segmentHalf2Id) {
        final View segmentHalf1 = findViewById(segmentHalf1Id);
        final View segmentHalf2 = findViewById(segmentHalf2Id);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the selected state of both halves
                boolean newSelectedState = !segmentHalf1.isSelected();
                segmentHalf1.setSelected(newSelectedState);
                segmentHalf2.setSelected(newSelectedState);
            }
        };

        // Set the same click listener for both halves of the diagonal segment
        segmentHalf1.setOnClickListener(clickListener);
        segmentHalf2.setOnClickListener(clickListener);
    }
}