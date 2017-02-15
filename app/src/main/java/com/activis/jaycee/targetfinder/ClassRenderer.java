package com.activis.jaycee.targetfinder;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

import javax.microedition.khronos.opengles.GL10;

class ClassRenderer extends RajawaliRenderer
{
    private static final String TAG = ClassRenderer.class.getSimpleName();

    private ATexture tangoCameraTexture;

    private boolean sceneCameraConfigured = false;

    ClassRenderer(Context context)
    {
        super(context);
    }

    @Override
    protected void initScene()
    {
        ScreenQuad backgroundQuad = new ScreenQuad();

        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);

        tangoCameraTexture = new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);

        try
        {
            tangoCameraMaterial.addTexture(tangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        }
        catch(ATexture.TextureException e)
        {
            Log.e(TAG, "Error creating camera texture: " + e);
        }

        getCurrentScene().addChildAt(backgroundQuad, 0);
        addChild(new double[]{0, 0, -2});
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset){ }

    @Override
    public void onTouchEvent(MotionEvent event){ }

    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height)
    {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        sceneCameraConfigured = false;
    }

    int getTextureId()
    {
        return tangoCameraTexture == null ? -1 : tangoCameraTexture.getTextureId();
    }

    boolean isSceneCameraConfigured()
    {
        return sceneCameraConfigured;
    }

    void updateRenderCameraPose(TangoPoseData tangoPose)
    {
        float[] rotation = tangoPose.getRotationAsFloats();
        float[] translation = tangoPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    void setProjectionMatrix(TangoCameraIntrinsics intrinsics)
    {
        Matrix4 projectionMatrix = ClassScenePoseCalculator.calculateProjectionMatrix(intrinsics.width, intrinsics.height, intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy);
        getCurrentCamera().setProjectionMatrix(projectionMatrix);
    }

    public void addChild(double[] coordinate)
    {
        /* Clear old objects before adding new one */
        if(ClassRenderer.this.getCurrentScene().getNumChildren() != 0)
        {
            ClassRenderer.this.getCurrentScene().clearChildren();
        }

        Material material = new Material();
        try
        {
            Texture t = new Texture("smiley", R.drawable.smiley);
            material.addTexture(t);
        }
        catch (ATexture.TextureException e)
        {
            Log.e(TAG, "Exception generating texture", e);
        }

        /* Set properties and position and add to scene */
        material.setColorInfluence(0);
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        Object3D obj = new Sphere(0.4f, 20, 20);
        obj.setMaterial(material);
        obj.setPosition(coordinate[0], coordinate[1], coordinate[2]);
        obj.rotate(Vector3.Axis.Y, -90);

        ClassRenderer.this.getCurrentScene().addChild(obj);

        Log.i(TAG, "Added marker.");
    }
}
