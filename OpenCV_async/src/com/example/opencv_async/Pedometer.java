package com.example.opencv_async;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

/**
 - dorobic zapis do pliku ???
*/
public class Pedometer implements SensorEventListener {
	
	float accVectValue;
	float gyroVectValue;
	public float[] accel;
	public float[] gyro;
	public static final int TIME_CONSTANT = 30;
    float lowPassFilterAlpha = 0.3f;
	float ax,ay,az;
	float gx,gy,gz;
	
    int graphIndex;
    private long startEvetn;
    private long lastEvetn;
    private long now;
    private long interval;
    
    boolean isFirstSet = true;    
    boolean stopFlag = false;
    boolean startFlag = false;
    
    public int stepCounter = 0;
    
	private Handler mHandler = new Handler();
	private Timer fuseTimer = new Timer();
	
    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    
    public TextView compassTextBox = null;
    public TextView testTextBox2 = null;
    public ImageView redDot = null;
    
    GraphView gv;
    GraphViewSeries exampleSeries;
    GraphViewSeries gyroSeries;
    
    Context context;
    
    private static final Classifier classifier = new Classifier();
    
	public Pedometer(Context c, ImageView dot){
		this.context = c;
		sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
		accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		
		fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);
		
		accel = new float[3];
		gyro = new float[3];
		
		this.redDot = dot;
	}
	
	public void setAlpha(float a){
		this.lowPassFilterAlpha = a;	
	}
	
	public float getAlpha(){		
		return this.lowPassFilterAlpha;
	}
	int x=0;
	int y=0;
	class calculateFusedOrientationTask extends TimerTask {
        public void run() {
        	if(isFirstSet)
        	{
        		startEvetn = System.currentTimeMillis();
        		isFirstSet=false;
        	}
        	now = System.currentTimeMillis();
        	interval = now - lastEvetn;
        	lastEvetn = now;
        	
        	
        	
        	//gx = gx + lowPassFilterAlpha  * (accel[0] - gx);
            //gy = gy + lowPassFilterAlpha  * (accel[1] - gy);
            //gz = gz + lowPassFilterAlpha  * (accel[2] - gz);
            
            ax = lowPassFilterAlpha * ax + (1 - lowPassFilterAlpha) * accel[0];
            ay = lowPassFilterAlpha * ay + (1 - lowPassFilterAlpha) * accel[1];            
            az = lowPassFilterAlpha * az + (1 - lowPassFilterAlpha) * accel[2];            
            
            accVectValue = (float)Math.sqrt((ax*ax)+(ay*ay)+(az*az));               	        	                                                       
                
        	//if( classifier.isStep(accel[0], accel[1], accel[2]) ) {
        		//currentTime = System.currentTimeMillis();
        	//	stepCounter++;
        	//}
        	
        	gx = lowPassFilterAlpha * gx + (1 - lowPassFilterAlpha) * gyro[0];
            gy = lowPassFilterAlpha * gy + (1 - lowPassFilterAlpha) * gyro[1];            
            gz = lowPassFilterAlpha * gz + (1 - lowPassFilterAlpha) * gyro[2]; 
        	
            gyroVectValue = (float)Math.sqrt((gx*gx)+(gy*gy)+(gz*gz));        
            //gyroVectValue = (float)Math.sqrt((gyro[0]*gyro[0])+(gyro[1]*gyro[1])+(gyro[2]*gyro[2]));
            
            
        	mHandler.post(new Runnable() {
                    public void run() {
                    	//testTextBox2.setText("kroki: "+stepCounter);
                    	//compassTextBox.setText("czas: "+interval);
                    	exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
                    	gyroSeries.appendData(new GraphViewData(graphIndex, gyroVectValue), true, 1024);
                        graphIndex++; 

                       // Animation animation = new TranslateAnimation(redDot.getLeft(),redDot.getLeft()+50,
                       // 											 redDot.getTop(),redDot.getTop()+50);
                        
                        
                        //animation.setDuration(1);
                        
                       // animation.setRepeatCount(5);
                        //animation.setRepeatMode(2);
                       // redDot.startAnimation(animation);
                        
                        testTextBox2.setText("dot X: "+redDot.getLeft()+" Y: "+redDot.getTop());
                    
                    }
           });
        	
        	//**************************************************************************************
        	

        }
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
    	switch(event.sensor.getType()) {
        case Sensor.TYPE_LINEAR_ACCELERATION:
        	System.arraycopy(event.values, 0, accel, 0, 3);
        	break;
        case Sensor.TYPE_GYROSCOPE:
        	//Log.e("GYRO", "GYRO DATA COLLECTED !!!");
        	System.arraycopy(event.values, 0, gyro, 0, 3);
        	break;
    	}
	}
	

    public void start() {
    		sensorManager.registerListener(this, accSensor,
    						SensorManager.SENSOR_DELAY_FASTEST);
    		sensorManager.registerListener(this, gyroSensor,
							SensorManager.SENSOR_DELAY_FASTEST);
    		
    		x = redDot.getLeft();
            y = redDot.getTop();
            testTextBox2.setText("dot X: "+x+" Y: "+y);
    }

    public void stop() {   
    	sensorManager.unregisterListener(this);                
    }
}
