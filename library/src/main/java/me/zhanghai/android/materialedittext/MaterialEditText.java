/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import me.zhanghai.android.materialedittext.internal.ViewCompat;

public class MaterialEditText extends AppCompatEditText {

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
        ViewCompat.setBackground(this, new MaterialEditTextBackgroundDrawable(getContext()));
    }
}
