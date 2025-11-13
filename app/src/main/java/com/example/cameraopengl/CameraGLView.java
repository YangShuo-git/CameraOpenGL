package com.example.cameraopengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

// GLSurfaceView   代码运行在 glthread线程
public class CameraGLView extends GLSurfaceView {
    private  CameraRender renderer;
    private  Camera2Render renderer2;
    private GLSurfaceView glSurfaceView;
    public CameraGLView(Context context) {
        super(context);
    }

    public CameraGLView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 1.配置EGL的版本
        setEGLContextClientVersion(2);
        // 2.设置渲染器
        //renderer = new CameraRender(this);
        renderer2 = new Camera2Render(this);
        setRenderer(renderer2);
        /**
         *  刷新方式：
         *  RENDERMODE_WHEN_DIRTY   手动刷新（按需），调用requestRender(); 效率高一点
         *  RENDERMODE_CONTINUOUSLY 自动刷新（连续），大概16ms自动回调一次onDrawFrame()
         */
        // 3.设置渲染模式
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
