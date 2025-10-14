package com.example.nativedemo;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraRender";
    private CameraHelper cameraHelper;
    private CameraView cameraView;
    private ScreenFilter screenFilter;
    private SurfaceTexture mCameraTexure;
    private  int[] textures;
    float[] mtx = new float[16];
    public CameraRender(CameraView cameraView) {
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        //  打开摄像头
        cameraHelper = new CameraHelper(lifecycleOwner, this);
    }

    // 监听画布创建完成
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 准备好摄像头绘制的画布; 通过gl创建一个纹理id
        textures = new int[1];
        // 让 SurfaceTexture 与 Gpu（OpenGL）共享一个数据源  0-31  让摄像头的数据和Opengl的数据进行共享
        mCameraTexure.attachToGLContext(textures[0]);
        // 监听摄像头数据回调
        mCameraTexure.setOnFrameAvailableListener(this);

        // 必须要在glThread中进行初始化
        screenFilter = new ScreenFilter(cameraView.getContext());
    }

    // 监听画布改变
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screenFilter.setSize(width,height);
    }

    // 渲染画画
    @Override
    public void onDrawFrame(GL10 gl) {
        // 重新渲染 会调用该接口
        //Log.i(TAG, "onDrawFrame 线程: " + Thread.currentThread().getName());
        // 把摄像头的数据先输出来  更新纹理  SufaceTexture  给了GPU
        mCameraTexure.updateTexImage();
        // 获得变换矩阵
        mCameraTexure.getTransformMatrix(mtx);
        screenFilter.setTransformMatrix(mtx);

        screenFilter.onDraw(textures[0]);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        // 摄像头预览到的数据 在这里
        mCameraTexure = output.getSurfaceTexture();
        Log.i(TAG, "onUpdated: ");
    }

    // 监听到有一个可用帧时
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // 当有数据过来的时候 进行手动刷新 即当有一个可用帧时，就调用requestRender()
        cameraView.requestRender();
        //Log.i(TAG, "onFrameAvailable");
    }
}