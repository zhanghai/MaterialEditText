/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialedittext.EditTextBackgroundDrawable;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.normal_edit)
    EditText normalEdit;
    @Bind(R.id.error_edit)
    EditText errorEdit;
    @Bind(R.id.disabled_edit)
    EditText disabldEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        normalEdit.setBackground(new EditTextBackgroundDrawable(this));
        //errorEdit.setBackground(new EditTextBackgroundDrawable(this));
        errorEdit.setError("Error");
        disabldEdit.setBackground(new EditTextBackgroundDrawable(this));
    }
}
