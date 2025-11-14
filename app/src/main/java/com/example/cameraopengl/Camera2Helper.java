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
    private ImageReader imageReader;
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

    private CameraCaptureSession mCaptureSession;

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
        startBackgroundThread();
        setUpCameraOutputs(width, height);

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.checkSelfPermission(Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            // 第一个参数指示打开哪个摄像头，第二个参数mStateCallback为相机的状态回调接口，
            // 第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
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
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                //获取此id对应摄像头的参数
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //选择摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
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
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                if (mOnPreviewSizeListener != null) {
                    mOnPreviewSizeListener.onSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }

                //从相机获取未经压缩的原始图像数据，参数2表示使用双缓冲
                //一个缓冲区用于当前图像处理，另一个缓冲区接收新的相机帧，避免处理过程中丢失帧或造成阻塞
                imageReader = ImageReader.newInstance(mPreviewSize.getWidth(),
                        mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
                //当有新图像可用时的处理流程：
                //相机输出新的YUV帧
                //ImageReader 接收到帧数据
                //在后台线程触发 mOnImageAvailableListener
                //在监听器中可以获取 Image 对象进行处理：
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                        mBackgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
        }
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
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

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
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

    private void createCameraPreviewSession() {
        try {
            // This is the output Surface we need to start preview.
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(mSurfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(imageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "onConfigureFailed: ");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
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
        }

    };


    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }

            Image.Plane[] planes = image.getPlanes();
            int width = image.getWidth();
            int height = image.getHeight();

            byte[] yBytes = new byte[width * height];
            byte[] uBytes = new byte[width * height / 4];
            byte[] vBytes = new byte[width * height / 4];
            byte[] i420   = new byte[width * height * 3 / 2];


            for (int i = 0; i < planes.length; i++) {
                int dstIndex = 0;
                int uIndex = 0;
                int vIndex = 0;
                int pixelStride = planes[i].getPixelStride();
                int rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                byte[] bytes = new byte[buffer.capacity()];

                buffer.get(bytes);
                int srcIndex = 0;
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

                if (mOnPreviewListener != null) {
                    mOnPreviewListener.onPreviewFrame(i420, i420.length);
                }
            }
            image.close();
        }
    };
}

