/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.vulcandocs.scantobim;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.atap.tango.mesh.io.TangoMeshFileUtils;
import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUx.StartParams;
import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

/** Custom **/
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import android.os.Environment;
import android.media.MediaScannerConnection;
import android.net.Uri;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Main Activity class for the Point Cloud Sample. Handles the connection to the {@link Tango}
 * service and propagation of Tango PointCloud data to OpenGL and Layout views. OpenGL rendering
 * logic is delegated to the {@link PointCloudRajawaliRenderer} class.
 */


public class ScanToBIM extends Activity implements OnClickListener {
    private static final String TAG = ScanToBIM.class.getSimpleName();
    private static final int SECS_TO_MILLISECS = 1000;

    private Tango mTango;
    private TangoConfig mConfig;
    private TangoUx mTangoUx;
    private TangoPointCloudManager mPointCloudManager;

    private PointCloudRajawaliRenderer mRenderer;
    private RajawaliSurfaceView mSurfaceView;
    private TextView mPointCountTextView;
    private TextView mAverageZTextView;

    private Button doneButton;

    private double mPointCloudPreviousTimeStamp;
    private boolean mIsConnected = false;
    private boolean check = true;


    private static final DecimalFormat FORMAT_THREE_DECIMAL = new DecimalFormat("0.000");
    private static final double UPDATE_INTERVAL_MS = 100.0;

    private double mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;

    private int mDepthCameraToDisplayRotation = 0;

