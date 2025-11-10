package com.example.cameraopengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

// GLSurfaceView   代码运行在 glthread线程
public class CameraView extends GLSurfaceView {
    private  CameraRender renderer;
    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 1.配置EGL的版本（必须设置）
        setEGLContextClientVersion(2);
        // 2.设置渲染器
        renderer = new CameraRender(this);
        setRenderer(renderer);
        /**
         *  刷新方式：
         *  RENDERMODE_WHEN_DIRTY   手动刷新（按需），调用requestRender(); 效率高一点
         *  RENDERMODE_CONTINUOUSLY 自动刷新（连续），大概16ms自动回调一次onDrawFrame()
         */
        // 3.设置渲染模式
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
