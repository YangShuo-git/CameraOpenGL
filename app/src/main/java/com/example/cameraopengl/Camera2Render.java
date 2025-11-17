package com.example.cameraopengl;


import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera2Render implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Camera2Helper.OnPreviewSizeListener, Camera2Helper.OnPreviewListener {

    private static final String TAG = "Camera2Render";
    private CameraGLView mCameraGLView;
    private Camera2Helper mCamera2Helper;
    private Screen2Filter screenFilter;
    private Camera2Filter cameraFilter;
    private SurfaceTexture mSurfaceTexture;
    private  int[] mTextures;
    float[] mtx = new float[16];

    private int mPreviewWdith;
    private int mPreviewHeight;

    private int screenSurfaceWid;
    private int screenSurfaceHeight;
    private int screenX;
    private int screenY;
    public Camera2Render(CameraGLView cameraGLView) {
        mCameraGLView = cameraGLView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCamera2Helper = new Camera2Helper((Activity) mCameraGLView.getContext());
        mTextures = new int[1];
        //创建一个纹理
        GLES20.glGenTextures(mTextures.length, mTextures, 0);

        mSurfaceTexture = new SurfaceTexture(mTextures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        //使用fbo 将samplerExternalOES 输入到sampler2D中
        cameraFilter = new Camera2Filter(mCameraGLView.getContext());
        //负责将图像绘制到屏幕上
        screenFilter = new Screen2Filter(mCameraGLView.getContext());
        Log.i(TAG, "onSurfaceCreated finished");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCamera2Helper.setOnPreviewListener(this);
        mCamera2Helper.setPreviewSizeListener(this);

        //打开相机，传入SurfaceTexture，相机会将预览数据给到该SurfaceTexture
        try {
            mCamera2Helper.openCamera(width, height, mSurfaceTexture);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

        float scaleX = (float) mPreviewHeight / (float) width;
        float scaleY = (float) mPreviewWdith / (float) height;

        float max = Math.max(scaleX, scaleY);

        screenSurfaceWid = (int) (mPreviewHeight / max);
        screenSurfaceHeight = (int) (mPreviewWdith / max);
        screenX = width - (int) (mPreviewHeight / max);
        screenY = height - (int) (mPreviewWdith / max);

        //prepare 传如 绘制到屏幕上的宽 高 起始点的X坐标 起使点的Y坐标
        cameraFilter.prepare(screenSurfaceWid, screenSurfaceHeight, screenX, screenY);
        screenFilter.prepare(screenSurfaceWid, screenSurfaceHeight, screenX, screenY);
        Log.i(TAG, "onSurfaceChanged finished");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        int textureId;
        // 配置屏幕
        //清理屏幕 :告诉opengl 需要把屏幕清理成什么颜色
        GLES20.glClearColor(0, 0, 0, 0);
        //执行上一个：glClearColor配置的屏幕颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //更新获取一张图
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mtx);

        //cameraFiler需要一个矩阵，是SurfaceTexture和手机屏幕的一个坐标之间的关系
        cameraFilter.setMatrix(mtx);
        textureId = cameraFilter.onDrawFrame(mTextures[0]);
        screenFilter.onDrawFrame(textureId);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mCameraGLView.requestRender();
    }

    @Override
    public void onSize(int width, int height) {
        mPreviewWdith = width;
        mPreviewHeight = height;
        Log.i(TAG, "mPreviewWdith:" + mPreviewWdith);
        Log.i(TAG, "mPreviewHeight:" + mPreviewHeight);
    }

    @Override
    public void onPreviewFrame(byte[] data, int len) {
    }
}
