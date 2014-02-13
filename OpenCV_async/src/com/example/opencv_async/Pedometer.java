package com.example.opencv_async;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
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
	public float[] magnet;
	
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
    private Sensor magnetSensor;
    
    public TextView compassTextBox = null;
    public TextView testTextBox2 = null;
    public ImageView redDot = null;
    
    GraphView gv;
    GraphViewSeries exampleSeries;
    GraphViewSeries gyroSeries;
    
    Context context;
    
    //public Drawing drawView;
    
     
    String returnString;
    SharedValue sv;
    
    public void getHTTPdata(int id){
    	 ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
    	 postParameters.add(new BasicNameValuePair("id",id+""));
    	 String response = null;
    	 testTextBox2.setText("getHTTPdata");
    	 
    	 try {
    	     response = CustomHttpClient.executeHttpPost(
    	       //"http://129.107.187.135/CSE5324/jsonscript.php", // your ip address if using localhost server
    	       sv.getServerAddr(),  // in case of a remote server
    	       postParameters);
    	     
    	     // store the result returned by PHP script that runs MySQL query
    	     String result = response.toString();  
    	     //parse json data
             try{
                     returnString = "";
               JSONArray jArray = new JSONArray(result);
                     for(int i=0;i<jArray.length();i++){
                             JSONObject json_data = jArray.getJSONObject(i);
                             //Log.i("log_tag","id: "+json_data.getInt("id")+
                             //        ", name: "+json_data.getString("name")+
                             //        ", sex: "+json_data.getInt("sex")+
                             //        ", birthyear: "+json_data.getInt("birthyear")
                             //);
                             //Get an output to the screen
                             returnString += "\n" + json_data.getInt("identyfikator") + " -> "+ json_data.getInt("x") + " -> "+ json_data.getInt("y");
                     }
             }
             catch(JSONException e){
                     Log.e("log_tag", "Error parsing data "+e.toString());
             }
         
             try{
            	 testTextBox2.setText(returnString);
             }
             catch(Exception e){
              Log.e("log_tag","Error in Display!" + e.toString());;          
             }   
        }
              catch (Exception e) {
         Log.e("log_tag","Error in http connection!!" + e.toString());     
        }

    }
    
    
	public Pedometer(Context c, ImageView dot){
		this.context = c;
		sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
		accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		//gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		//fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);
		
		accel = new float[3];
		//gyro = new float[3];
		magnet = new float[3];
		
		this.redDot = dot;
		
		sv = new SharedValue();
		
		d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(2);
        d.setMinimumFractionDigits(2);
        
        matrixR = new float[9];
        matrixI = new float[9];
        matrixValues = new float[3];
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
                
       	
        	//gx = lowPassFilterAlpha * gx + (1 - lowPassFilterAlpha) * gyro[0];
            //gy = lowPassFilterAlpha * gy + (1 - lowPassFilterAlpha) * gyro[1];            
            //gz = lowPassFilterAlpha * gz + (1 - lowPassFilterAlpha) * gyro[2]; 
        	
         //   gyroVectValue = (float)Math.sqrt((gx*gx)+(gy*gy)+(gz*gz));                   
            
        	mHandler.post(new Runnable() {
                    public void run() {
                    	//testTextBox2.setText("kroki: "+stepCounter);
                    	//compassTextBox.setText("czas: "+interval);
                    	exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
                    	//gyroSeries.appendData(new GraphViewData(graphIndex, gyroVectValue), true, 1024);
                        graphIndex++; 

                        //testTextBox2.setText("dot X: "+redDot.getLeft()+" Y: "+redDot.getTop());
                    
                    }
           });
        	
        	//**************************************************************************************
        	

        }
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	 private float[] matrixR;
	 private float[] matrixI;
	 private float[] matrixValues;
	 private float azimuth = 0f;
	 DecimalFormat d = new DecimalFormat("#.##");
	 
	 
	@Override
	public void onSensorChanged(SensorEvent event) {
    	switch(event.sensor.getType()) {
        case Sensor.TYPE_LINEAR_ACCELERATION:
        	System.arraycopy(event.values, 0, accel, 0, 3);
        	break;
        case Sensor.TYPE_MAGNETIC_FIELD:
        	System.arraycopy(event.values, 0, magnet, 0, 3);
        	break;	
    	}
    	
    	/*
	    if(SensorManager.getRotationMatrix(matrixR, matrixI, accel, magnet)) {
	        SensorManager.getOrientation(matrixR, matrixValues);
	        azimuth = (float) Math.toDegrees(matrixValues[0]); // orientation
            azimuth = (azimuth + 360) % 360;
           	compassTextBox.setText("=>azimuth: "+d.format(azimuth));
	    }*/
	    
    	ax = lowPassFilterAlpha * ax + (1 - lowPassFilterAlpha) * accel[0];
        ay = lowPassFilterAlpha * ay + (1 - lowPassFilterAlpha) * accel[1];            
        az = lowPassFilterAlpha * az + (1 - lowPassFilterAlpha) * accel[2];            
        
        accVectValue = (float)Math.sqrt((ax*ax)+(ay*ay)+(az*az));      
        
        exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
    	//gyroSeries.appendData(new GraphViewData(graphIndex, gyroVectValue), true, 1024);
        graphIndex++; 
	}
	

    public void start() {
    		sensorManager.registerListener(this, accSensor,
    						SensorManager.SENSOR_DELAY_FASTEST);
    		sensorManager.registerListener(this, magnetSensor,
							SensorManager.SENSOR_DELAY_FASTEST);
    		
    		x = redDot.getLeft();
            y = redDot.getTop();
           // testTextBox2.setText("dot X: "+x+" Y: "+y);
    }

    public void stop() {   
    	sensorManager.unregisterListener(this);                
    }
}
