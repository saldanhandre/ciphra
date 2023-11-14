package com.example.uidesign_cistercian.ui.components;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class TouchDelegateImageView extends androidx.appcompat.widget.AppCompatImageView {

    private Rect touchableArea = new Rect();
    private TouchDelegate touchDelegate;

    public TouchDelegateImageView(Context context) {
        super(context);
    }

    public TouchDelegateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchDelegateImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTouchableArea(Rect area) {
        touchableArea.set(area);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Assuming you've already defined the area based on the image's visible parts
        // and considering that the ImageView might be larger than the actual touchable area.
        touchDelegate = new TouchDelegate(touchableArea, this);
        // Parent view needs to be the delegate for the touch events
        if (getParent() instanceof View) {
            ((View) getParent()).setTouchDelegate(touchDelegate);
        }
    }
}
