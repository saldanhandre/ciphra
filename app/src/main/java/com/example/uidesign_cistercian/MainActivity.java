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
        setupSegmentClickListener(R.id.segment3);
        setupSegmentClickListener(R.id.segment4);
        setupSegmentClickListener(R.id.segment5);
        setupSegmentClickListener(R.id.segment6);
        setupSegmentClickListener(R.id.segment7);
        setupSegmentClickListener(R.id.segment8);
        setupSegmentClickListener(R.id.segment9);
        setupSegmentClickListener(R.id.segment10);
        setupSegmentClickListener(R.id.segment11);
        setupSegmentClickListener(R.id.segment12);
        setupSegmentClickListener(R.id.segment13);
        setupSegmentClickListener(R.id.segment14);
        setupSegmentClickListener(R.id.segment15);
        setupSegmentClickListener(R.id.segment16);
        setupSegmentClickListener(R.id.segment17);
        setupSegmentClickListener(R.id.segment18);
        setupSegmentClickListener(R.id.segment19);
        setupSegmentClickListener(R.id.segment20);
    }

    private void setupSegmentClickListener(int segmentId) {
        final View segmentView = findViewById(segmentId);
        segmentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
            }
        });

        // Post a runnable to the segment view's message queue to set touch delegate after the layout is complete
        segmentView.post(new Runnable() {
            @Override
            public void run() {
                // Calculate touchable area rect here based on the expected touch area
                Rect delegateArea = new Rect();
                segmentView.getHitRect(delegateArea);

                // Optionally increase touch area. This example code expands the touch area by 10 pixels on all sides.
                delegateArea.left -= 10;
                delegateArea.right += 10;
                delegateArea.top -= 10;
                delegateArea.bottom += 10;

                // Set the TouchDelegate on the parent view to the calculated area
                if (View.class.isInstance(segmentView.getParent())) {
                    ((View) segmentView.getParent()).setTouchDelegate(new TouchDelegate(delegateArea, segmentView));
                }
            }
        });
    }
}