    /**
     * Custom
     **/
    private int count;
    private int mPreviousPoseStatus;
    private int mPointCount;
    private float mDeltaTime;
    private float mPosePreviousTimeStamp;
    private float mCurrentTimeStamp;
    private float mPointCloudFrameDelta;
    private String mServiceVersion;
    private boolean mIsTangoServiceConnected;
    private TangoPoseData mPose;
    public static Object poseLock = new Object();
    public static Object depthLock = new Object();
    int scanNumber = 1;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_to_bim);


        mPointCountTextView = (TextView) findViewById(R.id.point_count_textview);
        mAverageZTextView = (TextView) findViewById(R.id.average_z_textview);
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_surface_view);

        doneButton = (Button) findViewById(R.id.doneButton);
        doneButton.setOnClickListener(this);

        mPointCloudManager = new TangoPointCloudManager();
        mTangoUx = setupTangoUxAndLayout();
        mRenderer = new PointCloudRajawaliRenderer(this);
        setupRenderer();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setAndroidOrientation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setAndroidOrientation();

        mTangoUx.start(new StartParams());
        // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
        // should create a new Tango object.
        mTango = new Tango(ScanToBIM.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (ScanToBIM.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        mIsConnected = true;
                    } catch (TangoOutOfDateException e) {
                        if (mTangoUx != null) {
                            mTangoUx.showTangoOutOfDate();
                        }
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use the default configuration plus add depth sensing.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    /**
     * Set up the callback listeners for the Tango service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the Point Cloud and Tango Events and Pose.
     */
    private void startupTango() {
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Passing in the pose data to UX library produce exceptions.
                if (mTangoUx != null) {
                    mTangoUx.updatePoseStatus(pose.statusCode);
                }

                synchronized (poseLock) {
                    mPose = pose;
                    // Calculate the delta time from previous pose.
                    mDeltaTime = (float) (pose.timestamp - mPosePreviousTimeStamp)
                            * SECS_TO_MILLISECS;
                    mPosePreviousTimeStamp = (float) pose.timestamp;
                    if (mPreviousPoseStatus != pose.statusCode) {
                        count = 0;
                    }
                    count++;
                    mPreviousPoseStatus = pose.statusCode;
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // Never runs.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                if (mTangoUx != null) {
                    mTangoUx.updateXyzCount(pointCloud.numPoints);
                }
                mPointCloudManager.updatePointCloud(pointCloud);

                final double currentTimeStamp = pointCloud.timestamp;
                final double pointCloudFrameDelta =
                        (currentTimeStamp - mPointCloudPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = currentTimeStamp;
                final double averageDepth = getAveragedDepth(pointCloud.points,
                        pointCloud.numPoints);

                mPointCloudTimeToNextUpdate -= pointCloudFrameDelta;

                if (mPointCloudTimeToNextUpdate < 0.0) {
                    mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;
                    final String pointCountString = Integer.toString(pointCloud.numPoints);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPointCountTextView.setText(pointCountString);
                            mAverageZTextView.setText(FORMAT_THREE_DECIMAL.format(averageDepth));
                        }
                    });
                }

                try {
                    synchronized (depthLock) {
                        mCurrentTimeStamp = (float) currentTimeStamp;
                        mPointCloudFrameDelta = (float) (mCurrentTimeStamp - mPointCloudPreviousTimeStamp)
                                * SECS_TO_MILLISECS;
                        mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                        TangoPoseData pointCloudPose = mTango.getPoseAtTime(mCurrentTimeStamp,
                                framePairs.get(0));
                        mPointCount = pointCloud.numPoints;

                        saveScanData(mPose, pointCloud);
                    }
                } catch (IOException e) {
                    Log.d("saveScanData", e.toString());
                }
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(event);
                }
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }

    /**
     * Sets Rajawali surface view and its renderer. This is ideally called only once in onCreate.
     */
    public void setupRenderer() {
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This will be executed on each cycle before rendering, called from the
                // OpenGL rendering thread

                // Prevent concurrent access from a service disconnect through the onPause event.
                synchronized (ScanToBIM.this) {
                    // Don't execute any tango API actions if we're not connected to the service.
                    if (!mIsConnected) {
                        return;
                    }

                    // Update point cloud data.
                    TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
                    if (pointCloud != null) {
                        // Calculate the camera color pose at the camera frame update time in
                        // OpenGL engine.
                        TangoSupport.TangoMatrixTransformData transform =
                                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                        Surface.ROTATION_0);
                        if (transform.statusCode == TangoPoseData.POSE_VALID) {
                            mRenderer.updatePointCloud(pointCloud, transform.matrix);
                        }
                    }

                    // Update current camera pose.
                    try {
                        // Calculate the last depth camera pose. This transform is used to display
                        // frustum in third and top down view, and used to render camera pose in
                        // first person view.
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                mDepthCameraToDisplayRotation);
                        mRenderer.updateCameraPose(lastFramePose);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Could not get valid transform");
                    }
                }
            }

            @Override
            public boolean callPreFrame() {
                return true;
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });
        mSurfaceView.setSurfaceRenderer(mRenderer);
    }

    /**
     * Sets up TangoUX layout and sets its listener.
     */
    private TangoUx setupTangoUxAndLayout() {
        TangoUxLayout uxLayout = (TangoUxLayout) findViewById(R.id.layout_tango);
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setLayout(uxLayout);
        tangoUx.setUxExceptionEventListener(mUxExceptionListener);
        return tangoUx;
    }

    /*
    * This is an advanced way of using UX exceptions. In most cases developers can just use the in
    * built exception notifications using the Ux Exception layout. In case a developer doesn't want
    * to use the default Ux Exception notifications, he can set the UxException listener as shown
    * below.
    * In this example we are just logging all the ux exceptions to logcat, but in a real app,
    * developers should use these exceptions to contextually notify the user and help direct the
    * user in using the device in a way Tango service expects it.
    */
    private UxExceptionEventListener mUxExceptionListener = new UxExceptionEventListener() {

        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(TAG, "Device lying on surface ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(TAG, "Very few depth points in mPoint cloud ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_INCOMPATIBLE_VM) {
                Log.i(TAG, "Device not running on ART");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_OVER_EXPOSED) {
                Log.i(TAG, "Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_TANGO_SERVICE_NOT_RESPONDING) {
                Log.i(TAG, "TangoService is not responding ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_UNDER_EXPOSED) {
                Log.i(TAG, "Camera Under Exposed ");
            }

        }
    };

    /**
     * First Person button onClick callback.
     */
    public void onFirstPersonClicked(View v) {
        mRenderer.setFirstPersonView();
    }

    /**
     * Third Person button onClick callback.
     */
    public void onThirdPersonClicked(View v) {
        mRenderer.setThirdPersonView();
    }

    /**
     * Top-down button onClick callback.
     */
    public void onTopDownClicked(View v) {
        mRenderer.setTopDownView();
    }

    /**
     * Done button onClick callback.
     */
    public void onClick(View v) {
        check = false;
        final EditText text = (EditText)findViewById(R.id.phone);
        String strip = text.getText().toString();
        TCPClient tcpclient = new TCPClient();
        tcpclient.getIp(strip);
        TCP tcp = new TCP();
        tcp.start();
        File file = new File("/storage/emulated/0/Documents/myScans");

        File[] Files = file.listFiles();
        if(Files != null) {
            int j;
            for(j = 0; j < Files.length; j++) {
                System.out.println(Files[j].getAbsolutePath());
                System.out.println(Files[j].delete());
            }
        }

        //if (file.exists()) {
        //String deleteCmd = "rm -r " + "/storage/emulated/0/Documents/myScans";
        //    Runtime runtime = Runtime.getRuntime();
        //    try {
        //        runtime.exec(deleteCmd);
        //    } catch (IOException e) { }
        //}

        new AlertDialog.Builder(ScanToBIM.this)
                .setTitle("Success")
                .setMessage("All files sent")
                .show();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mRenderer.onTouchEvent(event);
        return true;
    }

    /**
     * Calculates the average depth from a point cloud buffer.
     *
     * @param pointCloudBuffer
     * @param numPoints
     * @return Average depth.
     */
    private float getAveragedDepth(FloatBuffer pointCloudBuffer, int numPoints) {
        float totalZ = 0;
        float averageZ = 0;
        if (numPoints != 0) {
            int numFloats = 4 * numPoints;
            for (int i = 2; i < numFloats; i = i + 4) {
                totalZ = totalZ + pointCloudBuffer.get(i);
            }
            averageZ = totalZ / numPoints;
        }
        return averageZ;
    }

    /**
     * Compute the depth camera to display's rotation. This is used for rendering
     * camera in the correct rotation.
     */
    private void setAndroidOrientation() {
        Display display = getWindowManager().getDefaultDisplay();
        Camera.CameraInfo depthCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(1, depthCameraInfo);

        int depthCameraRotation = Surface.ROTATION_0;
        switch (depthCameraInfo.orientation) {
            case 90:
                depthCameraRotation = Surface.ROTATION_90;
                break;
            case 180:
                depthCameraRotation = Surface.ROTATION_180;
                break;
            case 270:
                depthCameraRotation = Surface.ROTATION_270;
                break;
        }

        mDepthCameraToDisplayRotation = display.getRotation() - depthCameraRotation;
        if (mDepthCameraToDisplayRotation < 0) {
            mDepthCameraToDisplayRotation += 4;
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        Log.i(TAG, "State: " + state);
        boolean v = Environment.MEDIA_MOUNTED.equals(state);
        return v;
    }

    public File getScanStorageDir() {
        // Get the directory for the user's public pictures directory
//        Date d = new Date();
//        String dirName = new SimpleDateFormat("Scans-yyyyMMdd-kkmmss").format(d);
        String dirName = "myScans";
        File path = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                dirName);
        if (!path.exists()) {
            if (!path.mkdirs()) {
                Log.e(TAG, "Directory not created");
                return null;
            }
        }
        return path;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, int len) {
        char[] hexChars = new char[len * 2];
        for (int j = 0; j < len; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void saveScanData(final TangoPoseData pose, final TangoPointCloudData pointCloud) throws IOException {
                if (check) {
                    Log.i(TAG, "Saving");
                    if (!isExternalStorageWritable()) {
                        Log.e(TAG, "External storage unavailable");
                        return;
                    }
                    File dir = getScanStorageDir();
                    if (dir == null) {
                        Log.e(TAG, "Failed to create scan directory");
                        return;
                    }


                    // Write
                    Integer numpoints = pointCloud.numPoints;
                    if (numpoints > 0) {

                        String filename = String.format("Scan%05d.data", scanNumber);
                        scanNumber += 1;
                        File file = new File(dir, filename);
                        Log.i(TAG, "Writing data to " + file.getAbsolutePath());

                        // Set up buffers
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        ByteBuffer buf = ByteBuffer.allocate(4 * 8);
                        buf.order(ByteOrder.LITTLE_ENDIAN);
                        // Pose
                        // Translation
                        for (int i = 0; i < pose.translation.length; i++) {
                            buf.putDouble(pose.translation[i]);
                        }
                        out.write(buf.array(), 0, 3 * 8);

                        // Rotation
                        buf.clear();
                        for (int i = 0; i < pose.rotation.length; i++) {
                            buf.putDouble(pose.rotation[i]);
                        }
                        out.write(buf.array(), 0, 4 * 8);

                        // Field of view
                        TangoCameraIntrinsics intrinsics = mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        double vFOV = 2 * Math.atan(0.5 * intrinsics.height / intrinsics.fy);
                        double hFOV = 2 * Math.atan(0.5 * intrinsics.width / intrinsics.fx);
                        buf.clear();
                        buf.putDouble(hFOV);
                        buf.putDouble(vFOV);
                        out.write(buf.array(), 0, 2 * 8);

                        // Pointcloud
                        FloatBuffer fb = pointCloud.points;
//            for (int i = 0; i < fb.capacity(); i++) {
//                buf.clear();
//                buf.putDouble(fb.get(i));
//                out.write(buf.array(), 0, 4);
//            }
                        buf.clear();
                        buf.putInt(pointCloud.numPoints);
                        out.write(buf.array(), 0, 4);
//            Log.i(TAG, "xyzCount="+Integer.toString(pointCloud.numPoints));
//            for (int i=0; i<10; i++) {
//                Log.i(TAG, Float.toString(xyzIj.xyz.get(i*3))+", "+Float.toString(xyzIj.xyz.get(i*3+1))+", "+Float.toString(xyzIj.xyz.get(i*3+2)));
//            }
                        for (int i = 0; i < pointCloud.numPoints * 4; i++) {
                            buf.clear();
                            buf.putFloat(fb.get(i));
                            out.write(buf.array(), 0, 4);
                        }
                        MediaScannerConnection.scanFile(
                                getApplicationContext(),
                                new String[]{file.getAbsolutePath()},
                                null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.v(TAG, "file " + path + " was scanned successfully: " + uri);
                                    }
                                });


                    }

                    // Save file

                    Log.i(TAG, "Done saving point cloud.");
                }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("PointCloud Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}