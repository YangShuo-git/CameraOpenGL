package com.example.nativedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera2Helper {
    private Context mContext;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;

    private String frontCameraId;
    private String backCameraId;
    private Preview.OnPreviewOutputUpdateListener listener;
    private SurfaceTexture previewSurfaceTexture;

    // 相机状态回调
    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    // 相机捕获会话状态回调
    private final CameraCaptureSession.StateCallback captureSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;

                    cameraCaptureSession = session;
                    try {
                        // 设置连续自动对焦模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        // 开始预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        cameraCaptureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // 配置失败处理
                }
            };

    public Camera2Helper(Context context, LifecycleOwner lifecycleOwner,
                         Preview.OnPreviewOutputUpdateListener listener) {
        this.listener = listener;
        mContext = context;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        // 创建后台线程
        handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        // 绑定生命周期
        if (lifecycleOwner instanceof ComponentActivity) {
            ((ComponentActivity) lifecycleOwner).getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        closeCamera();
                        handlerThread.quitSafely();
                    }
                }
            });
        }

        // 初始化相机
        initializeCamera();
    }

    private void initializeCamera() {
        backgroundHandler.post(() -> {
            try {
                // 获取相机ID列表
                String[] cameraIdList = cameraManager.getCameraIdList();

                for (String cameraId : cameraIdList) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (lensFacing != null) {
                        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            frontCameraId = cameraId;
                        } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                            backCameraId = cameraId;
                        }
                    }
                }

                // 默认使用前置摄像头
                openCamera(frontCameraId != null ? frontCameraId : backCameraId);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        });
    }

    private void openCamera(String cameraId) {
        try {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 处理权限问题
                return;
            }
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        if (cameraDevice == null || previewSurfaceTexture == null) return;

        try {
            // 设置SurfaceTexture的默认大小
            previewSurfaceTexture.setDefaultBufferSize(640, 480);
            Surface previewSurface = new Surface(previewSurfaceTexture);

            // 创建预览请求构建器
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);

            // 创建相机捕获会话
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    captureSessionCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setPreviewSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.previewSurfaceTexture = surfaceTexture;

        if (cameraDevice != null) {
            createCameraPreviewSession();
        }
    }

    public void switchCamera() {
        backgroundHandler.post(() -> {
            if (frontCameraId != null && backCameraId != null) {
                String currentCameraId = (cameraDevice != null) ?
                        (getCurrentCameraId().equals(frontCameraId) ? backCameraId : frontCameraId) : frontCameraId;
                openCamera(currentCameraId);
            }
        });
    }

    private String getCurrentCameraId() {
        try {
            if (cameraDevice != null) {
                // 通过相机设备获取ID（需要API级别28+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    return cameraDevice.getId();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frontCameraId;
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (previewSurfaceTexture != null) {
            previewSurfaceTexture.release();
            previewSurfaceTexture = null;
        }
    }

    // 获取相机特性
    public CameraCharacteristics getCameraCharacteristics(String cameraId) {
        try {
            return cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取支持的预览尺寸
    public List<Size> getSupportedPreviewSizes(String cameraId) {
        List<Size> supportedSizes = new ArrayList<>();
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                android.util.Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                for (android.util.Size size : sizes) {
                    supportedSizes.add(new Size(size.getWidth(), size.getHeight()));
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return supportedSizes;
    }
}