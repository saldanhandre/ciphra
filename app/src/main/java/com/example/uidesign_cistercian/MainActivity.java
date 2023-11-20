package com.example.uidesign_cistercian;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Map to hold the relationships between segments
    private Map<Integer, int[]> segmentRelations;
    private Map<Integer, Integer> diagonalSegmentPairs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the relationships
        initializeSegmentRelations();
        initializeDiagonalSegmentPairs();

        // Set up the click listeners for each segment
        initializeSegmentClickListeners();
    }

    private void initializeSegmentRelations() {
        segmentRelations = new HashMap<>();
        segmentRelations.put(R.id.segment1, new int[]{R.id.segment3_1, R.id.segment3_2});
        segmentRelations.put(R.id.segment2, new int[]{R.id.segment3_1, R.id.segment3_2, R.id.segment4_1, R.id.segment4_2});
        segmentRelations.put(R.id.segment3_1, new int[]{R.id.segment1, R.id.segment2, R.id.segment4_1, R.id.segment4_2, R.id.segment5});
        segmentRelations.put(R.id.segment3_2, new int[]{R.id.segment1, R.id.segment2, R.id.segment4_1, R.id.segment4_2, R.id.segment5});
        segmentRelations.put(R.id.segment4_1, new int[]{R.id.segment2, R.id.segment3_1, R.id.segment3_2, R.id.segment5});
        segmentRelations.put(R.id.segment4_2, new int[]{R.id.segment2, R.id.segment3_1, R.id.segment3_2, R.id.segment5});
        segmentRelations.put(R.id.segment5, new int[]{R.id.segment3_1, R.id.segment3_2, R.id.segment4_1, R.id.segment4_2});

        segmentRelations.put(R.id.segment6, new int[]{R.id.segment8_1, R.id.segment8_2});
        segmentRelations.put(R.id.segment7, new int[]{R.id.segment8_1, R.id.segment8_2, R.id.segment9_1, R.id.segment9_2});
        segmentRelations.put(R.id.segment8_1, new int[]{R.id.segment6, R.id.segment7, R.id.segment9_1, R.id.segment9_2, R.id.segment10});
        segmentRelations.put(R.id.segment8_2, new int[]{R.id.segment6, R.id.segment7, R.id.segment9_1, R.id.segment9_2, R.id.segment10});
        segmentRelations.put(R.id.segment9_1, new int[]{R.id.segment7, R.id.segment8_1, R.id.segment8_2, R.id.segment10});
        segmentRelations.put(R.id.segment9_2, new int[]{R.id.segment7, R.id.segment8_1, R.id.segment8_2, R.id.segment10});
        segmentRelations.put(R.id.segment10, new int[]{R.id.segment8_1, R.id.segment8_2, R.id.segment9_1, R.id.segment9_2});

        segmentRelations.put(R.id.segment11, new int[]{R.id.segment13_1, R.id.segment13_2});
        segmentRelations.put(R.id.segment12, new int[]{R.id.segment13_1, R.id.segment13_2, R.id.segment14_1, R.id.segment14_2});
        segmentRelations.put(R.id.segment13_1, new int[]{R.id.segment11, R.id.segment12, R.id.segment14_1, R.id.segment14_2, R.id.segment15});
        segmentRelations.put(R.id.segment13_2, new int[]{R.id.segment11, R.id.segment12, R.id.segment14_1, R.id.segment14_2, R.id.segment15});
        segmentRelations.put(R.id.segment14_1, new int[]{R.id.segment12, R.id.segment13_1, R.id.segment13_2, R.id.segment15});
        segmentRelations.put(R.id.segment14_2, new int[]{R.id.segment12, R.id.segment13_1, R.id.segment13_2, R.id.segment15});
        segmentRelations.put(R.id.segment15, new int[]{R.id.segment13_1, R.id.segment13_2, R.id.segment14_1, R.id.segment14_2});

        segmentRelations.put(R.id.segment16, new int[]{R.id.segment18_1, R.id.segment18_2});
        segmentRelations.put(R.id.segment17, new int[]{R.id.segment18_1, R.id.segment18_2, R.id.segment19_1, R.id.segment19_2});
        segmentRelations.put(R.id.segment18_1, new int[]{R.id.segment16, R.id.segment17, R.id.segment19_1, R.id.segment19_2, R.id.segment20});
        segmentRelations.put(R.id.segment18_2, new int[]{R.id.segment16, R.id.segment17, R.id.segment19_1, R.id.segment19_2, R.id.segment20});
        segmentRelations.put(R.id.segment19_1, new int[]{R.id.segment17, R.id.segment18_1, R.id.segment18_2, R.id.segment20});
        segmentRelations.put(R.id.segment19_2, new int[]{R.id.segment17, R.id.segment18_1, R.id.segment18_2, R.id.segment20});
        segmentRelations.put(R.id.segment20, new int[]{R.id.segment18_1, R.id.segment18_2, R.id.segment19_1, R.id.segment19_2});
    }

    private void initializeDiagonalSegmentPairs() {
        diagonalSegmentPairs = new HashMap<>();
        diagonalSegmentPairs.put(R.id.segment3_1, R.id.segment3_2);
        diagonalSegmentPairs.put(R.id.segment3_2, R.id.segment3_1);
        diagonalSegmentPairs.put(R.id.segment4_1, R.id.segment4_2);
        diagonalSegmentPairs.put(R.id.segment8_1, R.id.segment8_2);
        diagonalSegmentPairs.put(R.id.segment9_1, R.id.segment9_2);
        diagonalSegmentPairs.put(R.id.segment13_1, R.id.segment13_2);
        diagonalSegmentPairs.put(R.id.segment14_1, R.id.segment14_2);
        diagonalSegmentPairs.put(R.id.segment18_1, R.id.segment18_2);
        diagonalSegmentPairs.put(R.id.segment19_1, R.id.segment19_2);
    }

    private void initializeSegmentClickListeners() {
        for (int segmentId : segmentRelations.keySet()) {
            if (diagonalSegmentPairs.containsKey(segmentId)) {
                // If it's a diagonal pair, set up a special listener
                setupDiagonalSegmentClickListener(segmentId, diagonalSegmentPairs.get(segmentId));
            } else {
                // Otherwise, set up a normal segment click listener
                setupSegmentClickListener(segmentId);
            }
        }
    }

    private void setupSegmentClickListener(int imageViewId) {
        ImageView imageView = findViewById(imageViewId);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSegmentClick(imageViewId);
            }
        });
    }

    private void setupDiagonalSegmentClickListener(int segmentHalf1Id, int segmentHalf2Id) {
        final ImageView segmentHalf1 = findViewById(segmentHalf1Id);
        final ImageView segmentHalf2 = findViewById(segmentHalf2Id);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ensure both halves are toggled when either is clicked
                boolean newSelectedState = !segmentHalf1.isSelected() || !segmentHalf2.isSelected();
                segmentHalf1.setSelected(newSelectedState);
                segmentHalf2.setSelected(newSelectedState);

                // Now update the related segments for both halves
                updateRelatedSegments(segmentHalf1Id, newSelectedState);
                updateRelatedSegments(segmentHalf2Id, newSelectedState);
            }
        };

        // Set the same click listener for both halves
        segmentHalf1.setOnClickListener(clickListener);
        segmentHalf2.setOnClickListener(clickListener);
    }

    private void handleSegmentClick(int clickedSegmentId) {
        ImageView clickedSegment = findViewById(clickedSegmentId);
        boolean newSelectedState = !clickedSegment.isSelected();
        clickedSegment.setSelected(newSelectedState);

        // Synchronize the state of diagonal halves
        if (diagonalSegmentPairs.containsValue(clickedSegmentId)) {
            // Find the corresponding pair and update its state
            for (Map.Entry<Integer, Integer> entry : diagonalSegmentPairs.entrySet()) {
                if (entry.getValue() == clickedSegmentId) {
                    ImageView pairedSegment = findViewById(entry.getKey());
                    pairedSegment.setSelected(newSelectedState);
                    break;
                }
            }
        }

        // Update related segments
        updateRelatedSegments(clickedSegmentId, newSelectedState);
    }

    private void updateRelatedSegments(int clickedSegmentId, boolean isSelected) {
        // If the segment is selected, disable its related segments
        if (isSelected) {
            for (int relatedSegmentId : segmentRelations.get(clickedSegmentId)) {
                ImageView relatedSegment = findViewById(relatedSegmentId);
                relatedSegment.setEnabled(false);
                relatedSegment.setAlpha(0f); // Indicate disabled state visually
            }
        } else {
            // If the segment is deselected, we need to re-enable its related segments
            // but only if they are not related to any other selected segment.
            for (int relatedSegmentId : segmentRelations.get(clickedSegmentId)) {
                boolean canEnable = true;
                for (Map.Entry<Integer, int[]> entry : segmentRelations.entrySet()) {
                    ImageView segment = findViewById(entry.getKey());
                    if (segment.isSelected() && entry.getKey() != clickedSegmentId) {
                        for (int id : entry.getValue()) {
                            if (id == relatedSegmentId) {
                                canEnable = false;
                                break;
                            }
                        }
                    }
                    if (!canEnable) {
                        break;
                    }
                }
                if (canEnable) {
                    ImageView relatedSegment = findViewById(relatedSegmentId);
                    relatedSegment.setEnabled(true);
                    relatedSegment.setAlpha(1.0f);
                }
            }
        }
    }

    private boolean isSegmentInArray(int segmentId, int[] array) {
        for (int id : array) {
            if (id == segmentId) {
                return true;
            }
        }
        return false;
    }
}