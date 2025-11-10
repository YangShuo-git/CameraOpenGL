package com.example.cameraopengl;

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
        // 创建OpenGL纹理对象 textures
        textures = new int[1];
        // 将 CameraX 的 SurfaceTexture与 OpenGL 纹理关联起来，使得摄像头数据可以直接被 GPU 使用
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

    // 渲染画画，不断地调用
    @Override
    public void onDrawFrame(GL10 gl) {
        // 重新渲染 会不断调用该接口
        //Log.i(TAG, "onDrawFrame 线程: " + Thread.currentThread().getName());  //会一直打印
        // 将最新的摄像头图像数据更新到之前关联的OpenGL纹理中，并设置变换矩阵
        mCameraTexure.updateTexImage();
        mCameraTexure.getTransformMatrix(mtx);

        // 传递变换矩阵和纹理 ID
        screenFilter.setTransformMatrix(mtx);
        screenFilter.onDraw(textures[0]);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        // 获取来自 CameraX 的预览数据流（SurfaceTexture）
        // 这是数据流的起点
        Log.i(TAG, "PreviewOutput，onUpdated");
        mCameraTexure = output.getSurfaceTexture();
    }

    /*
    每当摄像头有新的帧数据填入 SurfaceTexture，此回调就会被触发。
    它手动调用 cameraView.requestRender()，驱动 OpenGL 渲染下一帧
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.i(TAG, "onFrameAvailable"); //会不断打印
        // 当有数据过来的时候 进行手动刷新RENDERMODE_WHEN_DIRTY； 即当有一个可用帧时，就调用requestRender()
        cameraView.requestRender();
    }
}

/*
将摄像头的preview给到surfacetexture；
再将surfacetexture与opengl的纹理绑定一起；
这样Gpu就可以渲染数据

onFrameAvailable是生产者（如相机）的通知，可能发生在任意线程；
    通过 requestRender()驱动opengl，确保了线程安全；
onDrawFrame是消费者（OpenGL）的操作，始终在专用的 GLThread中执行；
 */