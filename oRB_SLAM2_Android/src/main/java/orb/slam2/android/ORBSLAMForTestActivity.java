package orb.slam2.android;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import orb.slam2.android.nativefunc.OrbNdkHelper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.w3c.dom.Text;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * ORB Test Activity For DataSetMode
 *
 * @author buptzhaofang@163.com Mar 24, 2016 4:13:32 PM
 *
 */
public class ORBSLAMForTestActivity extends Activity implements
        Renderer,CvCameraViewListener2, View.OnClickListener, LocationListener, SensorEventListener {

    //maxiaoba
    Button TrackOnly, dataCollection;
    TextView dataTextView;
    LocationManager locationManager;
    String provider;
    public static Location location;
    public static double lng;
    public static double lat;
    boolean checkPermission;
    public static SensorManager mSensorManager;
    public Sensor linearAccelerometer;
    public Sensor gravitySensor;
    public Sensor gyroscope;
    public static double[] acce;
    public static double[] gyro;
    //maxiaoba
    private static final String TAG = "OCVSample::Activity";
    ImageView imgDealed;

    LinearLayout linear;

    String vocPath, calibrationPath;

    private static final int INIT_FINISHED=0x00010001;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    private final int CONTEXT_CLIENT_VERSION = 3;
    private GLSurfaceView mGLSurfaceView;

    long addr;
    int w,h;
    boolean isSLAMRunning=true;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    static {
        System.loadLibrary("ORB_SLAM2_EXCUTOR");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_test_orb);

        //maxiaoba
        acce = new double[3];
        gyro = new double[3];
        dataTextView = (TextView) findViewById(R.id.dataTextView);
        TrackOnly=(Button)findViewById(R.id.track_only);
        dataCollection = (Button) findViewById(R.id.data_collection);
        TrackOnly.setOnClickListener(this);
        dataCollection.setOnClickListener(this);
        //maxiaoba

        imgDealed = (ImageView) findViewById(R.id.img_dealed);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mGLSurfaceView = new GLSurfaceView(this);
        linear = (LinearLayout) findViewById(R.id.surfaceLinear);
        //mGLSurfaceView.setEGLContextClientVersion(CONTEXT_CLIENT_VERSION);
        mGLSurfaceView.setRenderer(this);
        linear.addView(mGLSurfaceView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        vocPath = getIntent().getStringExtra("voc");
        calibrationPath = getIntent().getStringExtra("calibration");
        if (TextUtils.isEmpty(vocPath) || TextUtils.isEmpty(calibrationPath)) {
            Toast.makeText(this, "null param,return!", Toast.LENGTH_LONG)
                    .show();
            finish();
        } else {
            Toast.makeText(ORBSLAMForTestActivity.this, "init has been started!",
                    Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    OrbNdkHelper.initSystemWithParameters(vocPath,
                            calibrationPath);
                    Log.e("information==========>",
                            "init has been finished!");
                    myHandler.sendEmptyMessage(INIT_FINISHED);
                }
            }).start();
        }

        //// GPS
        checkPermission = true;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);
                return;
            }
        }
        location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            Toast.makeText(getApplicationContext(), "Location achieved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Location not achieved", Toast.LENGTH_SHORT).show();
        }

        /// Motion Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        dataTextView.setText("No Data");

    }


    //maxiaoba
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()) {
            case R.id.track_only:
                OrbNdkHelper.trackOnly();
                Toast.makeText(ORBSLAMForTestActivity.this, "Track Only", Toast.LENGTH_LONG).show();
                break;
            case R.id.data_collection:
//                Double lat = location.getLatitude();
//                Double lng = location.getLongitude();
//                dataTextView.setText("Lat: " + String.valueOf(lat) + "\nLng: " + String.valueOf(lng) + "\n" + "accex:"
//                        + String.valueOf(acce[0])+"\naccey:"+String.valueOf(acce[1])+"\naccez:"+String.valueOf(acce[2]));

                break;
        }
    }
    //maxiaoba

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case INIT_FINISHED:
//                    Toast.makeText(ORBSLAMForTestActivity.this,
//                            "init has been finished!",
//                            Toast.LENGTH_LONG).show();
//                    new Thread(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            while(isSLAMRunning){
//                                timestamp = (double)System.currentTimeMillis()/1000.0;
//                                // TODO Auto-generated method stub
//                                int[] resultInt = OrbNdkHelper.startCurrentORBForCamera(timestamp, addr, w, h);
//                                resultImg = Bitmap.createBitmap(w, h,
//                                        Config.RGB_565);
//                                resultImg.setPixels(resultInt, 0, w, 0, 0, w, h);
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        // TODO Auto-generated method stub
//                                        imgDealed.setImageBitmap(resultImg);
//                                    }
//                                });
//                            }
//                        }
//                    }).start();
//                    break;
//            }
//            super.handleMessage(msg);
            switch (msg.what) {
                case INIT_FINISHED:
                    Toast.makeText(ORBSLAMForTestActivity.this,
                            "init has been finished!",
                            Toast.LENGTH_LONG).show();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            while(isSLAMRunning){
                                timestamp = (double)System.currentTimeMillis()/1000.0;
                                // TODO Auto-generated method stub
                                 resultfloat = OrbNdkHelper.startCurrentORBForCamera(timestamp, addr, w, h);
//                                resultImg = Bitmap.createBitmap(w, h,
//                                        Config.RGB_565);
//                                resultImg.setPixels(resultInt, 0, w, 0, 0, w, h);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO Auto-generated method stub
//                                        imgDealed.setImageBitmap(resultImg);
                                        if(resultfloat.length==16)
                                            dataTextView.setText("X: " + String.valueOf(-resultfloat[3]) + "\nY: " + String.valueOf(-resultfloat[7]) + "\n" + "Z:"
                                       + String.valueOf(-resultfloat[11]));
//                                    dataTextView.setText("X: " + String.valueOf(resultfloat.length));
                                       /* if(resultfloat.length==6)
                                            dataTextView.setText("X: " + String.valueOf(resultfloat[0]) + "\nY: " + String.valueOf(resultfloat[1]) + "\n" + "Z:"
                                                    + String.valueOf(resultfloat[2])+"Roll: " + String.valueOf(resultfloat[3]) + "\nPitch: " + String.valueOf(resultfloat[4]) + "\n" + "Yaw:"
                                                    + String.valueOf(resultfloat[5]));*/
                                    }
                                });

                            }
                        }
                    }).start();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private Bitmap tmp, resultImg;
    private float[] resultfloat;
    private double timestamp;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // TODO Auto-generated method stub
        //OrbNdkHelper.readShaderFile(mAssetMgr);
        OrbNdkHelper.glesInit();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        OrbNdkHelper.glesResize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // TODO Auto-generated method stub
        OrbNdkHelper.glesRender();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mGLSurfaceView.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mSensorManager.registerListener(this, linearAccelerometer, 200000);
        mSensorManager.registerListener(this, gravitySensor, 50000);
        mSensorManager.registerListener(this, gyroscope, 100000);
        if (checkPermission) {
            locationManager.requestLocationUpdates(provider, 100, 0, this);
        }
    }



    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mGLSurfaceView.onPause();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mSensorManager.unregisterListener(this);
        if (checkPermission) {
            locationManager.removeUpdates(this);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("stop SLAM");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            isSLAMRunning=false;
//	            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
//	            mIsJavaCamera = !mIsJavaCamera;
//
//	            if (mIsJavaCamera) {
//	                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
//	                toastMesage = "Java Camera";
//	            } else {
//	                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
//	                toastMesage = "Native Camera";
//	            }
//
//	            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//	            mOpenCvCameraView.setCvCameraViewListener(this);
//	            mOpenCvCameraView.enableView();
//	            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
//	            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat im=inputFrame.rgba();
        synchronized (im) {
            addr=im.getNativeObjAddr();
        }

        w=im.cols();
        h=im.rows();
        return inputFrame.rgba();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermission = true;
                } else {
                    checkPermission = false;
                }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
