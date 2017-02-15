package com.activis.jaycee.targetfinder;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityCamera extends Activity
{
    private static final String TAG = ActivityCamera.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private AtomicBoolean frameAvailableTangoThread = new AtomicBoolean(false);

    private boolean tangoConnected = false;

    private int connectedTextureIdGlThread = INVALID_TEXTURE_ID;

    private Tango tango;
    private TangoCameraIntrinsics tangoCameraIntrinsics;

    private RajawaliSurfaceView surfaceView;
    private ClassRenderer renderer;

    private ClassFrameCallback sceneFrameCallback = new ClassFrameCallback(ActivityCamera.this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = (RajawaliSurfaceView)findViewById(R.id.surfaceview);
        renderer = new ClassRenderer(this);

        renderer.getCurrentScene().registerFrameCallback(sceneFrameCallback);
        surfaceView.setSurfaceRenderer(renderer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        boolean alInit = JNINativeInterface.init();

        surfaceView.onResume();
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        tango = new Tango(ActivityCamera.this, new Runnable()
        {
            @Override
            public void run()
            {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (ActivityCamera.this)
                {
                    TangoSupport.initialize();

                    TangoConfig tangoConfig = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);

                    try
                    {
                        ArrayList<TangoCoordinateFramePair> framePairList = new ArrayList<>();
                        framePairList.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));

                        tango.connectListener(framePairList, new Tango.TangoUpdateCallback()
                        {
                            @Override
                            public void onFrameAvailable(int cameraId)
                            {
                                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
                                {
                                    if (surfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
                                    {
                                        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                                    }
                                    frameAvailableTangoThread.set(true);
                                    surfaceView.requestRender();
                                }
                            }
                        });

                        tango.connect(tangoConfig);
                        tangoConnected = true;
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

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (ActivityCamera.this)
                {
                    tangoCameraIntrinsics = tango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                }
            }
        });
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
        super.onPause();
    }

    public Tango getTango(){ return this.tango; }
    public TangoCameraIntrinsics getTangoCameraIntrinsics() { return this.tangoCameraIntrinsics; }
    public ClassRenderer getRenderer() { return  this.renderer; }
    public boolean getTangoConnected() { return this.tangoConnected; }
    public AtomicBoolean getFrameAvailableTangoThread() { return this.frameAvailableTangoThread; }
    public int getConnectedTextureIdGlThread() { return this.connectedTextureIdGlThread; }
    public void setConnectedTextureIdGlThread(int connectedTextureIdGlThread) { this.connectedTextureIdGlThread = connectedTextureIdGlThread; }
}
