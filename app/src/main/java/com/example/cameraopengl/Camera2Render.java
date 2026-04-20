package com.example.cameraopengl;


import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera2Render implements GLSurfaceView.Renderer {
    private static final String TAG = "Camera2Render";
    private CameraGLView mCameraGLView;
    private Camera2Helper mCamera2Helper;
    private Screen2Filter mScreen2Filter;
    private Camera2Filter mCamera2Filter;
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

    // GLSurfaceView.Renderer的三个回调
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCamera2Helper = new Camera2Helper((Activity) mCameraGLView.getContext());

        //创建纹理 用于连接camera和opengl
        mTextures = new int[1];
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        mSurfaceTexture = new SurfaceTexture(mTextures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mCameraGLView.requestRender();
            }
        });

        //使用FBO 将samplerExternalOES 输入到sampler2D中
        mCamera2Filter = new Camera2Filter(mCameraGLView.getContext());
        //将图像绘制到屏幕上
        mScreen2Filter = new Screen2Filter(mCameraGLView.getContext());
        Log.i(TAG, "onSurfaceCreated finished");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCamera2Helper.setOnPreviewListener(new Camera2Helper.OnPreviewListener() {
            @Override
            public void onPreviewFrame(byte[] data, int len) {

            }
        });
        mCamera2Helper.setPreviewSizeListener(new Camera2Helper.OnPreviewSizeListener() {
            @Override
            public void onSize(int width, int height) {
                mPreviewWdith = width;
                mPreviewHeight = height;
            }
        });

        //打开相机，传入SurfaceTexture，相机会将预览数据给到该SurfaceTexture
        try {
            mCamera2Helper.openCamera(width, height, mSurfaceTexture);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

        float scaleX = (float) mPreviewHeight / (float) width;
        float scaleY = (float) mPreviewWdith / (float) height;
        float max    = Math.max(scaleX, scaleY);
        screenSurfaceWid    = (int) (mPreviewHeight / max);
        screenSurfaceHeight = (int) (mPreviewWdith / max);
        screenX = width - (int) (mPreviewHeight / max);
        screenY = height - (int) (mPreviewWdith / max);

        //prepare 绘制到屏幕上的宽 高 起始点的X坐标、Y坐标
        mCamera2Filter.prepare(screenSurfaceWid, screenSurfaceHeight, screenX, screenY);
        mScreen2Filter.prepare(screenSurfaceWid, screenSurfaceHeight, screenX, screenY);
        Log.i(TAG, "onSurfaceChanged finished");
    }

    //每次渲染一帧都会被调用
    @Override
    public void onDrawFrame(GL10 gl) {
        //1、清理屏幕
        //glClearColor告诉opengl需要把屏幕清理成什么颜色
        //glClear 实际执行清除操作，将颜色缓冲区（即屏幕显示内容）填充为上述颜色，避免上一帧的残留
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //2、获取最新图像帧并更新纹理
        //从SurfaceTexture内部队列中取出最新的一帧图像数据，
        //更新到SurfaceTexture绑定的 OpenGL 纹理（即 mTextures[0]）上
        //这一步是将相机采集到的实时图像“贴”到 GPU 纹理中的关键操作
        mSurfaceTexture.updateTexImage();

        //若不使用 getTransformMatrix 的矩阵，相机预览画面可能会出现方向错误（倒置、旋转90°）或比例失调。
        //正确应用该矩阵可保证画面与屏幕显示方向一致
        mSurfaceTexture.getTransformMatrix(mtx);
        mCamera2Filter.setMatrix(mtx);

        //3、应用相机滤镜
        //输入：原始纹理id（mTextures[0]，包含最新的相机帧）。
        //处理：对原始图像施加某种滤镜效果（例如颜色调整、模糊、边缘检测等）。具体效果取决于 mCamera2Filter 的实现（可能是自定义的 OpenGL 着色器程序）。
        //输出：返回一个新的纹理id（textureId），这个纹理上存储的是经过滤镜处理后的图像。
        //常见的滤镜链设计：mCamera2Filter 负责将输入纹理绘制到一个离屏的帧缓冲对象（FBO）中，并返回该 FBO 绑定的纹理 ID
        int textureId = mCamera2Filter.onDrawFrame(mTextures[0]);

        //4、屏幕渲染
        //将滤镜处理后的最终纹理（textureId）绘制到屏幕上。
        //将纹理渲染到一个覆盖全屏的矩形（两个三角形组成）。
        //可选的额外处理：伽马校正、颜色空间转换、屏幕适配（如保持宽高比，黑边填充或裁剪）。
        //最后调用 eglSwapBuffers（由 GLSurfaceView 自动完成）将渲染结果显示到屏幕上。
        //注意：这里没有再次返回纹理 ID，因为不需要继续传递下去。
        mScreen2Filter.onDrawFrame(textureId);
    }
}

/*
驱动方式：
SurfaceTexture.OnFrameAvailableListener 监听到新帧后，触发 GLSurfaceView 的渲染请求，在 onDrawFrame 中完成一帧的更新和处理。

相机硬件
   ↓ (图像流)
SurfaceTexture (内部自动更新纹理 mTextures[0])
   ↓ (onFrameAvailable 触发)
mCameraGLView.requestRender()
   ↓ (渲染线程执行 onDrawFrame)
updateTexImage() → 获取最新帧到 mTextures[0]
getTransformMatrix(mtx)
mCamera2Filter.setMatrix(mtx)
mCamera2Filter.onDrawFrame(mTextures[0]) → 输出纹理 tex1
mScreen2Filter.onDrawFrame(tex1) → 绘制到屏幕
   ↓
用户看到带有滤镜的实时画面
 */
