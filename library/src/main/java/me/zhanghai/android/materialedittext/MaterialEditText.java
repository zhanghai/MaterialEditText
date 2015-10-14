/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;

import me.zhanghai.android.materialedittext.internal.ViewCompat;

/**
 * An {@code AppCompatEditText} that automatically sets its background to
 * {@link MaterialEditTextBackgroundDrawable} and calls {@link Drawable#setHotspot} for it on
 * platforms prior to {@link Build.VERSION_CODES#LOLLIPOP}.
 */
public class MaterialEditText extends AppCompatEditText {

    private MaterialEditTextBackgroundDrawable mBackground;

    public MaterialEditText(Context context) {
        super(context);

        init();
    }

    public MaterialEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public MaterialEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        mBackground = new MaterialEditTextBackgroundDrawable(getContext());
        ViewCompat.setBackground(this, mBackground);
    }

    @Override
    @SuppressLint("NewApi")
    public boolean onTouchEvent(MotionEvent event) {
        boolean consumed = super.onTouchEvent(event);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            int action = event.getAction();
            if (isEnabled() && (isClickable() || isLongClickable())
                    && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
                mBackground.setHotspot(event.getX(), event.getY());
            }
        }

        return consumed;
    }
}
