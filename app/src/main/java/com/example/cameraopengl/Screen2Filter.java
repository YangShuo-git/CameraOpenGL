package com.example.cameraopengl;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.example.cameraopengl.R;

public class Screen2Filter extends BaseFilter {
    public Screen2Filter(Context mContext) {
        super(mContext, R.raw.screen_vert, R.raw.screen_frag);
    }
}
