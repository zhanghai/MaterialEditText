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

    @Bind(R.id.edit1)
    EditText edit1;
    @Bind(R.id.edit2)
    EditText edit2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        edit1.setBackground(new EditTextBackgroundDrawable(this));
        edit2.setBackground(new EditTextBackgroundDrawable(this));
    }
}
