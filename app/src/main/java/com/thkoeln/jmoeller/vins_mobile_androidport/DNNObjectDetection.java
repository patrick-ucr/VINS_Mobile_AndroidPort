package com.thkoeln.jmoeller.vins_mobile_androidport;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;


import com.thkoeln.jmoeller.vins_mobile_androidport.env.ImageUtils;
import com.thkoeln.jmoeller.vins_mobile_androidport.env.Logger;
import com.thkoeln.jmoeller.vins_mobile_androidport.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DNNObjectDetection {
    private final String TAG = "DNNObjectDetection";
    private final Logger logger = new Logger();
    private Classifier detector; // DNN
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private int cropSize = TF_OD_API_INPUT_SIZE;
    private int previewWidth;
    private int previewHeight;
    private Integer sensorOrientation;

    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Runnable dnnDetector;
    public boolean running_dnn = false;

    private MultiBoxTracker tracker;
    private byte[] luminanceCopy;

    private final boolean SAVE_BITMAP = false;

    private String target = null;

    public DNNObjectDetection(Context context) {
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            tracker = new MultiBoxTracker(context);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception initializing classifier!");
        }
    }

    public void init(final Activity activity,
                     final int w,
                     final int h,
                     final String t){
        previewWidth = w;
        previewHeight = h;
        target = t;
        sensorOrientation = 90 - getScreenOrientation(activity);
        Log.i(TAG, "Sensor orientation " + sensorOrientation);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

    }
    protected int getScreenOrientation(Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    public List<RectF> getTrackedBbox(){
        List<RectF> listBbox = tracker.getTrackedBoundingBoxes();
        if (listBbox.size() <= 0){
            return new ArrayList<>();
        }
        return new ArrayList<>(listBbox);
    }
    public synchronized void detect(final Bitmap bitmap,final byte[] originalLuminance, final long currTimestamp){
        if (running_dnn)
            return;

        if (luminanceCopy == null){
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance,0,luminanceCopy,0,originalLuminance.length);

        rgbFrameBitmap = bitmap;
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        dnnDetector = new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                logger.i("Running detection on image " + currTimestamp);
                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                logger.i("Running detection finish in %d ms ", SystemClock.uptimeMillis()-startTime);

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);

                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = 0.5f;
                final List<Classifier.Recognition> mappedRecognitions =
                        new LinkedList<Classifier.Recognition>();

                for (final Classifier.Recognition result : results) {
                    final RectF location = result.getLocation();
                    if (location != null &&
                            result.getConfidence() >= minimumConfidence &&
                                result.getTitle().equals(target)) {
                        canvas.drawRect(location, paint);

                        cropToFrameTransform.mapRect(location);

                        result.setLocation(location);
                        Log.i(TAG, String.format("Detected object %s at location: %s",result.getTitle(),location.toString()));
                        mappedRecognitions.add(result);
                    }
                }

                if (SAVE_BITMAP) {
                    String filename = "image-" + String.valueOf(currTimestamp) + ".png";
                    ImageUtils.saveBitmap(cropCopyBitmap, filename);
                }
                tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                running_dnn = false;
            }
        };

        if (!running_dnn){
            running_dnn = true;
            dnnDetector.run();
        }
    }

    public synchronized void onFrame(final int w,
                        final int h,
                        final int rowStride,
                        Activity activity,
                        final byte[] frame,
                        final long timestamp){
        sensorOrientation = 90 - getScreenOrientation(activity);

        tracker.onFrame(w,h,rowStride,sensorOrientation,frame,timestamp);

    }

    public synchronized void draw(Canvas canvas){
        tracker.draw(canvas);
    }
}
