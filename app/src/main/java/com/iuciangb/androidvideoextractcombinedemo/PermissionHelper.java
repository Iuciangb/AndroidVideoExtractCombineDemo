package com.iuciangb.androidvideoextractcombinedemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * @author YY
 * @create 2019/4/9
 * @Describe
 **/
public class PermissionHelper {
    public static void requestPermission(Activity activity) {
        if (hasAllPermission(activity)) {
            return;
        }
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO}, 0);
    }

    private static boolean hasAllPermission(Context context) {
        return hasCameraPermission(context) && hasWriteExternalStoragePermission(context) && hasRecordAudioPermission(context);
    }

    private static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasWriteExternalStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
}