//
//        long time = System.currentTimeMillis();
            lat = location.getLatitude();
            lng = location.getLongitude();
//        dataTextView.setText("Lat: " + String.valueOf(lat) + "\nLng: " + String.valueOf(lng) + "\n");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time= System.currentTimeMillis();
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            acce[0] = event.values[0];
            acce[1] = event.values[1];
            acce[2] = event.values[2];
        }
//                new WriteData().writeToFile("Acceleration.txt", String.valueOf(time));
//                new WriteData().writeToFile("Acceleration.txt", "X: " + String.valueOf(event.values[0]) +"\nY: " + String.valueOf(event.values[1]) + "\nZ: " + String.valueOf(event.values[2]));
//                // acceTextView.setText("X: " + String.valueOf(event.values[0]) +"\nY: " + String.valueOf(event.values[1]) + "\nZ: " + String.valueOf(event.values[2]));
//        } else if (sensor.getType() == Sensor.TYPE_GRAVITY) {
//
////                new WriteData().writeToFile("Gravity.txt", String.valueOf(time));
////                new WriteData().writeToFile("Gravity.txt", "X: " + String.valueOf(event.values[0]) + "\nY: " + String.valueOf(event.values[1]) + "\nZ: " + String.valueOf(event.values[2]));
////            }
//        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro[0] = event.values[0];
            gyro[1] = event.values[1];
            gyro[2] = event.values[2];
                // gyroTextView.setText("X: " + String.valueOf(event.values[0]) + "\nY: " + String.valueOf(event.values[1]) + "\nZ: " + String.valueOf(event.values[2]));
        }
//
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
