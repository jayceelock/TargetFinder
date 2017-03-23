package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoPoseData;

class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();

    private TangoPoseData tangoPose;

    private ActivityCamera activityCamera;

    RunnableSoundGenerator(Context context)
    {
        activityCamera = (ActivityCamera)context;
    }

    @Override
    public void run()
    {
        try
        {
            mVector targetPoseVector = new mVector(activityCamera.getRenderer().getObjectPosition().x, activityCamera.getRenderer().getObjectPosition().y, activityCamera.getRenderer().getObjectPosition().z);

            double elevationAngle = ClassHelper.getElevationAngle(targetPoseVector, tangoPose);
            double xPositionListener = ClassHelper.getXPosition(targetPoseVector, tangoPose);
            double xPositionSource = activityCamera.getRenderer().getObjectPosition().x;

            Log.d(TAG, String.format("xPos: %f", xPositionListener));

            float[] tempSrc = new float[3];
            float[] tempList = new float[3];

            for(int i = 0; i < tangoPose.translation.length; i ++)
            {
                tempSrc[i] = (float)(activityCamera.getRenderer().getObjectPosition().toArray()[i]);
                tempList[i] = (float)(tangoPose.translation[i]);
            }

            // Get distance to objective, give voice confirm if it's close
            double xDist = activityCamera.getRenderer().getObjectPosition().toArray()[0] - tangoPose.translation[0];
            double yDist =  activityCamera.getRenderer().getObjectPosition().toArray()[1] - tangoPose.translation[2];
            double zDist = activityCamera.getRenderer().getObjectPosition().toArray()[2] - tangoPose.translation[1];

            float distanceToObjective = (float)(Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist));

            float pitch = activityCamera.getInterfaceParameters().getPitch(elevationAngle);
            float gain = activityCamera.getInterfaceParameters().getGain(distanceToObjective);

            tempSrc[0] = (float)xPositionSource;
            tempList[0] = (float)xPositionListener;

            JNINativeInterface.play(tempSrc, tempList, gain, pitch);
        }

        catch(TangoException e)
        {
            Log.e(TAG, "Error getting the Tango pose: " + e);
        }
    }

    void setTangoPose(TangoPoseData tangoPose)
    {
        this.tangoPose = tangoPose;
        this.run();
    }
}
