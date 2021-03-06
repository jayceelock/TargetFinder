package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;

class ClassTangoUpdateCallback extends Tango.TangoUpdateCallback
{
    private static final String TAG = ClassTangoUpdateCallback.class.getSimpleName();

    private ActivityCamera activityCamera;

    ClassTangoUpdateCallback(Context context)
    {
        this.activityCamera = (ActivityCamera)context;
    }

    @Override
    public void onFrameAvailable(int cameraId)
    {
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            if (activityCamera.getSurfaceView().getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityCamera.getSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
            activityCamera.getFrameAvailableTangoThread().set(true);
            activityCamera.getSurfaceView().requestRender();
        }
    }

    @Override
    public void onPoseAvailable(TangoPoseData pose)
    {
        activityCamera.getRunnableSoundGenerator().setTangoPose(pose);
        activityCamera.getMetrics().updateTimeStamp(pose.timestamp);
        activityCamera.getMetrics().updateTangoPose(pose);

        activityCamera.getMetrics().writeWifi();
    }
}
