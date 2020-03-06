package com.thkoeln.jmoeller.vins_mobile_androidport;

import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * JNI Java Part
 */
public class VinsJNI implements Serializable {

    // Used to load the 'native-lib' library on application startup.
    static { System.loadLibrary("NativeLib"); }
    
    public native void init();

    // pass detected location (bounding box) of an object to VINS to estimate the distance from the camera
    public static native float getObjectDistance(int left, int top, int right, int bottom);
    // get 3d point cloud of a passed object bbox as float array [x_1,y_1,z_1,x_2,...]
    public static native float[] getObjectPointCloud(int left, int top, int right, int bottom);

    public static native float[] projectPointCloudToScreen(int size,  float[] point_cloud);

    public static native void onImageAvailable(int width, int height, int rowStrideY, ByteBuffer bufferY, 
                                               int rowStrideUV, ByteBuffer bufferU, ByteBuffer bufferV, 
                                               Surface surface, long timeStamp, boolean isScreenRotated,
                                               float virtualCamDistance);

    public static native void onImageAvailableRemote(int width, int height, int rowStrideY, ByteBuffer bufferY,
                                                     int rowStrideUV, ByteBuffer bufferU, ByteBuffer bufferV,
                                                     Surface surface, long timeStamp, boolean isScreenRotated,
                                                     float virtualCamDistance);

    public static native void updateViewInfo(TextView tvX, TextView tvY, TextView tvZ,
                                             TextView tvTotal, TextView tvLoop, TextView tvFeature,
                                             TextView tvBuf, ImageView initImage);
    
    public static native void onPause();
    
    public static native void onARSwitch(boolean isChecked);
    public static native void onLoopSwitch(boolean isChecked);
    public static native void setARTouchPosition(float x, float y);

}
