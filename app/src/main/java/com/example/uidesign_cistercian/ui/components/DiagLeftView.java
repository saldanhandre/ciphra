package com.example.uidesign_cistercian.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.MotionEvent;
import android.graphics.Color;


public class DiagLeftView extends androidx.appcompat.widget.AppCompatImageView {

    private Path diagonalPath;
    private Paint touchPaint; // For debugging, if necessary
    private RectF pathBounds;
    private static final int STROKE_WIDTH = 7; // Or whatever your stroke width should be


    public DiagLeftView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize the path representing the diagonal segment
        diagonalPath = new Path();
        // Define the diagonal path here based on the image's shape
        diagonalPath.moveTo(0,0 ); // Start at the top-left corner
        diagonalPath.lineTo(getWidth(), getHeight()); // Draw to the bottom-right corner
        diagonalPath.lineTo(getWidth(), getHeight() - STROKE_WIDTH); // Account for stroke width
        diagonalPath.lineTo(STROKE_WIDTH, 0); // Draw back to the top-left, offset by stroke width
        diagonalPath.close(); // Close the path to create a solid shape

        // Debugging
        touchPaint = new Paint();
        touchPaint.setColor(Color.RED);
        touchPaint.setStyle(Paint.Style.STROKE);
        touchPaint.setStrokeWidth(4);

        pathBounds = new RectF();
        diagonalPath.computeBounds(pathBounds, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // For debugging, draw the path to see it on screen
        canvas.drawPath(diagonalPath, touchPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Use `Region` to handle complex touch areas
            Region region = new Region();
            // Set the region to the bounds of the canvas
            region.setPath(diagonalPath, new Region((int) pathBounds.left, (int) pathBounds.top, (int) pathBounds.right, (int) pathBounds.bottom));
            // Check if the region contains the touch coordinates
            if (region.contains((int) event.getX(), (int) event.getY())) {
                // Touch was within the diagonal path
                this.performClick(); // Perform the click action
                return true; // Indicate that we've handled the touch event
            }
        }
        return super.onTouchEvent(event); // Pass the event up to the parent if not handled
    }

    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the OnClickListener, if any.
        return super.performClick();
    }
}
