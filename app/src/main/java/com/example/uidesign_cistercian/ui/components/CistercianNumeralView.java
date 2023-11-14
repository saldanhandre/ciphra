/**
package com.example.uidesign_cistercian.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;

public class CistercianNumeralView extends View {

    private Paint activeSegmentPaint;
    private Paint inactiveSegmentPaint;

    private final RectF[] segments = new RectF[20];
    private final boolean[] segmentActives = new boolean[20];

                /*
                public CistercianNumeralView(Context context, @Nullable AttributeSet attrs) {
                    super(context, attrs);
                    init();
                }
                **/

/**
    public CistercianNumeralView(Context context) {
        super(context);
        init();
    }

    public CistercianNumeralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CistercianNumeralView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activeSegmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeSegmentPaint.setColor(Color.BLACK); // Active color
        activeSegmentPaint.setStyle(Paint.Style.STROKE);
        activeSegmentPaint.setStrokeWidth(10);

        inactiveSegmentPaint = new Paint(activeSegmentPaint);
        inactiveSegmentPaint.setColor(Color.LTGRAY); // Inactive color

        // Preallocate RectF objects for segments
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new RectF();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Calculate the center of the view
        float centerX = getWidth() / 2.0f;
        float centerY = getHeight() / 2.0f;
        float segmentLength = 50.0f; // Replace with your actual segment length

        // Define the RectF bounds for the segments
        if(segments[0] == null) {
            segments[0].set(centerX, centerY + (segmentLength * 1.5f), centerX + segmentLength, centerY + (segmentLength * 1.5f));
            segments[1].set(centerX, centerY + (segmentLength * 0.5f), centerX + segmentLength, centerY + (segmentLength * 0.5f));
            segments[2].set(centerX, centerY + (segmentLength * 1.5f), centerX + segmentLength, centerY + (segmentLength * 0.5f));
            segments[3].set(centerX, centerY + (segmentLength * 0.5f), centerX + segmentLength, centerY + (segmentLength * 1.5f));
            segments[4].set(centerX + segmentLength, centerY + (segmentLength * 1.5f), centerX + segmentLength, centerY + (segmentLength * 0.5f));

            segments[5].set(centerX, centerY + (segmentLength * 1.5f), centerX - segmentLength, centerY + (segmentLength * 1.5f));
            segments[6].set(centerX, centerY + (segmentLength * 0.5f), centerX - segmentLength, centerY + (segmentLength * 0.5f));
            segments[7].set(centerX, centerY + (segmentLength * 1.5f), centerX - segmentLength, centerY + (segmentLength * 0.5f));
            segments[8].set(centerX, centerY + (segmentLength * 0.5f), centerX - segmentLength, centerY + (segmentLength * 1.5f));
            segments[9].set(centerX - segmentLength, centerY + (segmentLength * 1.5f), centerX - segmentLength, centerY + (segmentLength * 0.5f));

            segments[10].set(centerX, centerY - (segmentLength * 1.5f), centerX + segmentLength, centerY - (segmentLength * 1.5f));
            segments[11].set(centerX, centerY - (segmentLength * 0.5f), centerX + segmentLength, centerY - (segmentLength * 0.5f));
            segments[12].set(centerX, centerY - (segmentLength * 1.5f), centerX + segmentLength, centerY - (segmentLength * 0.5f));
            segments[13].set(centerX, centerY - (segmentLength * 0.5f), centerX + segmentLength, centerY - (segmentLength * 1.5f));
            segments[14].set(centerX + segmentLength, centerY - (segmentLength * 1.5f), centerX + segmentLength, centerY - (segmentLength * 0.5f));

            segments[15].set(centerX, centerY - (segmentLength * 1.5f), centerX - segmentLength, centerY - (segmentLength * 1.5f));
            segments[16].set(centerX, centerY - (segmentLength * 0.5f), centerX - segmentLength, centerY - (segmentLength * 0.5f));
            segments[17].set(centerX, centerY - (segmentLength * 1.5f), centerX - segmentLength, centerY - (segmentLength * 0.5f));
            segments[18].set(centerX, centerY - (segmentLength * 0.5f), centerX - segmentLength, centerY - (segmentLength * 1.5f));
            segments[19].set(centerX - segmentLength, centerY - (segmentLength * 1.5f), centerX - segmentLength, centerY - (segmentLength * 0.5f));
        }

        // Draw the central stem
        canvas.drawLine(centerX, centerY - (segmentLength * 1.5f), centerX, centerY + (segmentLength * 1.5f), activeSegmentPaint);

        //Draw segments
        for (int i = 0; i < segments.length; i++) {
            Paint segmentPaint = segmentActives[i] ? activeSegmentPaint : inactiveSegmentPaint;
            canvas.drawRect(segments[i], segmentPaint);
        }
    }

    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClickListener if it is defined
        super.performClick();

        // Handle the action for the custom view
        // ...

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            for (int i = 0; i < segments.length; i++) {
                if (segments[i].contains(touchX, touchY)) {
                    segmentActives[i] = !segmentActives[i];
                    invalidate();
                    return true;
                }
            }
            performClick(); // Call this method to handle the click action
            return true;
        }
        return super.onTouchEvent(event);
    }
}
*/
