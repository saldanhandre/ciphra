package com.example.uidesign_cistercian;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Map<Integer, int[]> segmentRelations;
    private Map<Integer, Integer> diagonalSegmentPairs;
    TextView resultTextView;
    private EditText resultEditText;
    FrameLayout photo_gallery_button, camera_button, bin_button;
    ImageView imageView;
    Bitmap bitmap;
    int SELECT_CODE = 100, CAMERA_CODE = 101;
    Mat mat;
    private boolean isProgrammaticChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(OpenCVLoader.initDebug()) Log.d("LOADED", "success");
        else Log.d("LOADED", "err");

        initializeSegmentRelations(); // Initialize the relationships
        initializeDiagonalSegmentPairs();
        initializeSegmentClickListeners(); // Set up the click listeners for each segment
        resultTextView = findViewById(R.id.resultTextView); // Initialize the TextView for the result

        updateResult();  // Update the result initially
        getPermission(); // Get permissions such as the camera use

        FrameLayout history_button = findViewById(R.id.history_button);
        history_button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        resultEditText = findViewById(R.id.resultEditText);

        resultEditText.setText("0"); // Initialize with "0000"
        resultEditText.setSelection(resultEditText.getText().length()); // Move cursor to end
        resultEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    // Prevents deleting the last digit by setting it to "0"
                    resultEditText.setText("0");
                    // Position the cursor at the end of the text
                    resultEditText.setSelection(resultEditText.getText().length());

                }
                //else if(s.length() == 2 && s(1) == 0) {

                else {
                    // Normal processing for any other case
                    try {
                        int number = Integer.parseInt(s.toString());
                        int thousands = number / 1000;
                        int hundreds = (number % 1000) / 100;
                        int tens = (number % 100) / 10;
                        int units = number % 10;

                        updateCistercianSegments(thousands, hundreds, tens, units);
                    } catch (NumberFormatException e) {
                        // Handle the case where the input is not a valid number
                        clearCistercianSegments(); // Clear the segments in case of invalid number
                    }
                }
            }
        });





        camera_button = findViewById(R.id.camera_button);
        photo_gallery_button = findViewById(R.id.photo_gallery_button);
        bin_button = findViewById(R.id.bin_button);
        //imageView = findViewById(R.id.image_display_view);

        photo_gallery_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_CODE);
            }
        });

        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_CODE);
            }
        });

        bin_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateCistercianSegments(0,0,0,0);
                updateResult();

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

                // Reset the timer
                resetConversionTimer();
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

                // Reset the timer
                resetConversionTimer();
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
    For example, when pressing the segments for the arabic number 9, the user may press segments 1->2->5.
    This would be correct if the converted result is only seen after pressing a "CONVERT" button, but by implementing real time updating of the result,
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
        if (findViewById(R.id.segment3_1).isSelected() || findViewById(R.id.segment3_2).isSelected())
            units += 3;
        if (findViewById(R.id.segment4_1).isSelected() || findViewById(R.id.segment4_2).isSelected())
            units += 4;
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
        if (findViewById(R.id.segment8_1).isSelected() || findViewById(R.id.segment8_2).isSelected())
            tens += 30;
        if (findViewById(R.id.segment9_1).isSelected() || findViewById(R.id.segment9_2).isSelected())
            tens += 40;
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
        if (findViewById(R.id.segment13_1).isSelected() || findViewById(R.id.segment13_2).isSelected())
            hundreds += 300;
        if (findViewById(R.id.segment14_1).isSelected() || findViewById(R.id.segment14_2).isSelected())
            hundreds += 400;
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
        if (findViewById(R.id.segment18_1).isSelected() || findViewById(R.id.segment18_2).isSelected())
            thousands += 3000;
        if (findViewById(R.id.segment19_1).isSelected() || findViewById(R.id.segment19_2).isSelected())
            thousands += 4000;
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
        EditText resultEditText = findViewById(R.id.resultEditText);
        resultEditText.setText(String.valueOf(number));
    }

    private void updateResult() {
        int arabicNumber = convertCistercianToArabic();
        displayArabicNumber(arabicNumber);
    }


    /*
     * Arabic to Cistercian Conversion
     */
    private void updateCistercianSegments(int thousands, int hundreds, int tens, int units) {
        findViewById(R.id.central_stem).setVisibility(View.VISIBLE);
        switch(units){
            case 0:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 1:
                setSegmentPressed(R.id.segment1, true);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, true);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 2:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, true);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, true);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 3:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, true);
                setSegmentPressed(R.id.segment3_2, true);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, true);
                updateRelatedSegments(R.id.segment3_2, true);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 4:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, true);
                setSegmentPressed(R.id.segment4_2, true);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, true);
                updateRelatedSegments(R.id.segment4_2, true);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 5:
                setSegmentPressed(R.id.segment1, true);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, true);
                setSegmentPressed(R.id.segment4_2, true);
                setSegmentPressed(R.id.segment5, false);

                updateRelatedSegments(R.id.segment1, true);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, true);
                updateRelatedSegments(R.id.segment4_2, true);
                updateRelatedSegments(R.id.segment5, false);
                break;
            case 6:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, true);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, true);
                break;
            case 7:
                setSegmentPressed(R.id.segment1, true);
                setSegmentPressed(R.id.segment2, false);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, true);

                updateRelatedSegments(R.id.segment1, true);
                updateRelatedSegments(R.id.segment2, false);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, true);
                break;
            case 8:
                setSegmentPressed(R.id.segment1, false);
                setSegmentPressed(R.id.segment2, true);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, true);

                updateRelatedSegments(R.id.segment1, false);
                updateRelatedSegments(R.id.segment2, true);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, true);
                break;
            case 9:
                setSegmentPressed(R.id.segment1, true);
                setSegmentPressed(R.id.segment2, true);
                setSegmentPressed(R.id.segment3_1, false);
                setSegmentPressed(R.id.segment3_2, false);
                setSegmentPressed(R.id.segment4_1, false);
                setSegmentPressed(R.id.segment4_2, false);
                setSegmentPressed(R.id.segment5, true);

                updateRelatedSegments(R.id.segment1, true);
                updateRelatedSegments(R.id.segment2, true);
                updateRelatedSegments(R.id.segment3_1, false);
                updateRelatedSegments(R.id.segment3_2, false);
                updateRelatedSegments(R.id.segment4_1, false);
                updateRelatedSegments(R.id.segment4_2, false);
                updateRelatedSegments(R.id.segment5, true);
                break;
        }

        switch(tens){
            case 0:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 1:
                setSegmentPressed(R.id.segment6, true);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, true);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 2:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, true);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, true);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 3:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, true);
                setSegmentPressed(R.id.segment8_2, true);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, true);
                updateRelatedSegments(R.id.segment8_2, true);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 4:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, true);
                setSegmentPressed(R.id.segment9_2, true);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, true);
                updateRelatedSegments(R.id.segment9_2, true);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 5:
                setSegmentPressed(R.id.segment6, true);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, true);
                setSegmentPressed(R.id.segment9_2, true);
                setSegmentPressed(R.id.segment10, false);

                updateRelatedSegments(R.id.segment6, true);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, true);
                updateRelatedSegments(R.id.segment9_2, true);
                updateRelatedSegments(R.id.segment10, false);
                break;
            case 6:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, true);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, true);
                break;
            case 7:
                setSegmentPressed(R.id.segment6, true);
                setSegmentPressed(R.id.segment7, false);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, true);

                updateRelatedSegments(R.id.segment6, true);
                updateRelatedSegments(R.id.segment7, false);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, true);
                break;
            case 8:
                setSegmentPressed(R.id.segment6, false);
                setSegmentPressed(R.id.segment7, true);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, true);

                updateRelatedSegments(R.id.segment6, false);
                updateRelatedSegments(R.id.segment7, true);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, true);
                break;
            case 9:
                setSegmentPressed(R.id.segment6, true);
                setSegmentPressed(R.id.segment7, true);
                setSegmentPressed(R.id.segment8_1, false);
                setSegmentPressed(R.id.segment8_2, false);
                setSegmentPressed(R.id.segment9_1, false);
                setSegmentPressed(R.id.segment9_2, false);
                setSegmentPressed(R.id.segment10, true);

                updateRelatedSegments(R.id.segment6, true);
                updateRelatedSegments(R.id.segment7, true);
                updateRelatedSegments(R.id.segment8_1, false);
                updateRelatedSegments(R.id.segment8_2, false);
                updateRelatedSegments(R.id.segment9_1, false);
                updateRelatedSegments(R.id.segment9_2, false);
                updateRelatedSegments(R.id.segment10, true);
                break;
        }

        switch(hundreds){
            case 0:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 1:
                setSegmentPressed(R.id.segment11, true);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, true);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 2:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, true);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, true);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 3:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, true);
                setSegmentPressed(R.id.segment13_2, true);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, true);
                updateRelatedSegments(R.id.segment13_2, true);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 4:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, true);
                setSegmentPressed(R.id.segment14_2, true);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, true);
                updateRelatedSegments(R.id.segment14_2, true);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 5:
                setSegmentPressed(R.id.segment11, true);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, true);
                setSegmentPressed(R.id.segment14_2, true);
                setSegmentPressed(R.id.segment15, false);

                updateRelatedSegments(R.id.segment11, true);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, true);
                updateRelatedSegments(R.id.segment14_2, true);
                updateRelatedSegments(R.id.segment15, false);
                break;
            case 6:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, true);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, true);
                break;
            case 7:
                setSegmentPressed(R.id.segment11, true);
                setSegmentPressed(R.id.segment12, false);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, true);

                updateRelatedSegments(R.id.segment11, true);
                updateRelatedSegments(R.id.segment12, false);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, true);
                break;
            case 8:
                setSegmentPressed(R.id.segment11, false);
                setSegmentPressed(R.id.segment12, true);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, true);

                updateRelatedSegments(R.id.segment11, false);
                updateRelatedSegments(R.id.segment12, true);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, true);
                break;
            case 9:
                setSegmentPressed(R.id.segment11, true);
                setSegmentPressed(R.id.segment12, true);
                setSegmentPressed(R.id.segment13_1, false);
                setSegmentPressed(R.id.segment13_2, false);
                setSegmentPressed(R.id.segment14_1, false);
                setSegmentPressed(R.id.segment14_2, false);
                setSegmentPressed(R.id.segment15, true);

                updateRelatedSegments(R.id.segment11, true);
                updateRelatedSegments(R.id.segment12, true);
                updateRelatedSegments(R.id.segment13_1, false);
                updateRelatedSegments(R.id.segment13_2, false);
                updateRelatedSegments(R.id.segment14_1, false);
                updateRelatedSegments(R.id.segment14_2, false);
                updateRelatedSegments(R.id.segment15, true);
                break;
        }

        switch(thousands){
            case 0:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 1:
                setSegmentPressed(R.id.segment16, true);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, true);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 2:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, true);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, true);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 3:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, true);
                setSegmentPressed(R.id.segment18_2, true);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, true);
                updateRelatedSegments(R.id.segment18_2, true);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 4:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, true);
                setSegmentPressed(R.id.segment19_2, true);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, true);
                updateRelatedSegments(R.id.segment19_2, true);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 5:
                setSegmentPressed(R.id.segment16, true);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, true);
                setSegmentPressed(R.id.segment19_2, true);
                setSegmentPressed(R.id.segment20, false);

                updateRelatedSegments(R.id.segment16, true);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, true);
                updateRelatedSegments(R.id.segment19_2, true);
                updateRelatedSegments(R.id.segment20, false);
                break;
            case 6:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, true);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, true);
                break;
            case 7:
                setSegmentPressed(R.id.segment16, true);
                setSegmentPressed(R.id.segment17, false);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, true);

                updateRelatedSegments(R.id.segment16, true);
                updateRelatedSegments(R.id.segment17, false);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, true);
                break;
            case 8:
                setSegmentPressed(R.id.segment16, false);
                setSegmentPressed(R.id.segment17, true);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, true);

                updateRelatedSegments(R.id.segment16, false);
                updateRelatedSegments(R.id.segment17, true);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, true);
                break;
            case 9:
                setSegmentPressed(R.id.segment16, true);
                setSegmentPressed(R.id.segment17, true);
                setSegmentPressed(R.id.segment18_1, false);
                setSegmentPressed(R.id.segment18_2, false);
                setSegmentPressed(R.id.segment19_1, false);
                setSegmentPressed(R.id.segment19_2, false);
                setSegmentPressed(R.id.segment20, true);

                updateRelatedSegments(R.id.segment16, true);
                updateRelatedSegments(R.id.segment17, true);
                updateRelatedSegments(R.id.segment18_1, false);
                updateRelatedSegments(R.id.segment18_2, false);
                updateRelatedSegments(R.id.segment19_1, false);
                updateRelatedSegments(R.id.segment19_2, false);
                updateRelatedSegments(R.id.segment20, true);
                break;
        }
    }

    private void clearCistercianSegments(){
        setSegmentPressed(R.id.segment1, false);
        setSegmentPressed(R.id.segment2, false);
        setSegmentPressed(R.id.segment3_1, false);
        setSegmentPressed(R.id.segment3_2, false);
        setSegmentPressed(R.id.segment4_1, false);
        setSegmentPressed(R.id.segment4_2, false);
        setSegmentPressed(R.id.segment5, false);
        setSegmentPressed(R.id.segment6, false);
        setSegmentPressed(R.id.segment7, false);
        setSegmentPressed(R.id.segment8_1, false);
        setSegmentPressed(R.id.segment8_2, false);
        setSegmentPressed(R.id.segment9_1, false);
        setSegmentPressed(R.id.segment9_2, false);
        setSegmentPressed(R.id.segment10, false);
        setSegmentPressed(R.id.segment11, false);
        setSegmentPressed(R.id.segment12, false);
        setSegmentPressed(R.id.segment13_1, false);
        setSegmentPressed(R.id.segment13_2, false);
        setSegmentPressed(R.id.segment14_1, false);
        setSegmentPressed(R.id.segment14_2, false);
        setSegmentPressed(R.id.segment15, false);
        setSegmentPressed(R.id.segment16, false);
        setSegmentPressed(R.id.segment17, false);
        setSegmentPressed(R.id.segment18_1, false);
        setSegmentPressed(R.id.segment18_2, false);
        setSegmentPressed(R.id.segment19_1, false);
        setSegmentPressed(R.id.segment19_2, false);
        setSegmentPressed(R.id.segment20, false);
    }



    /*
     * Timer
     */
    private Handler conversionHandler = new Handler();
    private Runnable conversionRunnable = new Runnable() {
        @Override
        public void run() {
            // Get the current converted Arabic number
            int arabicNumber = convertCistercianToArabic();

            // Add it to the history
            // Assuming you have a method in ConversionHistoryManager to add just an integer
            ConversionHistoryManager.getInstance().addConversion(arabicNumber);
        }
    };

    private void resetConversionTimer() {
        conversionHandler.removeCallbacks(conversionRunnable);
        conversionHandler.postDelayed(conversionRunnable, 2500); // 5 seconds
    }


    /*
     * This method manages the buttons to add an image for processing, which may be
     * through the photo gallery, or the camera
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, ImageDisplayActivity.class);

            if (requestCode == SELECT_CODE && data != null) {
                Uri imageUri = data.getData();
                intent.putExtra("imageUri", imageUri.toString());
            } else if (requestCode == CAMERA_CODE && data != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
                // Convert bitmap to byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                intent.putExtra("imageBitmap", byteArray);
            }

            startActivity(intent);
        }
    }



    // Method to get the camera permission from the user
    void getPermission(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 102);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission();
            }
        }
    }
}