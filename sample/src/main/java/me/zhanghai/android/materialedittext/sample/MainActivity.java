/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext.sample;

import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialedittext.EditTextBackgroundDrawable;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.normal_edit)
    EditText mNormalEdit;
    @Bind(R.id.error_edit_layout)
    TextInputLayout mErrorEditLayout;
    @Bind(R.id.error_edit)
    EditText mErrorEdit;
    @Bind(R.id.disabled_edit)
    EditText mDisabledEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        mNormalEdit.setBackground(new EditTextBackgroundDrawable(this));
        mErrorEdit.setBackground(new EditTextBackgroundDrawable(this));
        mErrorEditLayout.setError("An error occurred");
        mDisabledEdit.setBackground(new EditTextBackgroundDrawable(this));
    }
}
