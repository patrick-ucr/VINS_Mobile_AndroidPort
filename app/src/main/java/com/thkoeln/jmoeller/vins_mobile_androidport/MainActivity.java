package com.thkoeln.jmoeller.vins_mobile_androidport;
// Android
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import androidx.annotation.NonNull;
//import android.support.annotation.NonNull;
import androidx.collection.SimpleArrayMap;
import androidx.core.app.ActivityCompat;
//import android.support.v4.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import android.support.v4.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
// Java
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
// OpenCV
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.RTrees;
import org.opencv.ml.SVM;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.thkoeln.jmoeller.vins_mobile_androidport.env.ImageUtils;
import com.thkoeln.jmoeller.vins_mobile_androidport.tracking.MultiBoxTracker;
import android.provider.Settings.Secure;
import android.widget.Toast;
import org.apache.commons.io.IOUtils;

/**
 * {@link MainActivity} only activity
 * manages camera input, texture output
 * textViews and buttons
 */
public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private static boolean ARchoice = false; // false
    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("opencv_java3");
        if (OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV loaded successfully");
        }
    }


    // needed for permission request callback
    private static final int PERMISSIONS_REQUEST_CODE = 1;//12345

    // camera2 API Camera
    private CameraDevice camera;
    // Back cam, 1 would be the front facing one
    private String cameraID = "0";

    // Texture View to display the camera picture, or the vins output
    private TextureView textureView;
    private Size previewSize;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader imageReader;

    // Handler for Camera Thread
    private Handler handler;
    private HandlerThread threadHandler;

    // Cam parameters
    private final int imageWidth = 640;
    private final int imageHeight = 480;//xukan origin 480



    int displayWidth;
    int displayHeight;


    private final int framesPerSecond = 30; //30
    
    /** Adjustment to auto-exposure (AE) target image brightness in EV */
    private final int aeCompensation = 0;
