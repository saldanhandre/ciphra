package com.example.uidesign_cistercian;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CistercianThumbnailView extends View {
    private int number = 0; // Default number

    public CistercianThumbnailView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setNumber(int number) {
        this.number = number;
        invalidate(); // Redraw the view with the new number
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int units = number % 10;
        int tens = (number % 100) / 10;
        int hundreds = (number % 1000) / 100;
        int thousands = number / 1000;
        int segmentLength = width/3;

        // Set up paint
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);

        // Central stem for all numbers
        float centerX = width / 2f;
        canvas.drawLine(centerX, 0, centerX, height, paint);

        switch(units) {
            case 1:
                canvas.drawLine(centerX, 2, centerX + segmentLength, 2, paint);
                break;
            case 2:
                canvas.drawLine(centerX, segmentLength, centerX + segmentLength, segmentLength, paint);
                break;
            case 3:
                Path path3 = new Path();
                path3.moveTo(centerX, 0);
                path3.lineTo(centerX + segmentLength, segmentLength);
                canvas.drawPath(path3, paint);
                break;
            case 4:
                Path path4 = new Path();
                path4.moveTo(centerX, segmentLength);
                path4.lineTo(centerX + segmentLength, 0);
                canvas.drawPath(path4, paint);
                break;
            case 5:
                canvas.drawLine(centerX, 2, centerX + segmentLength, 2, paint);
                Path path5 = new Path();
                path5.moveTo(centerX, segmentLength);
                path5.lineTo(centerX + segmentLength, 0);
                canvas.drawPath(path5, paint);
                break;
            case 6:
                canvas.drawLine(centerX + segmentLength, 0, centerX + segmentLength, segmentLength, paint);
                break;
            case 7:
                canvas.drawLine(centerX, 2, centerX + segmentLength, 2, paint);
                canvas.drawLine(centerX + segmentLength, 0, centerX + segmentLength, segmentLength, paint);
                break;
            case 8:
                canvas.drawLine(centerX, segmentLength, centerX + segmentLength, segmentLength, paint);
                canvas.drawLine(centerX + segmentLength, 0, centerX + segmentLength, segmentLength, paint);
                break;
            case 9:
                canvas.drawLine(centerX, 2, centerX + segmentLength, 2, paint);
                canvas.drawLine(centerX, segmentLength, centerX + segmentLength, segmentLength, paint);
                canvas.drawLine(centerX + segmentLength, 0, centerX + segmentLength, segmentLength, paint);
                break;
        }
        switch(tens) {
            case 1:
                canvas.drawLine(centerX, 2, centerX - segmentLength, 2, paint);
                break;
            case 2:
                canvas.drawLine(centerX, segmentLength, centerX - segmentLength, segmentLength, paint);
                break;
            case 3:
                Path path3 = new Path();
                path3.moveTo(centerX, 0);
                path3.lineTo(centerX - segmentLength, segmentLength);
                canvas.drawPath(path3, paint);
                break;
            case 4:
                Path path4 = new Path();
                path4.moveTo(centerX, segmentLength);
                path4.lineTo(centerX - segmentLength, 0);
                canvas.drawPath(path4, paint);
                break;
            case 5:
                canvas.drawLine(centerX, 2, centerX - segmentLength, 2, paint);
                Path path5 = new Path();
                path5.moveTo(centerX, segmentLength);
                path5.lineTo(centerX - segmentLength, 0);
                canvas.drawPath(path5, paint);
                break;
            case 6:
                canvas.drawLine(centerX - segmentLength, 0, centerX - segmentLength, 0 + segmentLength, paint);
                break;
            case 7:
                canvas.drawLine(centerX, 2, centerX - segmentLength, 2, paint);
                canvas.drawLine(centerX - segmentLength, 0, centerX - segmentLength, 0 + segmentLength, paint);
                break;
            case 8:
                canvas.drawLine(centerX, segmentLength, centerX - segmentLength, segmentLength, paint);
                canvas.drawLine(centerX - segmentLength, 0, centerX - segmentLength, segmentLength, paint);
                break;
            case 9:
                canvas.drawLine(centerX, 2, centerX - segmentLength, 2, paint);
                canvas.drawLine(centerX, segmentLength, centerX - segmentLength, segmentLength, paint);
                canvas.drawLine(centerX - segmentLength, 0, centerX - segmentLength, segmentLength, paint);
                break;
        }
        switch(hundreds) {
            case 1:
                canvas.drawLine(centerX, height-2, centerX + segmentLength, height-2, paint);
                break;
            case 2:
                canvas.drawLine(centerX, height - segmentLength, centerX + segmentLength, height - segmentLength, paint);
                break;
            case 3:
                Path path3 = new Path();
                path3.moveTo(centerX, height);
                path3.lineTo(centerX + segmentLength, height - segmentLength);
                canvas.drawPath(path3, paint);
                break;
            case 4:
                Path path4 = new Path();
                path4.moveTo(centerX, height - segmentLength);
                path4.lineTo(centerX + segmentLength, height);
                canvas.drawPath(path4, paint);
                break;
            case 5:
                canvas.drawLine(centerX, height-2, centerX + segmentLength, height-2, paint);
                Path path5 = new Path();
                path5.moveTo(centerX, height - segmentLength);
                path5.lineTo(centerX + segmentLength, height);
                canvas.drawPath(path5, paint);
                break;
            case 6:
                canvas.drawLine(centerX + segmentLength, height, centerX + segmentLength, height - segmentLength, paint);
                break;
            case 7:
                canvas.drawLine(centerX, height-2, centerX + segmentLength, height-2, paint);
                canvas.drawLine(centerX + segmentLength, height, centerX + segmentLength, height - segmentLength, paint);
                break;
            case 8:
                canvas.drawLine(centerX, height - segmentLength, centerX + segmentLength, height - segmentLength, paint);
                canvas.drawLine(centerX + segmentLength, height, centerX + segmentLength, height - segmentLength, paint);
                break;
            case 9:
                canvas.drawLine(centerX, height-2, centerX + segmentLength, height-2, paint);
                canvas.drawLine(centerX, height - segmentLength, centerX + segmentLength, height - segmentLength, paint);
                canvas.drawLine(centerX + segmentLength, height, centerX + segmentLength, height - segmentLength, paint);
                break;
        }
        switch(thousands) {
            case 1:
                canvas.drawLine(centerX, height-2, centerX - segmentLength, height-2, paint);
                break;
            case 2:
                canvas.drawLine(centerX, height - segmentLength, centerX - segmentLength, height - segmentLength, paint);
                break;
            case 3:
                Path path3 = new Path();
                path3.moveTo(centerX, height);
                path3.lineTo(centerX - segmentLength, height - segmentLength);
                canvas.drawPath(path3, paint);
                break;
            case 4:
                Path path4 = new Path();
                path4.moveTo(centerX, height - segmentLength);
                path4.lineTo(centerX - segmentLength, height);
                canvas.drawPath(path4, paint);
                break;
            case 5:
                canvas.drawLine(centerX, height-2, centerX - segmentLength, height-2, paint);
                Path path5 = new Path();
                path5.moveTo(centerX, height - segmentLength);
                path5.lineTo(centerX - segmentLength, height);
                canvas.drawPath(path5, paint);
                break;
            case 6:
                canvas.drawLine(centerX - segmentLength, height, centerX - segmentLength, height - segmentLength, paint);
                break;
            case 7:
                canvas.drawLine(centerX, height-2, centerX - segmentLength, height-2, paint);
                canvas.drawLine(centerX - segmentLength, height, centerX - segmentLength, height - segmentLength, paint);
                break;
            case 8:
                canvas.drawLine(centerX, height - segmentLength, centerX - segmentLength, height - segmentLength, paint);
                canvas.drawLine(centerX - segmentLength, height, centerX - segmentLength, height - segmentLength, paint);
                break;
            case 9:
                canvas.drawLine(centerX, height-2, centerX - segmentLength, height-2, paint);
                canvas.drawLine(centerX, height - segmentLength, centerX - segmentLength, height - segmentLength, paint);
                canvas.drawLine(centerX - segmentLength, height, centerX - segmentLength, height - segmentLength, paint);
                break;
        }
    }
}
