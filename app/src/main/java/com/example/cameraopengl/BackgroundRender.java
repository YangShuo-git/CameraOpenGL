package com.example.cameraopengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BackgroundRender implements GLSurfaceView.Renderer {
    private String TAG = BackgroundRender.class.getSimpleName();

    // 系统调用这个方法一次创建时GLSurfaceView,使用此方法来执行只需要发生一次的操作，比如设置OpenGL的环境参数或初始化的OpenGL图形对象
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // 清理之前存留的数据
        GLES20.glClearColor(0.0f, 0.0f, 0.0f,1.0f);
    }

    // 系统调用此方法时的GLSurfaceView几何形状的变化，包括尺寸变化GLSurfaceView或设备屏幕的取向
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // 渲染窗口大小发生改变或者屏幕方法发生变化时候回调
        GLES20.glViewport(0,0,width,height);
    }

    // 系统调用上的每个重绘此方法GLSurfaceView,使用此方法作为主要执行点用于绘制（和重新绘制）的图形对象
    @Override
    public void onDrawFrame(GL10 gl10) {
        //执行渲染工作   两种模式：16ms、手动刷新
        //Redraw background color
        GLES20.glClearColor(0f,1.0f,0f,0f);
    }
}