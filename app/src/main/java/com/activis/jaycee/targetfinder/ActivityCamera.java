package com.activis.jaycee.targetfinder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityCamera extends Activity implements TextToSpeech.OnInitListener
{
    private static final String TAG = ActivityCamera.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private AtomicBoolean frameAvailableTangoThread = new AtomicBoolean(false);

    private boolean tangoConnected = false;

    private int connectedTextureIdGlThread = INVALID_TEXTURE_ID;

    private Tango tango;
    private TangoCameraIntrinsics tangoCameraIntrinsics;

    private SurfaceView surfaceView;
    private TextToSpeech tts;

    private ClassFrameCallback sceneFrameCallback = new ClassFrameCallback(ActivityCamera.this);
    private RunnableSoundGenerator runnableSoundGenerator = new RunnableSoundGenerator(ActivityCamera.this);
    private RunnableSpeechGenerator runnableSpeechGenerator = new RunnableSpeechGenerator(ActivityCamera.this);
    private ClassInterfaceParameters interfaceParameters;
    private ClassRenderer renderer;
    private ClassHelper helper = new ClassHelper(ActivityCamera.this);

    private int displayRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        renderer = new ClassRenderer(this);

        interfaceParameters = new ClassInterfaceParameters(ActivityCamera.this);
        tts = new TextToSpeech(getApplicationContext(), this);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null)
        {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener()
            {
                @Override
                public void onDisplayAdded(int displayId) {  }

                @Override
                public void onDisplayChanged(final int displayId)
                {
                    synchronized (this)
                    {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) { }
            }, null);
        }

        renderer.getCurrentScene().registerFrameCallback(sceneFrameCallback);
        surfaceView.setSurfaceRenderer(renderer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        boolean alInit = JNINativeInterface.init();

        if(checkAndRequestPermissions())
        {
            initialiseTango();
        }

        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause()
    {
        boolean alKill = JNINativeInterface.kill();

        if(tangoConnected)
        {
            synchronized (ActivityCamera.this)
            {
                try
                {
                    tango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    connectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    tango.disconnect();

                    tangoConnected = false;
                }
                catch(TangoException e)
                {
                    Log.e(TAG, "Tango disconnect error: " + e);
                }
            }
        }

        if(tts.isSpeaking())
        {
            tts.shutdown();
            tts.stop();
        }

        super.onPause();
    }

    @Override
    public void onInit(int status)
    {
        if (status == TextToSpeech.SUCCESS)
        {
            tts.setLanguage(Locale.UK);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();

        switch(action)
        {
            case MotionEvent.ACTION_DOWN:
                // metrics.updateTargetPosition(currentTarget);
                renderer.updateTarget(helper.selectRandomTarget());
                break;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(hasPermissions())
        {
            initialiseTango();
        }
    }

    public void initialiseTango()
    {
        tango = new Tango(ActivityCamera.this, new Runnable()
        {
            @Override
            public void run()
            {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (ActivityCamera.this)
                {
                    TangoConfig tangoConfig = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_SMOOTH_POSE, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

                    try
                    {
                        ArrayList<TangoCoordinateFramePair> framePairList = new ArrayList<>();
                        framePairList.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));

                        tango.connectListener(framePairList, new ClassTangoUpdateCallback(ActivityCamera.this));

                        tango.connect(tangoConfig);
                        tangoConnected = true;

                        TangoSupport.initialize(tango);

                        setDisplayRotation();
                    }
                    catch (TangoOutOfDateException e)
                    {
                        Log.e(TAG, "Tango core out of date, please update: " + e);
                    }

                    catch (TangoErrorException e)
                    {
                        Log.e(TAG, "Could not connect to Tango service: " + e);
                    }
                }
            }
        });
    }

    public boolean checkAndRequestPermissions()
    {
        if(hasPermissions())
        {
            ActivityCompat.requestPermissions(ActivityCamera.this, new String[]{Manifest.permission.CAMERA}, 0);

            return false;
        }
        return true;
    }

    public boolean hasPermissions()
    {
        return ContextCompat.checkSelfPermission(ActivityCamera.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    public void setDisplayRotation()
    {
        Display display = getWindowManager().getDefaultDisplay();
        displayRotation = display.getRotation();

        /* We also need to update the camera texture UV coordinates. This must be run in the OpenGL thread */
        surfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                if (tangoConnected)
                {
                    renderer.updateColorCameraTextureUvGlThread(displayRotation);
                }
            }
        });
    }

    public Tango getTango(){ return this.tango; }
    public ClassRenderer getRenderer() { return  this.renderer; }
    public boolean getTangoConnected() { return this.tangoConnected; }
    public AtomicBoolean getFrameAvailableTangoThread() { return this.frameAvailableTangoThread; }
    public int getConnectedTextureIdGlThread() { return this.connectedTextureIdGlThread; }
    public void setConnectedTextureIdGlThread(int connectedTextureIdGlThread) { this.connectedTextureIdGlThread = connectedTextureIdGlThread; }
    public SurfaceView getSurfaceView() { return this.surfaceView; }
    public ClassInterfaceParameters getInterfaceParameters() { return this.interfaceParameters; }
    public RunnableSoundGenerator getRunnableSoundGenerator() { return this.runnableSoundGenerator; }
    public RunnableSpeechGenerator getRunnableSpeechGenerator() { return this.runnableSpeechGenerator; }
    public TextToSpeech getTextToSpeech() { return this.tts; }
    public int getDisplayRotation() { return this.displayRotation; }
}
