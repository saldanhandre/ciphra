package com.example.uidesign_cistercian;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Map to hold the relationships between segments
    private Map<Integer, int[]> segmentRelations;
    private Map<Integer, Integer> diagonalSegmentPairs;
    TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeSegmentRelations(); // Initialize the relationships
        initializeDiagonalSegmentPairs();
        initializeSegmentClickListeners(); // Set up the click listeners for each segment
        resultTextView = findViewById(R.id.resultTextView); // Initialize the TextView for the result

        // Update the result initially
        updateResult();

        Button conversionButton = findViewById(R.id.cistArabConversionButton);

        Button openArabicConversionLayoutButton = findViewById(R.id.openArabicConversionLayoutButton);
        openArabicConversionLayoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ArabicConversionActivity.class);
            startActivity(intent);
        });

        conversionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int arabicNumber = convertCistercianToArabic();
                displayArabicNumber(arabicNumber);
            }
        });
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
        diagonalSegmentPairs.put(R.id.segment4_2, R.id.segment4_1);
        diagonalSegmentPairs.put(R.id.segment8_1, R.id.segment8_2);
        diagonalSegmentPairs.put(R.id.segment8_2, R.id.segment8_1);
        diagonalSegmentPairs.put(R.id.segment9_1, R.id.segment9_2);
        diagonalSegmentPairs.put(R.id.segment9_2, R.id.segment9_1);
        diagonalSegmentPairs.put(R.id.segment13_1, R.id.segment13_2);
        diagonalSegmentPairs.put(R.id.segment13_2, R.id.segment13_1);
        diagonalSegmentPairs.put(R.id.segment14_1, R.id.segment14_2);
        diagonalSegmentPairs.put(R.id.segment14_2, R.id.segment14_1);
        diagonalSegmentPairs.put(R.id.segment18_1, R.id.segment18_2);
        diagonalSegmentPairs.put(R.id.segment18_2, R.id.segment18_1);
        diagonalSegmentPairs.put(R.id.segment19_1, R.id.segment19_2);
        diagonalSegmentPairs.put(R.id.segment19_2, R.id.segment19_1);
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

    /*
    Setting up click listeners
    */
    private void setupSegmentClickListener(int imageViewId) {
        ImageView imageView = findViewById(imageViewId);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the selected state
                boolean isSelected = !imageView.isSelected();
                imageView.setSelected(isSelected);

                // Check for invalid combinations every time a segment is pressed
                invalidCheck();

                // If segment 5 is unpressed and both 1 and 2 are pressed, unpress segment 1
                if (imageViewId == R.id.segment5 && !isSelected &&
                        isSegmentPressed(R.id.segment1) && isSegmentPressed(R.id.segment2)) {
                    Log.d("SegmentClick", "Unpressing segment 2 as segment 5 is unpressed while 1 and 2 are pressed");
                    setSegmentPressed(R.id.segment2, false);
                    setSegmentPressed(R.id.segment5, false);
                }

                // If segment 10 is unpressed and both 6 and 7 are pressed, unpress segment 6
                if (imageViewId == R.id.segment10 && !isSelected &&
                        isSegmentPressed(R.id.segment6) && isSegmentPressed(R.id.segment7)) {
                    Log.d("SegmentClick", "Unpressing segment 7 as segment 10 is unpressed while 6 and 7 are pressed");
                    setSegmentPressed(R.id.segment7, false);
                    setSegmentPressed(R.id.segment10, false);
                }

                // If segment 15 is unpressed and both 11 and 12 are pressed, unpress segment 11
                if (imageViewId == R.id.segment15 && !isSelected &&
                        isSegmentPressed(R.id.segment11) && isSegmentPressed(R.id.segment12)) {
                    Log.d("SegmentClick", "Unpressing segment 12 as segment 15 is unpressed while 11 and 12 are pressed");
                    setSegmentPressed(R.id.segment12, false);
                    setSegmentPressed(R.id.segment15, false);
                }

                // If segment 20 is unpressed and both 16 and 17 are pressed, unpress segment 16
                if (imageViewId == R.id.segment20 && !isSelected &&
                        isSegmentPressed(R.id.segment16) && isSegmentPressed(R.id.segment17)) {
                    Log.d("SegmentClick", "Unpressing segment 17 as segment 20 is unpressed while 16 and 17 are pressed");
                    setSegmentPressed(R.id.segment17, false);
                    setSegmentPressed(R.id.segment20, false);
                }

                // Update the related segments for this ImageView
                updateRelatedSegments(imageViewId, isSelected);

                // Update the result
                updateResult();
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

                // Update the result
                updateResult();  // Make sure this line is there
            }
        };

        // Set the same click listener for both halves
        segmentHalf1.setOnClickListener(clickListener);
        segmentHalf2.setOnClickListener(clickListener);
    }



    /*
    This method is the 1st step in avoiding invalid combinations.
    When a segment is pressed, it makes the segments that wouldn't make a valid combination with the clicked one, unclickable
    */
    private void updateRelatedSegments(int clickedSegmentId, boolean isSelected) {
        if (isSelected) {
            // If the segment is now selected, disable related segments
            for (int relatedSegmentId : segmentRelations.get(clickedSegmentId)) {
                ImageView relatedSegment = findViewById(relatedSegmentId);
                relatedSegment.setEnabled(false);
                relatedSegment.setAlpha(0.0f); // set transparency to indicate disabled state visually
            }
        } else {
            // If the segment is deselected, enable all segments that are not related to any selected segment
            for (int segmentId : segmentRelations.keySet()) {
                ImageView segment = findViewById(segmentId);
                if (!segment.isSelected()) { // Check if the segment itself is not selected
                    boolean shouldEnable = true;
                    for (int relatedSegmentId : segmentRelations.get(segmentId)) {
                        ImageView relatedSegment = findViewById(relatedSegmentId);
                        // If any related segment is selected, this segment should remain disabled
                        shouldEnable &= !relatedSegment.isSelected();
                    }
                    if (shouldEnable) {
                        segment.setEnabled(true);
                        segment.setAlpha(1.0f); // reset transparency
                    }
                }
            }
        }
    }




    /*
    This logic is the 2nd step in avoiding invalid combinations.
    Even after making some segments unavailable, it is possible for the user to write an invalid character while trying to achieve another one.
    For example, when pressing the segments for the arabic number 9, the user can press segments 1->2->5.
    This would be correct if the converted result is only seen after pressing a button, but by implementing real time updating of the result,
    it leads to invalid combinations (in this case, before pressing segment5, only segments 1 and 2 are pressed, which is invalid.

    When a segment is pressed and forms an invalid combination, the next segment to form a valid combination is automatically pressed. If that segment
    is manually unpressed, the method remembers the last pressed segment and unpressed it as well.
    */
    private void invalidCheck() {
        Log.d("InvalidCheck", "Checking for invalid combinations");
        // Check for invalid combination
        if (isSegmentPressed(R.id.segment1) && isSegmentPressed(R.id.segment2) &&
                !isSegmentPressed(R.id.segment3_1) && !isSegmentPressed(R.id.segment3_2) &&
                !isSegmentPressed(R.id.segment4_1) && !isSegmentPressed(R.id.segment4_2)) {
            Log.d("InvalidCheck", "Invalid combination found with segments 1 and 2");
            // This is the invalid combination. Press segment5 automatically.
            setSegmentPressed(R.id.segment5, true);
        }
        if (isSegmentPressed(R.id.segment6) && isSegmentPressed(R.id.segment7) &&
                !isSegmentPressed(R.id.segment8_1) && !isSegmentPressed(R.id.segment8_2) &&
                !isSegmentPressed(R.id.segment9_1) && !isSegmentPressed(R.id.segment9_2)) {
            Log.d("InvalidCheck", "Invalid combination found with segments 6 and 7");
            setSegmentPressed(R.id.segment10, true);
        }
        if (isSegmentPressed(R.id.segment11) && isSegmentPressed(R.id.segment12) &&
                !isSegmentPressed(R.id.segment13_1) && !isSegmentPressed(R.id.segment13_2) &&
                !isSegmentPressed(R.id.segment14_1) && !isSegmentPressed(R.id.segment14_2)) {
            Log.d("InvalidCheck", "Invalid combination found with segments 11 and 12");
            setSegmentPressed(R.id.segment15, true);
        }
        if (isSegmentPressed(R.id.segment16) && isSegmentPressed(R.id.segment17) &&
                !isSegmentPressed(R.id.segment18_1) && !isSegmentPressed(R.id.segment18_2) &&
                !isSegmentPressed(R.id.segment19_1) && !isSegmentPressed(R.id.segment19_2)) {
            Log.d("InvalidCheck", "Invalid combination found with segments 16 and 17");
            setSegmentPressed(R.id.segment20, true);
        }
        /*
        // Check if segment 5 is unpressed while both 1 and 2 are pressed
        if (!isSegmentPressed(R.id.segment5) && isSegmentPressed(R.id.segment1) && isSegmentPressed(R.id.segment2)) {
            Log.d("InvalidCheck", "Unpressing segment 1 as segment 5 is unpressed while 1 and 2 are pressed");
            setSegmentPressed(R.id.segment1, false);
        }
        */


    }

    private boolean isSegmentPressed(int segmentId) {
        ImageView segment = findViewById(segmentId);
        return segment.isSelected();
    }

    private void setSegmentPressed(int segmentId, boolean pressed) {
        ImageView segment = findViewById(segmentId);
        segment.setSelected(pressed);

        // Visually update the segment's appearance based on its state
        segment.setAlpha(pressed ? 1.0f : 0.0f);
    }


    /*
    Logic of conversion from Cistercian to Arabic.
    Tests the combinations and turns them into arabic numbers
    Result in int arabicResult
     */
    private int convertCistercianToArabic() {
        // Initialize the number for each quadrant
        int units = 0, tens = 0, hundreds = 0, thousands = 0;

        // Check the segments for the units
        if (findViewById(R.id.segment1).isSelected()) units += 1;
        if (findViewById(R.id.segment2).isSelected()) units += 2;
        if (findViewById(R.id.segment3_1).isSelected() || findViewById(R.id.segment3_2).isSelected()) units += 3;
        if (findViewById(R.id.segment4_1).isSelected() || findViewById(R.id.segment4_2).isSelected()) units += 4;
        // Check if segment5 is selected along with segments 1 or 2 for numbers 7 and 8
        if (findViewById(R.id.segment5).isSelected()) {
            if (units == 1) {
                units = 7; // segment1 and segment5
            } else if (units == 2) {
                units = 8; // segment2 and segment5
            } else if (units == 3) {
                units = 9; // segment1, segment2 and segment5
            } else if (units == 0) {
                units = 6; // only segment5
            } // If units is already 3 or 4, we do not change it because segment5 cannot combine with segment3 or segment4
        }

        // Check the segments for the tens
        if (findViewById(R.id.segment6).isSelected()) tens += 10;
        if (findViewById(R.id.segment7).isSelected()) tens += 20;
        if (findViewById(R.id.segment8_1).isSelected() || findViewById(R.id.segment8_2).isSelected()) tens += 30;
        if (findViewById(R.id.segment9_1).isSelected() || findViewById(R.id.segment9_2).isSelected()) tens += 40;
        if (findViewById(R.id.segment10).isSelected()) {
            if (tens == 10) {
                tens = 70;
            } else if (tens == 20) {
                tens = 80;
            } else if (tens == 30) {
                tens = 90;
            } else {
                tens = 60;
            }
        }

        // Check the segments for the hundreds
        if (findViewById(R.id.segment11).isSelected()) hundreds += 100;
        if (findViewById(R.id.segment12).isSelected()) hundreds += 200;
        if (findViewById(R.id.segment13_1).isSelected() || findViewById(R.id.segment13_2).isSelected()) hundreds += 300;
        if (findViewById(R.id.segment14_1).isSelected() || findViewById(R.id.segment14_2).isSelected()) hundreds += 400;
        if (findViewById(R.id.segment15).isSelected()) {
            if (hundreds == 100) {
                hundreds = 700;
            } else if (hundreds == 200) {
                hundreds = 800;
            } else if (hundreds == 300) {
                hundreds = 900;
            } else {
                hundreds = 600;
            }
        }

        // Check the segments for the thousands
        if (findViewById(R.id.segment16).isSelected()) thousands += 1000;
        if (findViewById(R.id.segment17).isSelected()) thousands += 2000;
        if (findViewById(R.id.segment18_1).isSelected() || findViewById(R.id.segment18_2).isSelected()) thousands += 3000;
        if (findViewById(R.id.segment19_1).isSelected() || findViewById(R.id.segment19_2).isSelected()) thousands += 4000;
        if (findViewById(R.id.segment20).isSelected()) {
            if (thousands == 1000) {
                thousands = 7000;
            } else if (thousands == 2000) {
                thousands = 8000;
            } else if (thousands == 3000) {
                thousands = 9000;
            } else {
                thousands = 6000;
            }
        }

        int arabicResult = thousands + hundreds + tens + units;
        return arabicResult;
    }


    /*
    Display the arabicResult
     */
    private void displayArabicNumber(int number) {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.cistercian_conversion_layout, null);
        // Find the TextView and set the number
        TextView resultTextView = dialogView.findViewById(R.id.resultTextView);
        resultTextView.setText(String.valueOf(number));
        // Set the custom layout on the dialog builder
        builder.setView(dialogView);
        // Add an OK button to dismiss the dialog
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                dialog.dismiss();
            }
        });
        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateResult() {
        int arabicNumber = convertCistercianToArabic();
        resultTextView.setText(String.valueOf(arabicNumber));
    }
}