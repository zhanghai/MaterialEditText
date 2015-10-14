/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import me.zhanghai.android.materialedittext.internal.ViewCompat;

public class MaterialTextInputLayout extends TextInputLayout {

    private MaterialEditTextBackgroundDrawable mEditTextBackground;

    public MaterialTextInputLayout(Context context) {
        super(context);
    }

    public MaterialTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaterialTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (child instanceof MaterialEditText) {
            // Just throw a ClassCastException if the background of MaterialEditText is not the one
            // automatically set.
            mEditTextBackground = (MaterialEditTextBackgroundDrawable) child.getBackground();
        } else if (child instanceof EditText) {
            mEditTextBackground = new MaterialEditTextBackgroundDrawable(getContext());
            ViewCompat.setBackground(child, mEditTextBackground);
        }
    }

    @Override
    public void setError(CharSequence error) {
        super.setError(error);

        if (mEditTextBackground != null) {
            mEditTextBackground.setError(!TextUtils.isEmpty(error));
        }
    }
}
