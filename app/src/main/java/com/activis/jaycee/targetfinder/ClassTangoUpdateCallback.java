package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;

public class ClassTangoUpdateCallback extends Tango.TangoUpdateCallback
{
    private ActivityCamera activityCamera;

    public ClassTangoUpdateCallback(Context context)
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
}
