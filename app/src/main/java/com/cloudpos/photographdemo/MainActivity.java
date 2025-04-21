package com.cloudpos.photographdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "photograph";
    private SurfaceHolder surfaceHolder;
    private LinearLayout ll_pre;
    private LinearLayout ll_post;
    private SurfaceView surfaceView;
    private Camera camera;
    private int cameraId = 0;
    private int pictureRotation = 0;
    private boolean isPreview = false;
    private Bundle bundle = null;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        initView();
    }

    private void initView() {
        ll_pre = (LinearLayout) findViewById(R.id.ll_pre);
        ll_post = (LinearLayout) findViewById(R.id.ll_post);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        Button btn_take = (Button) findViewById(R.id.btn_take);
        Button btn_scan = (Button) findViewById(R.id.btn_scan);
        Button btn_switch = (Button) findViewById(R.id.btn_switch);
        Button btn_store = (Button) findViewById(R.id.btn_store);
        Button btn_cancel = (Button) findViewById(R.id.btn_cancel);

        btn_take.setOnClickListener(this);
        btn_scan.setOnClickListener(this);
        btn_switch.setOnClickListener(this);
        btn_store.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceCallback());
        surfaceHolder.setKeepScreenOn(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take:
                if (camera != null) {
                    camera.takePicture(null, null, new MyPictureCallback());
                }
                break;
            case R.id.btn_scan:
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");
                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, 1);
                break;
            case R.id.btn_switch:
                showCameraSwitchDialog();
                break;
            case R.id.btn_cancel:
                ll_pre.setVisibility(View.VISIBLE);
                ll_post.setVisibility(View.GONE);
                camera.startPreview();
                break;
            case R.id.btn_store:
                byte[] data = bundle.getByteArray("bytes");
                try {
                    saveToSDCard(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ll_pre.setVisibility(View.VISIBLE);
                ll_post.setVisibility(View.GONE);
                camera.startPreview();
                break;
        }
    }

    private void openCamera(int id, SurfaceHolder holder) {
        releaseCamera();
        camera = Camera.open(id);
        cameraId = id;

        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupCameraParameters();
        camera.startPreview();
        isPreview = true;
    }

    private void setupCameraParameters() {
        initCameraRotation(camera, cameraId);

        Camera.Parameters params = camera.getParameters();
        Camera.Size bestSize = params.getSupportedPictureSizes().get(0);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size;
            }
        }

        params.setPictureSize(bestSize.width, bestSize.height);
        params.setRotation(pictureRotation);

        Camera.Size previewSize = params.getSupportedPreviewSizes().get(0);
        params.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(params);
    }

    private void initCameraRotation(Camera camera, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        int degrees;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
            default: degrees = 0;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        pictureRotation = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                ? (info.orientation + degrees) % 360
                : (info.orientation - degrees + 360) % 360;

        camera.setDisplayOrientation(result);
    }

    private void showCameraSwitchDialog() {
        View view = View.inflate(mContext, R.layout.half_dialog_view, null);
        final EditText editText = (EditText) view.findViewById(R.id.dialog_edit);
        new AlertDialog.Builder(mContext)
                .setTitle("Switch camera index")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Done", (dialog, which) -> {
                    String input = editText.getText().toString();
                    int newId = Integer.parseInt(input);
                    openCamera(newId, surfaceHolder);
                }).create().show();
    }

    public void saveToSDCard(byte[] data) throws IOException {
        String filename = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        File folder = new File(Environment.getExternalStorageDirectory() + "/Pictures/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File jpgFile = new File(folder, filename);
        try (FileOutputStream outputStream = new FileOutputStream(jpgFile)) {
            outputStream.write(data);
        }

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(jpgFile));
        mContext.sendBroadcast(intent);
    }

    private void releaseCamera() {
        if (camera != null) {
            if (isPreview) {
                camera.stopPreview();
            }
            camera.release();
            camera = null;
            isPreview = false;
        }
    }

    private final class MyPictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bundle = new Bundle();
            bundle.putByteArray("bytes", data);
            ll_pre.setVisibility(View.GONE);
            ll_post.setVisibility(View.VISIBLE);
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            openCamera(cameraId, holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && data != null) {
            Intent intent = new Intent(mContext, ShowPicture.class);
            intent.setData(data.getData());
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
