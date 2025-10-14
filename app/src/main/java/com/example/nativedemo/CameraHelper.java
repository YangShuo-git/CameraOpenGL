package com.example.nativedemo;

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

        CameraX.bindToLifecycle(lifecycleOwner, getPreView());
    }
    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合给的参数，设置一个最为接近的分辨率
        // 得到camera的数据
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(frontFacing) //前置或者后置摄像头
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(listener);
        return preview;
    }
}
