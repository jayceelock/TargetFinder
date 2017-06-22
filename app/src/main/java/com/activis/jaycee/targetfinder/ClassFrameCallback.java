package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.scene.ASceneFrameCallback;

class ClassFrameCallback extends ASceneFrameCallback
{
    private static final String TAG = ClassFrameCallback.class.getSimpleName();

    private double cameraPoseTimestamp, rgbTimestampGlThread;

    private final Context activityContext;
    private ActivityCamera activityCamera;

    public ClassFrameCallback(Context activityContext)
    {
        this.activityContext = activityContext;

        this.activityCamera = (ActivityCamera)activityContext;
    }

    @Override
    public void onPreFrame(long sceneTime, double deltaTime)
    {
        synchronized (activityContext)
        {
            // Don't execute any tango API actions if we're not connected to the service.
            if (!activityCamera.getTangoConnected())
            {
                return;
            }

            // Set-up scene camera projection to match RGB camera intrinsics.
            if (!activityCamera.getRenderer().isSceneCameraConfigured())
            {
                TangoCameraIntrinsics intrinsics = TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityCamera.getDisplayRotation());
                activityCamera.getRenderer().setProjectionMatrix(intrinsics);
            }

            // Connect the camera texture to the OpenGL Texture if necessary
            // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
            // texture with a different ID.
            if (activityCamera.getConnectedTextureIdGlThread() != activityCamera.getRenderer().getTextureId())
            {
                activityCamera.getTango().connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityCamera.getRenderer().getTextureId());
                activityCamera.setConnectedTextureIdGlThread(activityCamera.getRenderer().getTextureId());
            }

            // If there is a new RGB camera frame available, update the texture with it
            if (activityCamera.getFrameAvailableTangoThread().compareAndSet(true, false))
            {
                rgbTimestampGlThread = activityCamera.getTango().updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
            }

            // If a new RGB frame has been rendered, update the camera pose to match.
            if (rgbTimestampGlThread > cameraPoseTimestamp)
            {
                // Calculate the camera color pose at the camera frame update time in
                // OpenGL engine.
                TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(rgbTimestampGlThread,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        activityCamera.getDisplayRotation());
                if (lastFramePose.statusCode == TangoPoseData.POSE_VALID)
                {
                    // Update the camera pose from the renderer
                    activityCamera.getRenderer().updateRenderCameraPose(lastFramePose);
                    cameraPoseTimestamp = lastFramePose.timestamp;
                }
                else
                {
                    Log.w(TAG, "Can't get device pose at time: " + rgbTimestampGlThread);
                }
            }
        }
    }

    @Override
    public void onPreDraw(long sceneTime, double deltaTime) { }

    @Override
    public void onPostFrame(long sceneTime, double deltaTime) { }

    @Override
    public boolean callPreFrame()
    {
        return true;
    }
}