//    private final int aeCompensation = -1;
    
    private Surface surface;
    
    // JNI Object
    private VinsJNI vinsJNI;

    // TextViews
    private TextView tvX;
    private TextView tvY;
    private TextView tvZ;
    private TextView tvTotal;
    private TextView tvLoop;
    private TextView tvFeature;
    private TextView tvBuf;
    private OverlayView trackingOverlay;
    
    // ImageView for initialization instructions
    private ImageView ivInit;
    private SurfaceView surfaceView;
    private Canvas surfaceCanvas;

    // directory path for BRIEF config files
    private final String directoryPathBriefFiles = "/storage/emulated/0/VINS";

    // Distance of virtual Cam from Center
    // could be easily manipulated in UI later
    private float virtualCamDistance = 2;
    private final float minVirtualCamDistance = 2;
    private final float maxVirtualCamDistance = 40;

    private static int countVINS;
    private static long startActivityMs;

    private SensorManager sensorManager;
    private Sensor sensor;
    private TriggerEventListener triggerEventListener;

    private CascadeClassifier cascadeClassifier = null;
    private HOGDescriptor hogDescriptor = null;
    static final int kMaxChannelValue = 262143;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private Runnable imageConverter;
    private Runnable objectDetector; // using cascade classifier
    private Runnable drawingThread;
    private boolean detecting_objects = false;
    private boolean drawing_detections = false;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap drawBitmap = null;
    private boolean updatedDrawBitmap = false;
    private Mat rgbFrameMat = new Mat(imageHeight,imageWidth, CvType.CV_8UC4);

    private DNNObjectDetection playerDetector;
    private DNNObjectDetection tvDetector;
    private int timestamp = 0;
    private Activity appActivity;

    private String android_id;

    private boolean DO_SLAM = false;
    private boolean DO_DNN = false;

    private boolean sent_frame = false;
    private class Device{
        String name;
        Byte byteID;
        String uniqueID;
        String macAddress;
        String phoneModel;
        String endpointID; // for nearby connection id
        private Device(String nme, int bid, String uid, String mac, String mod){
            this.name = nme;
            this.byteID = (byte) bid;
            this.uniqueID = uid;
            this.macAddress = mac;
            this.phoneModel = mod;
        }
        private Device(Device d){
            this.name = d.name;
            this.byteID = d.byteID;
            this.uniqueID = d.uniqueID;
            this.macAddress = d.macAddress;
            this.phoneModel = d.phoneModel;
            if (d.endpointID != null){
                this.endpointID = d.endpointID;
            }
        }
        private void setEndpointID(String id){
            this.endpointID = id;
        }
        private String getEndpointID(){
            return this.endpointID;
        }
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("name: "+name).append(" byteID: "+byteID)
                    .append(" uniqueID: "+uniqueID).append(" macAddress: "+macAddress)
                    .append(" phoneModel: "+phoneModel).append(" endpointID: "+endpointID);
            return sb.toString();
        }
    }
    Device thisDevice;
    List<Device> knownPeers;
    List<Device> connectedPeers;
    Set<String> connectedEPID;
    // Nearby Connection API parameters
    final private Strategy P2P_STRATEGY = Strategy.P2P_CLUSTER;//P2P_CLUSTER
    private String SERVICE_ID;
    //Map<String, String> mapIDName; //map unique android_id to nickname
    //Map<String, Byte> mapNameByteID; // map nickname to one-byte ID
    //List<String> connectedEndpointID;

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // endpointId is remote endpoint ID
                    // Automatically accept the connection on both sides.
                    Log.i(TAG,String.format("Nearby: onConnectionInitiated with %s",endpointId));
                    Log.i(TAG, String.format("Request initated from a remote device %b",connectionInfo.isIncomingConnection()));
                    if (!connectedEPID.contains(endpointId)) {
                        Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
                    }
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.i(TAG,"Nearby: onConnectionResult");
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.i(TAG,"Nearby: onConnectionResult: STATUS_OK");
                            Log.i(TAG,String.format("Nearby: onConnectionResult: connectedEndpointID: %s",endpointId));
                            connectedEPID.add(endpointId);
                            byte[] handshake = new byte[2];
                            handshake[0] = (byte) 1; // messageID
                            handshake[1] = thisDevice.byteID;
                            //InputStream hsStream = new ByteArrayInputStream(handshake);
                            sendByte2Endpoint(handshake,endpointId);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.i(TAG,"Nearby: onConnectionResult: STATUS_CONNECTION_REJECTED");
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.i(TAG,"Nearby: onConnectionResult: STATUS_ERROR");
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    Log.i(TAG,String.format("Nearby: onDisconnected: %s",endpointId));
                    for (Device d: connectedPeers){
                        if (endpointId.equals(d.endpointID)){
                            connectedPeers.remove(d);
                        }
                    }
                    if (connectedEPID.contains(endpointId)){
                        connectedEPID.remove(endpointId);
                    }
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(final String endpointId, DiscoveredEndpointInfo info) {
                    // An endpoint was found. We request a connection to it.
                    Log.i(TAG, String.format("Nearby: onEndpointFound: %s", endpointId));
                    if (!connectedEPID.contains(endpointId)) {
                        Nearby.getConnectionsClient(getApplicationContext())
                                .requestConnection(thisDevice.name, endpointId, connectionLifecycleCallback)
                                .addOnSuccessListener(
                                        new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i(TAG, "Nearby: startDiscovery: requestConnection: SUCCESS");
                                            }
                                        }
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                )
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i(TAG, String.format("Nearby: startDiscovery: requestConnection: FAILURE %s", e.getMessage()));
                                                if (!connectedEPID.contains(endpointId)){
                                                    stopDiscovery();
                                                    startDiscovery();
                                                }
                                            }
                                        });
                    }
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                    Log.i(TAG,String.format("Nearby: onEndpointLost: %s",endpointId));
                    if (!connectedEPID.contains(endpointId)){
                        stopDiscovery();
                        startDiscovery();
                    }
                }
            };

    public static byte[] inputStream2Bytes( InputStream stream ) throws IOException {
        if (stream == null) return new byte[] {};
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean error = false;
        try {
            int numRead = 0;
            while ((numRead = stream.read(buffer)) > -1) {
                output.write(buffer, 0, numRead);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        output.flush();
        return output.toByteArray();
    }

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();

        @Override
        public void onPayloadReceived(@NonNull String fromEndpoint, @NonNull Payload payload) {
            Log.i(TAG, String.format("Nearby: onPayloadReceived %s payload.getId() %d", fromEndpoint, payload.getId()));
            incomingPayloads.put(payload.getId(), payload);
            Log.i(TAG, String.format("Nearby: onPayloadReceived incomingPayloads.size() %d", incomingPayloads.size()));
            if (payload.getType() != Payload.Type.BYTES){
                Log.e(TAG, "Nearby: onPayloadReceived Error!!! payload is not bytes");
                return;
            }
            byte[] receivedBytes = payload.asBytes();
            Log.i(TAG, String.format("Nearby: onPayloadReceived receivedBytes.length %d",receivedBytes.length));
            int messageTypeID = (int) receivedBytes[0];
            switch (messageTypeID) {
                case 1:// handshake
                    if (receivedBytes.length > 10) {
                        return; // this is not handshake
                    }
                    Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 1"));
                    // second byte is remote endpoint byteID
                    byte remoteByteID = receivedBytes[1];
                    for (Device d : knownPeers) {
                        // found known peer
                        if (Byte.compare(d.byteID, remoteByteID) == 0) {
                            Log.i(TAG, String.format("Nearby: onPayloadReceived this device is known %s",d.toString()));
                            boolean newPeer = true;
                            for (Device k : connectedPeers) {
                                // found known connected peer
                                if (Byte.compare(k.byteID, remoteByteID) == 0) {
                                    Log.i(TAG, String.format("Nearby: onPayloadReceived this device is connected %s",k.toString()));
                                    if (k.endpointID.equals(fromEndpoint)) {
                                        // all match so this is duplicate
                                        newPeer = false;
                                        break;
                                    } else { // endpointID has changed
                                        connectedPeers.remove(k);
                                        if (connectedEPID.contains(fromEndpoint)) {
                                            connectedEPID.remove(fromEndpoint);
                                        }
                                    }
                                    break;
                                }
                            }
                            if (newPeer) {
                                Device remoteDevice = new Device(d);
                                remoteDevice.setEndpointID(fromEndpoint);
                                connectedPeers.add(remoteDevice);
                                Log.i(TAG, String.format("Nearby: connectedPeers added remoteDevice %s", remoteDevice.toString()));
                                byte[] handshake = new byte[2];
                                handshake[0] = (byte) 1; // messageID
                                handshake[1] = thisDevice.byteID;
                                //InputStream hsStream = new ByteArrayInputStream(handshake);
                                sendByte2Endpoint(handshake, fromEndpoint);

                            }
                            break; // byteid is unique
                        }
                    }
                    break;
                case 2:// feature point
                    //do
                    Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 2"));
                    Log.i(TAG, String.format("Nearby: onPayloadReceived feature point %d", receivedBytes.length));
                    Log.i(TAG, String.format("Nearby: onPayloadReceived %s", Arrays.toString(receivedBytes)));
                    FeaturePointData receivedData = new FeaturePointData(receivedBytes);
                    Log.i(TAG, String.format("Nearby: onPayloadReceived: received FeaturePointData %s", receivedData.toString()));
                    float[] point_cloud = receivedData.getPointsFloatArray();
                    float[] screenPixels = VinsJNI.projectPointCloudToScreen(point_cloud.length, point_cloud);
                    Log.i(TAG, String.format("after projectPointCloudToScreen %s", Arrays.toString(screenPixels)));
                    ScreenPixelData pixels = new ScreenPixelData(1);
                    for (int i = 0; i < screenPixels.length; i = i + 2) {
                        pixels.addPixelPoint(Math.round(screenPixels[i]), Math.round(screenPixels[i + 1]));
                    }
                    Log.i(TAG, String.format("Pixels on screen: %s", pixels.toString()));
                    break;
                case 3:
                    Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 3"));
                    Log.i(TAG, String.format("Nearby: onPayloadReceived camera frame length %d", receivedBytes.length));
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String fromEndpoint, @NonNull PayloadTransferUpdate update) {
            Log.i(TAG, String.format("Nearby: onPayloadTransferUpdate getStatus %d getPayloadId() %d getBytesTransferred () %d",
                    update.getStatus(),update.getPayloadId(),update.getBytesTransferred ()));

            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                Log.i(TAG, String.format("Nearby: onPayloadTransferUpdate incomingPayloads.size() %d", incomingPayloads.size()));
                Payload payload = incomingPayloads.get(update.getPayloadId());
                if (payload == null || payload.getType() != Payload.Type.STREAM){
                    return;
                }
                InputStream is = payload.asStream().asInputStream();
                byte[] receivedBytes = null;
                try {
                    receivedBytes = IOUtils.toByteArray(is);
                } catch (IOException e){

                } finally {
                    if (is != null) {
                        try {is.close();} catch (IOException e) {}
                    }
                }
                if (receivedBytes == null || receivedBytes.length <= 1){
                    return;
                }
                int messageTypeID = (int) receivedBytes[0];
                switch (messageTypeID) {
                    case 1:// handshake
                        if (receivedBytes.length > 10) {
                            return; // this is not handshake
                        }
                        Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 1"));
                        // second byte is remote endpoint byteID
                        byte remoteByteID = receivedBytes[1];
                        for (Device d : knownPeers) {
                            // found known peer
                            if (Byte.compare(d.byteID, remoteByteID) == 0) {
                                Log.i(TAG, String.format("Nearby: onPayloadReceived this device is known %s",d.toString()));
                                boolean newPeer = true;
                                for (Device k : connectedPeers) {
                                    // found known connected peer
                                    if (Byte.compare(k.byteID, remoteByteID) == 0) {
                                        Log.i(TAG, String.format("Nearby: onPayloadReceived this device is connected %s",k.toString()));
                                        if (k.endpointID.equals(fromEndpoint)) {
                                            // all match so this is duplicate
                                            newPeer = false;
                                            break;
                                        } else { // endpointID has changed
                                            connectedPeers.remove(k);
                                            if (connectedEPID.contains(fromEndpoint)) {
                                                connectedEPID.remove(fromEndpoint);
                                            }
                                        }
                                        break;
                                    }
                                }
                                if (newPeer) {
                                    Device remoteDevice = new Device(d);
                                    remoteDevice.setEndpointID(fromEndpoint);
                                    connectedPeers.add(remoteDevice);
                                    Log.i(TAG, String.format("Nearby: connectedPeers added remoteDevice %s", remoteDevice.toString()));
                                    byte[] handshake = new byte[2];
                                    handshake[0] = (byte) 1; // messageID
                                    handshake[1] = thisDevice.byteID;
                                    InputStream hsStream = new ByteArrayInputStream(handshake);
                                    sendStream2Endpoint(hsStream, fromEndpoint);
                                    try {
                                        hsStream.close();
                                    } catch (IOException e){
                                        Log.i(TAG, String.format("Nearby: hsStream.close() IOException"));
                                    }
                                }
                                break; // byteid is unique
                            }
                        }
                        break;
                    case 2:// feature point
                        //do
                        Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 2"));
                        Log.i(TAG, String.format("Nearby: onPayloadReceived feature point %d", receivedBytes.length));
                        Log.i(TAG, String.format("Nearby: onPayloadReceived %s", Arrays.toString(receivedBytes)));
                        FeaturePointData receivedData = new FeaturePointData(receivedBytes);
                        Log.i(TAG, String.format("Nearby: onPayloadReceived: received FeaturePointData %s", receivedData.toString()));
                        float[] point_cloud = receivedData.getPointsFloatArray();
                        float[] screenPixels = VinsJNI.projectPointCloudToScreen(point_cloud.length, point_cloud);
                        Log.i(TAG, String.format("after projectPointCloudToScreen %s", Arrays.toString(screenPixels)));
                        ScreenPixelData pixels = new ScreenPixelData(1);
                        for (int i = 0; i < screenPixels.length; i = i + 2) {
                            pixels.addPixelPoint(Math.round(screenPixels[i]), Math.round(screenPixels[i + 1]));
                        }
                        Log.i(TAG, String.format("Pixels on screen: %s", pixels.toString()));
                        break;
                    case 3:
                        Log.i(TAG, String.format("Nearby: onPayloadReceived messageTypeID: 3"));
                        Log.i(TAG, String.format("Nearby: onPayloadReceived camera frame length %d", receivedBytes.length));
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public class Point3D {
        float x;
        float y;
        float z;

        public Point3D (float x, float y, float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public class Point2D {
        int x;
        int y;

        public Point2D (int x, int y){
            this.x = x;
            this.y = y;
        }
    }

    public class ScreenPixelData {
        int objectID;
        List<Point2D> screenPixels;

        public ScreenPixelData(int oid){
            this.objectID = oid;
            this.screenPixels = new ArrayList<>();
        }

        public void addPixelPoint(int x, int y){
            int x_inscreen = Math.min(x,imageWidth);
            x_inscreen = Math.max(x_inscreen,0);
            int y_inscreen = Math.min(y,imageHeight);
            y_inscreen = Math.max(y_inscreen,0);
            // filter out of view pixel
            if ((x_inscreen == 0 && y_inscreen == 0) ||
                    (x_inscreen == imageWidth && y_inscreen == imageHeight)){
                return;
            }
            Point2D p = new Point2D(x_inscreen, y_inscreen);
            screenPixels.add(p);
        }

        public List<Point2D> getScreenPixels(){
            return screenPixels;
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("objectID: "+objectID).append("\n");
            List<Point2D> points = getScreenPixels();
            for (int i = 0; i < points.size(); i++){
                sb.append("x:"+points.get(i).x).append(",y:"+points.get(i).y+"\n");
            }
            return sb.toString();
        }
    }
    public class FeaturePointData {
        byte messageTypeID; // type id = 2 for feature point data
        byte srcID;
        byte dstID;
        byte featurePointSize;
        List<Point3D> featurePoints;


        public FeaturePointData (byte mType, byte dst){
            this.messageTypeID = mType; //
            this.srcID = thisDevice.byteID;
            this.dstID = dst;
            this.featurePointSize = 0;
            this.featurePoints = new ArrayList<>();
        }

        public FeaturePointData (byte mType, byte dst, ArrayList<Point3D> featurePoints){
            this.messageTypeID = mType; //
            this.srcID = thisDevice.byteID;
            this.dstID = dst;
            this.featurePointSize = (byte) featurePoints.size();
            this.featurePoints = new ArrayList<>();
            this.featurePoints = (List<Point3D>) featurePoints.clone();
        }

        public FeaturePointData (byte[] bytes){
            Log.i(TAG, String.format("Nearby: FeaturePointData %d",bytes.length));
            assert bytes.length == (bytes[3] * 12) + 4;
            this.messageTypeID = bytes[0];
            this.srcID = bytes[1];
            this.dstID = bytes[2];
            this.featurePointSize = bytes[3];
            this.featurePoints = new ArrayList<>();
            int ind = 4;
            for (int i = 0; i < (int) featurePointSize ; i++){
                //Log.i(TAG, String.format("Nearby: featurePointSize %d",this.featurePointSize));
                //Log.i(TAG, String.format("Nearby: loop index %d",i));
                //Log.i(TAG, String.format("Nearby: xArray start index %d",ind));
                byte[] xArray = Arrays.copyOfRange(bytes, ind, ind+4);
                ind += 4;
                float x = ByteBuffer.wrap(xArray).getFloat();
                //Log.i(TAG, String.format("Nearby: yArray start index %d",ind));
                byte[] yArray = Arrays.copyOfRange(bytes, ind, ind+4);
                ind += 4;
                float y = ByteBuffer.wrap(yArray).getFloat();
                //Log.i(TAG, String.format("Nearby: zArray start index %d",ind));
                byte[] zArray = Arrays.copyOfRange(bytes, ind, ind+4);
                ind += 4;
                float z = ByteBuffer.wrap(zArray).getFloat();
                Point3D point3D = new Point3D(x,y,z);
                this.appendFeaturePoint(point3D);
            }
        }

        public int getSrcID() {
            return srcID;
        }
        public int getDstID() {
            return dstID;
        }
        public void setDstID(byte d){
            dstID = d;
        }

        private void appendFeaturePoint(Point3D p){
            // without increasing featurePointSize
            // called from constructor
            featurePoints.add(p);
        }
        public void addFeaturePoint(Point3D p){
            if (featurePointSize < 255) {
                featurePoints.add(p);
                featurePointSize += 1;
            } else {
                Log.e(TAG, "Error number of feature points cannot exceed 255");
            }
        }
        public void addFeaturePoints(ArrayList<Point3D> points) {
            if (featurePoints.size() + points.size() > 255){
                Log.e(TAG, "Error number of feature points cannot exceed 255");
                return;
            }
            featurePointSize = (byte) points.size();
            featurePoints.addAll(points);
        }
        public List<Point3D> getFeaturePoints(){
            return featurePoints;
        }

        public byte[] toByteArray(){
            byte[] output = new byte[(12*featurePointSize)+4];
            output[0] = messageTypeID;
            output[1] = srcID;
            output[2] = dstID;
            output[3] = featurePointSize;
            int ind = 4; //starting index
            for (final Point3D p: featurePoints){
                final byte[] xBytes = ByteBuffer.allocate(4).putFloat(p.x).array();
                for (byte b: xBytes){
                    output[ind] = b;
                    ind += 1;
                }
                final byte[] yBytes = ByteBuffer.allocate(4).putFloat(p.y).array();
                for (byte b: yBytes){
                    output[ind] = b;
                    ind += 1;
                }
                final byte[] zBytes = ByteBuffer.allocate(4).putFloat(p.z).array();
                for (byte b: zBytes){
                    output[ind] = b;
                    ind += 1;
                }
            }
            return output;
        }

        public float[] getPointsFloatArray(){
            float[] output = new float[featurePointSize*3];
            int index = 0;
            for (Point3D p: featurePoints){
                output[index++] = p.x;
                output[index++] = p.y;
                output[index++] = p.z;
            }
            return output;
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("msgTypeID: "+messageTypeID).append(" srcID: "+srcID).append(" dstID: "+dstID).append(" pointSize: "+featurePointSize+"\n");
            List<Point3D> points = getFeaturePoints();
            for (int i = 0; i < featurePointSize; i++){
                sb.append("x:"+points.get(i).x).append(",y:"+points.get(i).y).append(",z:"+points.get(i).z+"\n");
            }
            return sb.toString();
        }
    }
    /**
     * Gets Called after App start
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // first make sure the necessary permissions are given
        checkPermissionsIfNeccessary();
        
        if(!checkBriefFileExistance()) {
            Log.e(TAG, "Brief files not found here: " + directoryPathBriefFiles);
            finish();
        }
        Display d = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        displayWidth = d.getWidth();
        displayHeight = d.getHeight();
        initLooper();
        initVINS();
        initViews();
        countVINS = 0;
        startActivityMs = System.currentTimeMillis();
        initializeOpenCVDependencies();
        rgbFrameBitmap = Bitmap.createBitmap(imageWidth,imageHeight, Bitmap.Config.ARGB_8888);
        drawBitmap = Bitmap.createBitmap(imageWidth,imageHeight, Bitmap.Config.ARGB_8888);
        //MovementDetector.getInstance(this);
        initSensor();
        final Context appContext = getApplicationContext();
        playerDetector = new DNNObjectDetection(appContext);
        appActivity = (Activity) this;
        playerDetector.init(appActivity, imageWidth,imageHeight, "person");
        tvDetector = new DNNObjectDetection(appContext);
        tvDetector.init(appActivity,imageWidth,imageHeight,"tv");
        android_id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        Log.i(TAG, String.format("Android Unique ID: %s",android_id));
        addKnownPeers();
        //setRoles();
        SERVICE_ID = this.getPackageName();
        Log.i(TAG, String.format("SERVICE_ID: %s",SERVICE_ID));
        Log.i(TAG, String.format("displayWidth: %d displayHeight: %d",displayWidth,displayHeight));
        startAdvertising();
        startDiscovery();
    }

    // check whether this peer is already connected
    private boolean alreadyConnectedPeer(String eid){
        if (connectedPeers.size() <= 0){
            return false;
        }
        boolean connected = false;
        for (Device d: connectedPeers){
            if (eid.equals(d.endpointID)){
                connected = true;
            }
        }
        return connected;
    }
    private void addKnownPeers(){
        knownPeers = new ArrayList<>();
        knownPeers.add(new Device("green",1,"a3ed773a1d743ff8", "42:4e:36:89:06:5d","GPix2"));
        knownPeers.add(new Device("red",2, "1a8da2b6f7279b9b","42:4e:36:91:0a:8c","GPix2"));
        knownPeers.add(new Device("blue",3, "453c359d22425269","de:0b:34:bd:a5:f0","LGg6"));
        for (Device d: knownPeers){
            if (d.uniqueID.equals(android_id)){
                thisDevice = d;
            }
        }
        Log.i(TAG, String.format("thisDevice: %s",thisDevice.toString()));
        connectedPeers = new ArrayList<>();
        connectedEPID = new HashSet<>();
    }

    private void setRoles(){
        switch(thisDevice.name) {
            case "red":
                DO_DNN = false;
                DO_SLAM = true;
                break;
            case "green":
                DO_DNN = false;
                DO_SLAM = false;
                break;
            default:
                break;
        }
    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(P2P_STRATEGY).build();
        Nearby.getConnectionsClient(getApplicationContext()).startAdvertising(
                thisDevice.name,SERVICE_ID, connectionLifecycleCallback,advertisingOptions
        )
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG,"Nearby: startAdvertising: SUCCESS");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG,"Nearby: startAdvertising: FAILURE");
                    }
                });
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(P2P_STRATEGY).build();
        Nearby.getConnectionsClient(getApplicationContext()).startDiscovery(
                SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG,"Nearby: startDiscovery: SUCCESS");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG,"Nearby: startDiscovery: FAILURE");
                    }
                });
    }

    private void stopAdvertising() {
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
    }

    private void stopDiscovery() {
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
    }

    private void listConnectedPeers() {
        Log.i(TAG, String.format("Nearby: Number connectedPeers: %d connected epid: %d",
                connectedPeers.size(), connectedEPID.size()));

        if (connectedPeers.size() > 0){
            for (final Device d: connectedPeers) {
                Log.i(TAG, String.format("Nearby: connected peer %s",d.toString()));
            }
            for (final String s: connectedEPID){
                Log.i(TAG, String.format("Nearby: connected epid %s",s));
            }
        }
    }

    private ArrayList<Point3D> getFeaturePoints(){
        int featureSize = 20;
        ArrayList<Point3D> point3DList = new ArrayList<>();
        for (int i = 0; i < featureSize; i++){
            point3DList.add(new Point3D((float) 1.2,(float) 2.3,(float) 3.4));
        }
        return point3DList;
    }

    private void sendByte2Endpoint(byte[] data, String endpoint_id){
        if (data.length > ConnectionsClient.MAX_BYTES_DATA_SIZE)
        {
            Log.e(TAG, String.format("Data size %d exceed MAX_BYTES_DATA_SIZE",data.length));
            return;
        }
        Payload bytesPayload = Payload.fromBytes(data);
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endpoint_id,bytesPayload);
    }

    private void sendByte2AllPeers(byte[] data){
        if (data.length > ConnectionsClient.MAX_BYTES_DATA_SIZE)
        {
            Log.e(TAG, String.format("Data size %d exceed MAX_BYTES_DATA_SIZE",data.length));
            return;
        }

        if (connectedPeers.size() <= 0){
            return;
        }

        Payload bytesPayload = Payload.fromBytes(data);
        List<String> allPeersEID = new ArrayList<>();
        for (Device dev : connectedPeers) {
            allPeersEID.add(dev.endpointID);
            Log.i(TAG, String.format("Sending bytes to %s", dev.endpointID));
        }
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(allPeersEID,bytesPayload);
    }

    private void sendStream2Endpoint(InputStream stream, String endpointID) {
        if (!connectedEPID.contains(endpointID)){
            Log.i(TAG, String.format("Unknown endpoint %s",endpointID));
            return;
        }
        Payload streamPayload = Payload.fromStream(stream);
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endpointID,streamPayload);
    }
    private void sendStream2AllPeers(InputStream stream){
        if (connectedPeers.size() <= 0){
            Log.i(TAG, "No peer to send");
            return;
        }

        Payload streamPayload = Payload.fromStream(stream);

        List<String> allPeersEID = new ArrayList<>();
        for (Device dev : connectedPeers) {
            allPeersEID.add(dev.endpointID);
            Log.i(TAG, String.format("Sending stream to %s", dev.endpointID));
        }
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(allPeersEID,streamPayload);

    }


    private void attemptConnections(){
        if (connectedPeers.size() == 0){
            stopAdvertising();
            stopDiscovery();
            startAdvertising();
            startDiscovery();
        }
    }
    private void initSensor(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        triggerEventListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                Log.i(TAG, "Significant motion detected!!!");
            }
        };

        sensorManager.requestTriggerSensor(triggerEventListener, sensor);
    }
    private void initializeOpenCVDependencies() {

        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_fullbody);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            // clear data cache in app folder "cascade"
            if (cascadeDir.isDirectory()){
                String[] children = cascadeDir.list();
                for (String c : children){
                    new File(cascadeDir, c).delete();
                }
            }
            Log.i(TAG, "cascadeDir: "+cascadeDir.getAbsolutePath());
            File mCascadeFile = new File(cascadeDir, "cascade.xml");
            Log.i(TAG, String.format("cascadeDir mCascadeFile: %s size: %d",mCascadeFile.getAbsolutePath(),mCascadeFile.length()));
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer,0,bytesRead);
            }
            is.close();
            os.close();
            Log.i(TAG, String.format("cascadeDir mCascadeFile: %s size: %d",mCascadeFile.getAbsolutePath(),mCascadeFile.length()));
            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            //cascadeClassifier.load(mCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }
        if (cascadeClassifier != null){
            Log.i(TAG, "cascadeClassifier not null");
        }

        hogDescriptor = new HOGDescriptor();
        hogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());

        if (rgbBytes == null){
            rgbBytes = new int[imageWidth * imageHeight];
        }
    }
    /**
     * check if necessary files brief_k10L6.bin and brief_pattern.yml exist in the directoryPathBriefFiles
     * @return true if files are existent and read and writable
     */
    private boolean checkBriefFileExistance() {
        File directoryFile = new File(directoryPathBriefFiles);
        //Checking the availability state of the External Storage.
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {

            //If it isn't mounted - we can't write into it.
            return false;
        }

        //String externalStoragePath =
        if(!directoryFile.exists())
            return false;

        String filepathVoc = directoryFile.getAbsolutePath() + File.separator + "brief_k10L6.bin";
        File vocFile = new File(filepathVoc);
        Log.d(TAG, "Filepath: " + filepathVoc + 
                   " File Exists: " + vocFile.exists() + 
                   " File Write: " + vocFile.canWrite() +  
                   " File Read: " + vocFile.canRead());
        if(!vocFile.exists() || !vocFile.canRead() || !vocFile.canWrite())
            return false;
        
        String filepathPattern = directoryFile + File.separator + "brief_pattern.yml";
        File patternFile = new File(filepathPattern);
        Log.d(TAG, "Filepath: " + filepathPattern + 
                   " File Exists: " + patternFile.exists() + 
                   " File Write: " + patternFile.canWrite() +  
                   " File Read: " + patternFile.canRead());
        if(!patternFile.exists() || !patternFile.canRead() || !patternFile.canWrite())
            return false;
        
        return true;
    }

    /**
     * Starting separate thread to handle camera input
     */
    private void initLooper() {
        threadHandler = new HandlerThread("Camera2Thread");
        threadHandler.start();
        handler = new Handler(threadHandler.getLooper());
    }

    /**
     * initializes an new VinsJNI Object
     */
    private void initVINS() {
        vinsJNI = new VinsJNI();
        vinsJNI.init();
    }
    
    /**
     * Finding all UI Elements,
     * Setting TextureView Listener to this object.
     */
    private void initViews() {
        tvX = (TextView) findViewById(R.id.x_Label);
        tvY = (TextView) findViewById(R.id.y_Label);
        tvZ = (TextView) findViewById(R.id.z_Label);
        tvTotal = (TextView) findViewById(R.id.total_odom_Label);
        tvLoop = (TextView) findViewById(R.id.loop_Label);
        tvFeature = (TextView) findViewById(R.id.feature_Label);
        tvBuf = (TextView) findViewById(R.id.buf_Label);
        
        ivInit = (ImageView) findViewById(R.id.init_image_view);
        ivInit.setVisibility(View.INVISIBLE);//View.VISIBLE

        textureView = (TextureView) findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);


        // Define the Switch listeners
        Switch arSwitch = (Switch) findViewById(R.id.ar_switch);
        arSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG,"arSwitch State = " + isChecked);
                ARchoice = isChecked;
                VinsJNI.onARSwitch(isChecked);
            }
        });
        
        Switch loopSwitch = (Switch) findViewById(R.id.loop_switch);
        loopSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG,"loopSwitch State = " + isChecked);
                VinsJNI.onLoopSwitch(isChecked);
            }
        });

        SeekBar zoomSlider = (SeekBar) findViewById(R.id.zoom_slider);
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                virtualCamDistance = minVirtualCamDistance + ((float)progress / 100) * (maxVirtualCamDistance - minVirtualCamDistance);
                Log.i(TAG,String.format("virtualCamDistance: %.2f", virtualCamDistance));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {  }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  }
        });
    }
    
    /**
     * SurfaceTextureListener interface function 
     * used to set configuration of the camera and start it
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            // check permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                checkPermissionsIfNeccessary();
                return;
            }
            
            // start up Camera (not the recording)
            cameraManager.openCamera(cameraID, cameraDeviceStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            try {
                camera = cameraDevice;
                startCameraView(camera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {}

        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    /**
     * starts CameraView
     */
    private void startCameraView(CameraDevice camera) throws CameraAccessException {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        
        // to set CameraView size
        texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
        Log.d(TAG, "texture width: " + textureView.getWidth() + " height: " + textureView.getHeight());
        surface = new Surface(texture);
                
        try {
            // to set request for CameraView
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // to set the format of captured images and the maximum number of images that can be accessed in mImageReader
        imageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.YUV_420_888, 1);
        //imageReader = ImageReader.newInstance(, 480, ImageFormat.YUV_420_888, 1);

        imageReader.setOnImageAvailableListener(onImageAvailableListener, handler);


        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
        // get the StepSize of the auto exposure compensation
        Rational aeCompStepSize = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);


        if(aeCompStepSize == null) {
            Log.e(TAG, "Camera doesn't support setting Auto-Exposure Compensation");
            finish();
        }
        Log.d(TAG, "AE Compensation StepSize: " + aeCompStepSize);
        
        int aeCompensationInSteps = aeCompensation * aeCompStepSize.getDenominator() / aeCompStepSize.getNumerator();
        Log.d(TAG, "aeCompensationInSteps: " + aeCompensationInSteps );
        previewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, aeCompensationInSteps);
        
        // set the camera output frequency to 30Hz
        previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(framesPerSecond, framesPerSecond));

        // the first added target surface is for CameraView display
        // the second added target mImageReader.getSurface() 
        // is for ImageReader Callback where it can be access EACH frame
        //mPreviewBuilder.addTarget(surface);
        previewBuilder.addTarget(imageReader.getSurface());

        //output Surface
        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(imageReader.getSurface());
        
        
        camera.createCaptureSession(outputSurfaces, sessionStateCallback, handler);
    }

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                updateCameraView(session);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };
    /**
     * Starts the RepeatingRequest for 
     */
    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                Log.d(TAG,String.format("Initializing buffer %d at size %d", i, buffer.capacity()));
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    private void runCascadeClassifier(){


    }

    private void updateCameraView(CameraCaptureSession session)
            throws CameraAccessException {
//        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

        session.setRepeatingRequest(previewBuilder.build(), null, handler);
    }

    //@TargetApi(19)
    public static byte[] yuvImageToByteArray(Image image) {

        assert(image.getFormat() == ImageFormat.YUV_420_888);

        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        byte[] result = new byte[width * height * 3 / 2];

        int stride = planes[0].getRowStride();
        assert (1 == planes[0].getPixelStride());
        if (stride == width) {
            planes[0].getBuffer().get(result, 0, width*height);
        }
        else {
            for (int row = 0; row < height; row++) {
                planes[0].getBuffer().position(row*stride);
                planes[0].getBuffer().get(result, row*width, width);
            }
        }

        stride = planes[1].getRowStride();
        assert (stride == planes[2].getRowStride());
        int pixelStride = planes[1].getPixelStride();
        assert (pixelStride == planes[2].getPixelStride());
        byte[] rowBytesCb = new byte[stride];
        byte[] rowBytesCr = new byte[stride];

        for (int row = 0; row < height/2; row++) {
            int rowOffset = width*height + width/2 * row;
            planes[1].getBuffer().position(row*stride);
            planes[1].getBuffer().get(rowBytesCb);
            planes[2].getBuffer().position(row*stride);
            planes[2].getBuffer().get(rowBytesCr);

            for (int col = 0; col < width/2; col++) {
                result[rowOffset + col*2] = rowBytesCr[col*pixelStride];
                result[rowOffset + col*2 + 1] = rowBytesCb[col*pixelStride];
            }
        }
        return result;
    }
    /**
     *  At last the actual function with access to the image
     */
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        /*
         *  The following method will be called every time an image is ready
         *  be sure to use method acquireNextImage() and then close(), otherwise, the display may STOP
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            //connectToPeers();
            // get the newest frame
            Image image = reader.acquireNextImage();
            
            if (image == null) {
                return;
            }
            // Patrick
            //long startConvertPayload = System.currentTimeMillis();
            //byte[] payload = yuvImageToByteArray(image);
            //long timeConvertPayload = System.currentTimeMillis() - startConvertPayload;
            //Log.i(TAG,String.format("ImageReader.OnImageAvailableListener image byte array size %d time %d",
            //        payload.length, timeConvertPayload));

            if (image.getFormat() != ImageFormat.YUV_420_888) {
                Log.e(TAG, "xukan, camera image is in wrong format");
            }else{
                Log.i(TAG, "xukan, camera image is in right format");
            }
            ++timestamp;
            final long currTimestamp = timestamp;
            //RGBA output
            Image.Plane Y_plane = image.getPlanes()[0];
            final int Y_rowStride = Y_plane.getRowStride();
            Image.Plane U_plane = image.getPlanes()[1];
            final int UV_rowStride = U_plane.getRowStride();
            final int UV_pixelStride = U_plane.getPixelStride();
            Image.Plane V_plane = image.getPlanes()[2];


            int currentRotation = getWindowManager().getDefaultDisplay().getRotation();
            boolean isScreenRotated = currentRotation != Surface.ROTATION_90;
            Log.i(TAG, String.format("Screen rotated?: %b",isScreenRotated));
            // pass image to c++ part
            countVINS++;
            float vinsFPS =  ( (float)countVINS /(float)(System.currentTimeMillis()-startActivityMs)) * 1000.0f;
            Log.i(TAG,String.format("VINS frame rate: %.2f", vinsFPS));
            Log.i(TAG,"Before VinsJNI.onImageAvailable");

            if (DO_SLAM) {
                // loop detection remote
                int loop_detection_remote_period = 60;
                if (countVINS % loop_detection_remote_period == 0) {
                //if (false){ // to disable ViewController::loopDetectionRemote()
                    Log.i(TAG, "onImageAvailableRemote()");
                    VinsJNI.onImageAvailableRemote(image.getWidth(), image.getHeight(),
                            Y_rowStride, Y_plane.getBuffer(),
                            UV_rowStride, U_plane.getBuffer(), V_plane.getBuffer(),
                            surface, image.getTimestamp(), isScreenRotated,
                            virtualCamDistance);
                } else {
                    VinsJNI.onImageAvailable(image.getWidth(), image.getHeight(),
                            Y_rowStride, Y_plane.getBuffer(),
                            UV_rowStride, U_plane.getBuffer(), V_plane.getBuffer(),
                            surface, image.getTimestamp(), isScreenRotated,
                            virtualCamDistance);
                }

                // run the updateViewInfo function on the UI Thread so it has permission to modify it
                runOnUiThread(new Runnable() {
                    public void run() {
                        VinsJNI.updateViewInfo(tvX, tvY, tvZ, tvTotal, tvLoop, tvFeature, tvBuf, ivInit);
                    }
                });

            }
            // SLAM (VINS) end here
            // below are for DNN object detection and tracking

            synchronized (this) {
                fillBytes(image.getPlanes(), yuvBytes);
            }

            image.close();

            if (countVINS % 60 == 0) {
                listConnectedPeers();
            }

            int sendFramePeriod = 120;
            if (connectedPeers.size() > 0 &&
                    thisDevice.name == "green" &&
                    countVINS % sendFramePeriod == 0 &&
                    !sent_frame) {
                final long startMs = System.currentTimeMillis();
                byte[] yBytes = new byte[(yuvBytes[0].length) + 2];
                yBytes[0] = (byte) 3; // messageID
                yBytes[1] = thisDevice.byteID;
                System.arraycopy(yuvBytes[0], 0, yBytes, 2, yuvBytes[0].length);
                Log.i(TAG, String.format("yBytes[0] %d length %d", yBytes[0], yBytes.length));
                Log.i(TAG, String.format("ConnectionsClient.MAX_BYTES_DATA_SIZE %d", ConnectionsClient.MAX_BYTES_DATA_SIZE));
                InputStream yStream = new ByteArrayInputStream(yBytes);
                Log.i(TAG, String.format("Prepare frame to send %d ms",System.currentTimeMillis()-startMs));
                //stopDiscovery();
                sent_frame = true;
                /*
                int first_index = 0;
                int last_index = ConnectionsClient.MAX_BYTES_DATA_SIZE;
                while (last_index < yBytes.length) {
                    sendByte2AllPeers(Arrays.copyOfRange(yBytes,first_index,last_index));
                    first_index = last_index;
                    last_index += ConnectionsClient.MAX_BYTES_DATA_SIZE;
                }
                sendByte2AllPeers(Arrays.copyOfRange(yBytes,first_index,yBytes.length));
                */
                sendStream2AllPeers(yStream);


                /*try{
                    yStream.close();
                } catch (IOException e){

                }*/
            }

            if (DO_DNN) {
                ImageUtils.convertYUV420ToARGB8888(
                        yuvBytes[0],
                        yuvBytes[1],
                        yuvBytes[2],
                        imageWidth,
                        imageHeight,
                        Y_rowStride,
                        UV_rowStride,
                        UV_pixelStride,
                        rgbBytes
                );

                rgbFrameBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);


                playerDetector.onFrame(
                        imageWidth,
                        imageHeight,
                        Y_rowStride,
                        appActivity,
                        yuvBytes[0],
                        timestamp
                );

                int dnn_detection_period = 60;
                //if (countVINS % dnn_detection_period == 0) {

                // only RED phone detect
                if (thisDevice.name == "red" && !playerDetector.running_dnn) {
                    //Log.i(TAG, "DNN is triggered.");
                    playerDetector.detect(rgbFrameBitmap, yuvBytes[0], currTimestamp);
                    //attemptConnections();
                    listConnectedPeers();
                }

                List<RectF> listBbox = playerDetector.getTrackedBbox();
                Log.i(TAG, "listBbox size: " + listBbox.size());
                for (RectF bbox : listBbox) {
                    Log.i(TAG, "Bbox RectF: " + bbox);
                    float dist_ = VinsJNI.getObjectDistance(
                            (int) bbox.left,
                            (int) bbox.top,
                            (int) bbox.right,
                            (int) bbox.bottom
                    );
                    float[] objectPC = VinsJNI.getObjectPointCloud(
                            (int) bbox.left,
                            (int) bbox.top,
                            (int) bbox.right,
                            (int) bbox.bottom
                    );
                    Log.i(TAG, String.format("Object distance: %.2f", dist_));
                    Log.i(TAG, String.format("objectPC.length: %d", objectPC.length));
                    for (int i = 0; i < objectPC.length; i++) {
                        Log.i(TAG, String.format("objectPC %d %.2f", i, objectPC[i]));
                    }
                    if (objectPC.length > 0 && connectedPeers.size() > 0) {
                        byte[] objPC_byte = FloatArray2ByteArray(objectPC);
                        Log.i(TAG, String.format("objPC_byte: %d", objPC_byte.length));
                        byte[] pointcloud_msg = new byte[objPC_byte.length + 4];
                        pointcloud_msg[0] = 2; //msg type 2
                        pointcloud_msg[1] = thisDevice.byteID;
                        pointcloud_msg[3] = (byte) (objPC_byte.length / 12);
                        System.arraycopy(objPC_byte, 0, pointcloud_msg, 4, objPC_byte.length);
                        for (Device dev : connectedPeers) {
                            pointcloud_msg[2] = dev.byteID;
                            //InputStream pcStream = new ByteArrayInputStream(pointcloud_msg);
                            sendByte2Endpoint(pointcloud_msg, dev.endpointID);
                        }
                    }
                }
            }
        }
    };

    private void saveMatToExternal(Mat mat, int id){
        Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2BGR);
        String filename =  String.format("%010d.jpg",id);
        String fileFullPath = directoryPathBriefFiles + File.separator
                + "saved_images" + File.separator + filename;
        Imgcodecs.imwrite(fileFullPath,mat);
    }

    private void objectDetector(){
        /*
            Utils.bitmapToMat(rgbFrameBitmap,rgbFrameMat);

            objectDetector = new Runnable() {
                @Override
                public void run() {
                    detecting_objects = true;
                    Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                    if (cascadeClassifier == null || hogDescriptor == null) return;
                    Mat greyFrameMat = new Mat();
                    Imgproc.cvtColor(rgbFrameMat,greyFrameMat,Imgproc.COLOR_RGB2GRAY);
                    Imgproc.equalizeHist(greyFrameMat,greyFrameMat);
                    Mat lap = new Mat();
                    Imgproc.Laplacian(greyFrameMat,lap,3);
                    MatOfDouble median = new MatOfDouble();
                    MatOfDouble std = new MatOfDouble();
                    Core.meanStdDev(lap, median, std);
                    double varOfLap = Math.pow(std.get(0,0)[0],2);
                    Log.i(TAG, String.format("Variance of Laplace: %.4f",varOfLap));
                    Log.i(TAG, String.format("countVINS %010d",countVINS));
                    if (varOfLap > 1000.0){ // Variance of Laplace sharpness threshold
                        MatOfRect locationsCascade = new MatOfRect();
                        MatOfRect locationsHOG = new MatOfRect();
                        MatOfDouble weights = new MatOfDouble(); //for hog
                        cascadeClassifier.detectMultiScale(greyFrameMat,locationsCascade);
                        Mat bgrFrameMat = new Mat();
                        Imgproc.cvtColor(rgbFrameMat,bgrFrameMat, Imgproc.COLOR_RGB2BGR);
                        hogDescriptor.detectMultiScale(bgrFrameMat,locationsHOG,weights);
                        Log.i(TAG, String.format("Locations Cascade: %d HOG: %d ",
                                locationsCascade.toList().size(),locationsHOG.toList().size()));
                        List<Rect> locationsListCascade = locationsCascade.toList();
                        for (Rect rect : locationsListCascade) {
                            Imgproc.rectangle(bgrFrameMat,
                                    new Point(rect.x,rect.y),
                                    new Point(rect.x + rect.width , rect.y + rect.height),
                                    new Scalar(255,0,0),
                                    5);
                        }
                        List<Rect> locationsListHog = locationsHOG.toList();
                        for (Rect rect : locationsListHog) {
                            Imgproc.rectangle(bgrFrameMat,
                                    new Point(rect.x,rect.y),
                                    new Point(rect.x + rect.width , rect.y + rect.height),
                                    new Scalar(0,0,255),
                                    5);
                        }
                        Mat rgbFrameMatToDraw = new Mat();
                        Imgproc.cvtColor(bgrFrameMat,rgbFrameMatToDraw,Imgproc.COLOR_BGR2RGB);
                        Utils.matToBitmap(rgbFrameMatToDraw,drawBitmap);
                        updatedDrawBitmap = true;
                        //saveMatToExternal(rgbFrameMatToDraw,countVINS);

                    }
                    detecting_objects = false;
                }
            };

            final int OBJ_DET_PERIOD = 5; // detect objects every ... frame
            if (!detecting_objects && countVINS % OBJ_DET_PERIOD == 0) {
                objectDetector.run();
            }
            drawingThread = new Runnable() {
                @Override
                public void run() {
                    updatedDrawBitmap = false;
                    ivInit.setImageBitmap(drawBitmap);
                    drawing_detections = false;
                }
            };

            if (drawBitmap != null &&
                    !drawing_detections &&
                        updatedDrawBitmap == true){
                drawing_detections = true;
                runOnUiThread(drawingThread);
            }*/
        // pass the current device's screen orientation to the c++ part
        // save frames to external
        //saveMatToExternal(rgbFrameMat, countVINS);
    }
    protected void onResume() {
        super.onResume();
    }
    /**
     * shutting down onPause
     */
    protected void onPause() {
        if (null != camera) {
            camera.close();
            camera = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
        
        VinsJNI.onPause();
        super.onPause();
    }

    /**
     * @return true if permissions where given
     */
    private boolean checkPermissionsIfNeccessary() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                List<String> permissionsNotGrantedYet = new ArrayList<>(info.requestedPermissions.length);
                for (String p : info.requestedPermissions) {
                    Log.i(TAG, String.format("Requested permission: %s",p));
                    if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, String.format("Not-yet-granted permission: %s",p));
                        permissionsNotGrantedYet.add(p);
                    }
                }
                if(permissionsNotGrantedYet.size() > 0){
                    if (Build.VERSION.SDK_INT < 23) {
                        ActivityCompat.requestPermissions(this, permissionsNotGrantedYet.toArray(new String[permissionsNotGrantedYet.size()]),
                                PERMISSIONS_REQUEST_CODE);
                    } else {
                        Log.i(TAG, "Requesting permissions");
                        requestPermissions(permissionsNotGrantedYet.toArray(new String[permissionsNotGrantedYet.size()]),
                                PERMISSIONS_REQUEST_CODE);
                    }
                    return false;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if(requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean hasAllPermissions = true;
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length == 0)
                hasAllPermissions = false;
            for (String p: permissions){
                Log.i(TAG, String.format("permission: %s",p));
            }
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED)
                    hasAllPermissions = false;
            }
            Log.i(TAG, String.format("hasAllPermission?: %b",hasAllPermissions));
            if(!hasAllPermissions){
                finish();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        if(ARchoice) {
            float x = e.getX();
            float y = e.getY();
            Log.d("Xukan disply", "displayHeight: " + displayHeight + "; displayWidth: " + displayWidth);
            float yratio = y / displayHeight * imageWidth;//*640
            float xratio = x / displayWidth * imageHeight;//* 480
            VinsJNI.setARTouchPosition(xratio, yratio);
        }
        return true;
    }

    /** Represents a device we can talk to. */
    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }

    public String getWiFiDirectMacAddress(){
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

                if (ntwInterface.getName().equalsIgnoreCase("p2p0")) {
                    byte[] byteMac = ntwInterface.getHardwareAddress();
                    if (byteMac==null){
                        return null;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    for (int i=0; i<byteMac.length; i++) {
                        strBuilder.append(String.format("%02X:", byteMac[i]));
                    }

                    if (strBuilder.length()>0){
                        strBuilder.deleteCharAt(strBuilder.length()-1);
                    }

                    return strBuilder.toString();
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    public static byte[] FloatArray2ByteArray(float[] values){
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

        for (float value : values){
            buffer.putFloat(value);
        }

        return buffer.array();
    }
}
