package com.example.kevindeland.techshop1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera2Activity extends AppCompatActivity {

    private static final String DOG = "AndroidCameraApi";

    private static final String TAG = "MONTY";;

    private Button takePictureButton;
    private Button cancelButton;
    private Button confirmButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private String studentName;
    private Image currentImage;

    /*
     * When the activity is created... I know this one
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Bundle b = getIntent().getExtras();
        //studentName = b.getString("studentName");

        //Log.d(TAG, "creating activity for student " + studentName);
        Log.d(TAG, "creating camera activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_2);

        Bundle extras = getIntent().getExtras();
        studentName = extras.getString("studentName");

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;

        cancelButton = (Button) findViewById(R.id.btn_cancel);
        assert cancelButton != null;
        confirmButton = (Button) findViewById(R.id.btn_confirm);
        assert confirmButton != null;

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "taking a picture");
                takePictureButton.setVisibility(View.INVISIBLE);
                takePicture();
            }
        });
    }


    protected void takePicture() {
        Log.d(TAG, "takePicture: beginning");
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            Log.d(TAG, "Logging jpegSizes...");
            for (int i=0; i<jpegSizes.length; i++) {
                Log.d(TAG, "i=" +i + ", width="+ jpegSizes[i].getWidth() +", height=" + jpegSizes[i].getHeight());
            }
            // default sizes
            int width = 640;
            int height = 480;

            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[4].getWidth();
                height = jpegSizes[4].getHeight();
            }

            Log.d(TAG, "Will take photo with width=" + width +", height=" + height);
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            // which file to write it to
            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            Log.d(TAG, "Will write image to " + file.toString());
            // listens for an image available???
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "Image Available!!!");
                    //Image image = null;
                    currentImage = reader.acquireLatestImage();

                    // need to run it in the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            // what happens when you click cancel?
                            cancelButton.setVisibility(View.VISIBLE);
                            cancelButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    currentImage.close();
                                    takePictureButton.setVisibility(View.VISIBLE);
                                    cancelButton.setVisibility(View.INVISIBLE);
                                    confirmButton.setVisibility(View.INVISIBLE);
                                    createCameraPreview();
                                }
                            });

                            // what happens when you click confirm?
                            confirmButton.setVisibility(View.VISIBLE);
                            confirmButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    // TODO go to next screen

                                    // convert image to bitmap
                                    ByteBuffer buffer = currentImage.getPlanes()[0].getBuffer();
                                    byte[] bytes = new byte[buffer.capacity()];
                                    buffer.get(bytes);

                                    Log.d(TAG, "byteArray = " + bytes.length);

                                    // before starting new activity... set buttons back to original state
                                    takePictureButton.setVisibility(View.VISIBLE);
                                    cancelButton.setVisibility(View.INVISIBLE);
                                    confirmButton.setVisibility(View.INVISIBLE);

                                    // changing to next activity
                                    Intent activityChangeIntent = new Intent(Camera2Activity.this, RecordAudioActivity.class);

                                    Bundle b = new Bundle();
                                    b.putString("studentName", studentName);
                                    Log.d(TAG, "putting student name" + studentName);
                                    b.putByteArray("image", bytes);
                                    activityChangeIntent.putExtras(b);
                                    startActivity(activityChangeIntent);
                                }
                            });
                        }
                    });

                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.d(TAG, "Capture completed");
                    // TODO this is what happens when the capture is complete
                    // what happens here???


                    // Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    // createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession has been configured. Executing session.capture");
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "SurfaceTexture Available");
            // open the camera
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // don't  need
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            Log.v(TAG, "SurfaceTexture updated");
        }
    };



    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // This is called when the camera is open
            Log.d(TAG, "CameraDevice onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0]; // get first camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(Camera2Activity.this,
                        new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Camera is now open!");
    }


    private final CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // camera already closed
            if (null == cameraDevice) {
                return;
            }
            // When the session is ready, start displaying the preview
            Log.d(TAG, "captureSession is officially ready!");
            cameraCaptureSessions = session;
            updatePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Toast.makeText(Camera2Activity.this, "Configuration change", Toast.LENGTH_SHORT).show();
        }
    };

    protected void createCameraPreview() {
        Log.d(TAG, "createCameraPreview()");
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            // set frame rate settings
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface); // capture request going to the surface texture

            cameraDevice.createCaptureSession(Arrays.asList(surface), captureSessionStateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        Log.d(TAG, "updatePreview");
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        // sets exposure and all that to automatic
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        CaptureRequest myCaptureRequest = captureRequestBuilder.build();

        try {
            cameraCaptureSessions.setRepeatingRequest(myCaptureRequest, null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     *  BACKGROUND THREADING
     */
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        Log.d(TAG, "background thread started");
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Resume and Pause
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // closeCamera();
        stopBackgroundThread();
        super.onPause();
    }




}