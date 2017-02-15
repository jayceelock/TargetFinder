package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoPoseData;

import static java.lang.Math.abs;
import static java.lang.Math.tan;

public class RunnableSpeechGenerator implements Runnable
{
    private static final String TAG = RunnableSpeechGenerator.class.getSimpleName();

    private ActivityCamera activityCamera;

    private TangoPoseData tangoPose;

    private boolean running = false;

    public RunnableSpeechGenerator(Context context)
    {
        activityCamera = (ActivityCamera)context;
    }

    @Override
    public void run()
    {
        TangoPoseData tangoPoseData = activityCamera.getTango().getPoseAtTime(0.0, new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));
        mVector targetPoseVector = new mVector(activityCamera.getRenderer().getObjectPosition().x, activityCamera.getRenderer().getObjectPosition().y, activityCamera.getRenderer().getObjectPosition().z);

        mQuaternion requiredRotation = ClassHelper.getQuaternionToTarget(targetPoseVector, tangoPoseData);

        boolean behind = false;
        TextToSpeech tts = activityCamera.getTextToSpeech();

        if(abs(requiredRotation.y) > 0.5)
        {
            // Behind
            tts.speak("Behind", TextToSpeech.QUEUE_ADD, null);
            behind = true;
        }

        if(requiredRotation.x > 0.1)
        {
            if(behind)
            {
                tts.speak("Up", TextToSpeech.QUEUE_ADD, null);
            }
            else
            {
                tts.speak("Down", TextToSpeech.QUEUE_ADD, null);
            }
        }
        else if(requiredRotation.x < -0.1)
        {
            if(behind)
            {
                tts.speak("Down", TextToSpeech.QUEUE_ADD, null);
            }
            else
            {
                tts.speak("Up", TextToSpeech.QUEUE_ADD, null);
            }
        }

        if(!behind)
        {
            if (requiredRotation.y > 0.1)
            {
                // Left
                tts.speak("Left", TextToSpeech.QUEUE_ADD, null);
            }
            else if (requiredRotation.y < -0.1)
            {
                // Right
                tts.speak("Right", TextToSpeech.QUEUE_ADD, null);
            }
        }

        running = false;
    }

    public void setTangoPose(TangoPoseData tangoPose)
    {
        this.tangoPose = tangoPose;

        if(!running)
        {
            this.run();
            running = true;
        }
    }
}
