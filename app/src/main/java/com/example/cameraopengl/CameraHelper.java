package com.example.cameraopengl;

import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

// 该类封装了对Camera的一系列操作
public class CameraHelper {
    private HandlerThread handlerThread;
    private CameraX.LensFacing backFacing = CameraX.LensFacing.BACK;
    private CameraX.LensFacing frontFacing = CameraX.LensFacing.FRONT;
    private Preview.OnPreviewOutputUpdateListener listener;


    public CameraHelper(LifecycleOwner lifecycleOwner, Preview.OnPreviewOutputUpdateListener listener) {
        // camerax打开相机
        this.listener = listener;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();

        //将相机预览与传入的 LifecycleOwner（通常是一个 Activity或 Fragment）的生命周期绑定在一起。
        // 这意味着当界面可见时相机自动开启，界面销毁时相机自动释放，有效避免了资源泄漏
        CameraX.bindToLifecycle(lifecycleOwner, getPreView());
    }
    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合给的参数，设置一个最为接近的分辨率
        // 得到camera的数据
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(frontFacing) //前置或者后置摄像头
                .build();

        // 设置preview
        Preview preview = new Preview(previewConfig);
        // 会在预览就绪和输出更新时被调用
        // 在回调方法中，可以从 PreviewOutput参数里获取到 SurfaceTexture，进而得到每一帧的预览图像数据，用于显示或进一步处理
        preview.setOnPreviewOutputUpdateListener(listener);
        return preview;
    }
}
