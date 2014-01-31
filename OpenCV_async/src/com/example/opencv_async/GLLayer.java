package com.example.opencv_async;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;

/**
 * This class uses OpenGL ES to render the camera's viewfinder image on the
 * screen. Unfortunately I don't know much about OpenGL (ES). The code is mostly
 * copied from some examples. The only interesting stuff happens in the main
 * loop (the run method) and the onPreviewFrame method.
 */
public class GLLayer extends GLSurfaceView implements Renderer {
	private Cube mCube = new Cube();
    private float mCubeRotation;
	int onDrawFrameCounter=1;
	int[] cameraTexture;
	byte[] glCameraFrame=new byte[256*256]; //size of a texture must be a power of 2
	FloatBuffer cubeBuff;
	FloatBuffer texBuff;
	
	public static final int CUBE_OFF = 0;
	public static final int CUBE_ON = 1;
	public static int cubeStatus;
	
	public int getCubeStatus() {
		return this.cubeStatus;
	}

	public void setCubeStatus(int x) {
		if (x != 0 && x != 1)
			this.cubeStatus = 0;
		else
			this.cubeStatus = x;
	}

	
	public GLLayer(Context c) {
		super(c);
		Log.w("OPENGL!!!", "GLLayer start;");
		
		
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        
        this.setRenderer(this);        
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        //setKeepScreenOn(true);
        Log.d("OPENGL!!!", "PixelFormat.TRANSLUCENT;");
        
        cubeStatus = CUBE_OFF;
	}

    public void onDrawFrame( GL10 gl ) {
        // This method is called per frame, as the name suggests.
        // For demonstration purposes, I simply clear the screen with a random translucent gray.
        //float c = 1.0f / 256 * ( System.currentTimeMillis() % 256 );
        //gl.glClearColor( c, c, c, 0.5f );
       //gl.glClear( GL10.GL_COLOR_BUFFER_BIT );
    	
    	
	    	gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);   
	    	if(cubeStatus == CUBE_ON)
	    	{
	        gl.glLoadIdentity();
	        
	        gl.glTranslatef(0.0f, 0.0f, -10.0f);
	        gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);
	            
	        mCube.draw(gl);
	           
	        gl.glLoadIdentity();                                    
	            
	        mCubeRotation -= 0.15f; 
    	}
    }
 
    public void onSurfaceChanged( GL10 gl, int width, int height ) {
        // This is called whenever the dimensions of the surface have changed.
        // We need to adapt this change for the GL viewport.
        //gl.glViewport( 0, 0, width, height );
    	gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
 
    public void onSurfaceCreated( GL10 gl, EGLConfig config ) {
    	gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); 
        
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                  GL10.GL_NICEST);
    }
    
    private void openGLinit(){
    	
    }
	
}