package com.example.cameraopengl;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera2Helper {

    private final String TAG = "Camera2Helper";
    private Activity mContext;
    private String mCameraId;
    private ImageReader mImageReader;
    private OnPreviewListener mOnPreviewListener;
    private OnPreviewSizeListener mOnPreviewSizeListener;

    public Camera2Helper(Activity context) {
        mContext = context;
    }

    private Size mPreviewSize;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private SurfaceTexture mSurfaceTexture;

    private CameraDevice mCameraDevice;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest mPreviewRequest;

    public interface OnPreviewListener {
        void onPreviewFrame(byte[] data, int len);
    }

    public interface OnPreviewSizeListener {
        void onSize(int width, int height);
    }

    public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
        mOnPreviewListener = onPreviewListener;
    }

    public void setPreviewSizeListener(OnPreviewSizeListener onPreviewSizeListener) {
        mOnPreviewSizeListener = onPreviewSizeListener;
    }


    public void openCamera(int width, int height,
                           SurfaceTexture surfaceTexture) throws CameraAccessException {
        mSurfaceTexture = surfaceTexture;
        //A1、打开相机前，需要确定回调线程、配置相机输出
        startBackgroundThread();
        setCameraOutputs(width, height);

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.checkSelfPermission(Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            // mStateCallback为相机的状态回调，mBackgroundHandler是Callback执行的线程，为null就在当前线程
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                //获取此id对应摄像头的参数
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //选择摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                mCameraId = cameraId;

                //用来管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap streamConfigMap = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigMap == null) {
                    continue;
                }
                //传入的期望预览尺寸
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                //屏幕物理尺寸，作为上限
                Point displaySize = new Point();
                mContext.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                //从相机支持的YUV_420_888格式中找出面积最大的尺寸
                Size largest = Collections.max(
                        Arrays.asList(streamConfigMap.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());
                //选择合适的预览尺寸
                mPreviewSize = chooseOptimalSize(streamConfigMap.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                if (mOnPreviewSizeListener != null) {
                    mOnPreviewSizeListener.onSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }

                //通过回调ImageReader的从相机获取未经压缩的原始图像数据，参数2表示使用双缓冲
                //一个缓冲区用于当前图像处理，另一个缓冲区接收新的相机帧，避免处理过程中丢失帧或造成阻塞
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(),
                        mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
                //当有新图像可用时的处理流程：
                //相机输出新的YUV帧给到ImageReader
                //在后台线程触发 mOnImageAvailableListener 该回调可以获取 Image 对象进行处理：
                //该回调完成YUV420_888到标准I420的转换
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                        mBackgroundHandler);

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera2Helper", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            //A2、在StateCallback中创建预览
            createCameraCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private void createCameraCaptureSession() {
        try {
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(mSurfaceTexture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);   //用于预览显示
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface()); //用于图像数据处理

            //创建同时支持预览显示和图像数据处理的会话，输出目标：surface、mImageReader
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return;
                            }

                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                //设置自动对焦模式：连续自动对焦（适合预览）
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                //启动视频预览
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed: ");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //从 ImageReader 获取最新一帧图像
                    Image image = reader.acquireNextImage();
                    if (image == null) {
                        return;
                    }

                    Image.Plane[] planes = image.getPlanes();
                    int width  = image.getWidth();
                    int height = image.getHeight();

                    byte[] yBytes = new byte[width * height];
                    byte[] uBytes = new byte[width * height / 4];
                    byte[] vBytes = new byte[width * height / 4];
                    byte[] i420   = new byte[width * height * 3 / 2];

                    for (int i = 0; i < planes.length; i++) {
                        int dstIndex = 0;
                        int srcIndex = 0;
                        int pixelStride = planes[i].getPixelStride(); //像素步长
                        int rowStride   = planes[i].getRowStride();   //行步长

                        ByteBuffer buffer = planes[i].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        if (i == 0) {
                            for (int j = 0; j < height; j++) {
                                System.arraycopy(bytes, srcIndex, yBytes, dstIndex, width);
                                srcIndex += rowStride;
                                dstIndex += width;
                            }
                        } else if (i == 1) {
                            for (int j = 0; j < height / 2; j++) {
                                for (int k = 0; k < width / 2; k++) {
                                    uBytes[dstIndex++] = bytes[srcIndex];
                                    srcIndex += pixelStride;
                                }

                                if (pixelStride == 2) {
                                    srcIndex += rowStride - width;
                                } else if (pixelStride == 1) {
                                    srcIndex += rowStride - width / 2;
                                }
                            }
                        } else if (i == 2) {
                            for (int j = 0; j < height / 2; j++) {
                                for (int k = 0; k < width / 2; k++) {
                                    vBytes[dstIndex++] = bytes[srcIndex];
                                    srcIndex += pixelStride;
                                }

                                if (pixelStride == 2) {
                                    srcIndex += rowStride - width;
                                } else if (pixelStride == 1) {
                                    srcIndex += rowStride - width / 2;
                                }
                            }
                        }
                        System.arraycopy(yBytes, 0, i420, 0, yBytes.length);
                        System.arraycopy(uBytes, 0, i420, yBytes.length, uBytes.length);
                        System.arraycopy(vBytes, 0, i420, yBytes.length + uBytes.length,
                                vBytes.length);
                        //完成YUV420_888到标准I420格式的转换，为后续的图像处理（裁剪、缩放等）提供了标准化的数据输入

                        if (mOnPreviewListener != null) {
                            //将标准化后的I420预览帧数据传递给外部处理模块，实现了图像采集与业务逻辑的分离
                            mOnPreviewListener.onPreviewFrame(i420, i420.length);
                        }
                    }
                    image.close();
                }
            };

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            // 每帧捕获完成时的处理：可以在这里获取帧数据、处理对焦状态等
        }
    };

}

