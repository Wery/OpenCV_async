package com.example.opencv_async;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.opencv_async.PedometerTest.dataPoints;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

public class Compass implements SensorEventListener {
		private Handler mHandler = new Handler();
		
        private static final String TAG = "Compass";

        public boolean writeData;
        
        private SensorManager sensorManager;
        private Sensor gsensor;
        private Sensor msensor;
        private Sensor accSensor;
        private Sensor linAccSensor;
        
        public static final int TIME_CONSTANT = 20;
        public static final float FILTER_COEFFICIENT = 0.98f;
        private Timer fuseTimer = new Timer();
        
        private float[] gyro = new float[3];
        private float[] gyroMatrix = new float[9];
        private float[] gyroOrientation = new float[3];
     
        private float[] magnet = new float[3];
        private float[] accel = new float[3];
        private float[] accMagOrientation = new float[3];
     
        private float[] fusedOrientation = new float[3];
     
        private float[] rotationMatrix = new float[9];
        
        private float[] linAccel = new float[3];  
        
        private float azimuth = 0f;
        private float currectAzimuth = 0;
        
        // compass arrow to rotate
        public ImageView arrowView = null;
        public TextView compassTextBox = null;
        public TextView testTextBox = null;
        public TextView testTextBox2 = null;
        public TextView gyroTxtView = null;

        GraphView gv;
        GraphViewSeries exampleSeries;
        // ******************************************************************************* LOGGER STUFF
        
        public File myFile;
        public FileOutputStream fOut;
        public OutputStreamWriter myOutWriter;
        public BufferedWriter myBufferedWriter;
        public PrintWriter myPrintWriter;
        public FileWriter fileWriter;
        
        private long currentTime;
        private long startTime;
        
        private long actualTime;
        private long firstStartTime;
        
        boolean isFirstSet = true;
        
        boolean stopFlag = false;
        boolean startFlag = false;
        
        public View imgView;
        Context context;
        
        DecimalFormat d = new DecimalFormat("#.##");
        
        // ********************************************************************************************
        public Compass(Context context,GraphView gv, GraphViewSeries series) {
        	
        	this.context = context;
        	this.gv = gv;
        	this.exampleSeries = series;
        	
        	//this.imgView = iv;
        	//arrowView = (ImageView) imgView.findViewById(R.id.radar);
        	
        	
            sensorManager = (SensorManager) context
                            .getSystemService(Context.SENSOR_SERVICE);
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            linAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            
            gyroOrientation[0] = 0.0f;
            gyroOrientation[1] = 0.0f;
            gyroOrientation[2] = 0.0f;
     
            // initialise gyroMatrix with identity matrix
            gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
            gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
            gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
            
            fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                   1000, TIME_CONSTANT);
            
            d.setRoundingMode(RoundingMode.HALF_UP);
            d.setMaximumFractionDigits(2);
            d.setMinimumFractionDigits(2);

            mIndoorTrackSettings = new SharedValue();
                       
    		meanFilterLinearAcceleration = new MeanFilter();
    		meanFilterAcceleration = new MeanFilter();
    	    meanFilterMagnetic = new MeanFilter();
    	    varianceAccel = new StdDev();	
        }

        public void start() {
        		sensorManager.registerListener(this, accSensor,
        						SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gsensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, msensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, linAccSensor,
                        SensorManager.SENSOR_DELAY_FASTEST);
            	compassTextBox.setText("Compass started...");  	
        }

        public void stop() {
        	sensorManager.unregisterListener(this);                
        }


        // ************************************************************************************ adjustArrow
        private void adjustArrow(float dir) {
                if (arrowView == null) {
                        Log.i(TAG, "arrow view is not set");
                        return;
                }
         
                Animation an = new RotateAnimation(-currectAzimuth, -dir,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f);
                        
               // compassTextBox.setText("Image az: "+azimuth);
                currectAzimuth = dir;
                        
                
                an.setDuration(50);
                an.setRepeatCount(0);
                an.setFillAfter(true);
                arrowView.startAnimation(an);                                            
        }

        // ********************************************************************************************
        public static final float EPSILON = 0.000000001f;
        
        private static final float NS2S = 1.0f / 1000000000.0f;
        private float timestamp;
        private boolean initState = true;
         
       
        public float lowPassFilterAlpha = 0.01f;
    	float ax;
    	float ay;
    	float az;
        float accVectValue = 0f;
        float accVectValue2 = 0f;
        float accVectValue3 = 0f;
        
        float alx = 0f;
        float aly = 0f;            
        float alz = 0f;
        float accLinVectValue = 0f;
        
        GraphViewData[] graphData = new GraphViewData[1024];
        private int graphIndex=0;
        private int index=0;
        long lastTime=0;
        
        float[] tmpAccel = new float[3];
        float[] components = new float[3];
        float[] absoluteFrameOrientation = new float[3];
        float[] linearAcceleration = new float[3];
        float newLinAccVectValue2 = 0f;
        float newLinAccVectValue = 0f;
        private MeanFilter meanFilterLinearAcceleration;
      //**************************************************************************************** MAIN CALC 
                
         	class calculateFusedOrientationTask extends TimerTask {
        
            public void run() {
                if(firstStartFlag)
                {
                	firstStartTime=System.currentTimeMillis();
                	firstStartFlag=false;
                	Log.e("GRAPH","first time set... " + firstStartTime);
                }
            	/*
            	float[] tmpFusedorientation = calculateOrientation();
            	
                azimuth = (float) Math.toDegrees(tmpFusedorientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;
                
            	//**************************************************************************************

                ax = lowPassFilterAlpha * ax + (1 - lowPassFilterAlpha) * accel[0];
                ay = lowPassFilterAlpha * ay + (1 - lowPassFilterAlpha) * accel[1];            
                az = lowPassFilterAlpha * az + (1 - lowPassFilterAlpha) * accel[2];            
                
                accVectValue = (float)Math.sqrt((ax*ax)+(ay*ay)+(az*az));   
                float[] tempAcc = { ax, ay, az};
                
                // *********************************************************************** checkForStep
        		accelList.add(tempAcc);

        		float[] tempGyroOrientation = new float[3];
        		System.arraycopy(tmpFusedorientation, 0, tempGyroOrientation, 0, 3);
        		gyroOrientationList.add(tempGyroOrientation);

        		calculateLinearAcceleration(tmpFusedorientation);
        		
                //******************************************************************************* save to file
                                            
        		 //*************************************************************************************
            	mHandler.post(new Runnable() {
	                    public void run() {
	                    	exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
	                    	//testTextBox2.setText("sample: "+index);
	                    	//adjustArrow();
	                    	testTextBox.setText("filter: "+d.format(azimuth));
	                        graphIndex++; 
	                    }
	           });
            	*/
            	//**************************************************************************************
            	mHandler.post(new Runnable() {
                    public void run() {
//                    	linearAcceleration2 = calculate(acceleration, magnetic);
                    	calculate(acceleration, magnetic); 
                    }
            	});            	
            }
        }
     
       	
       	//************************************************************************************************ 
       	
    	@Override
        public void onSensorChanged(SensorEvent event) {

        	switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            	//System.arraycopy(event.values, 0, acceleration, 0, 3);            	
                System.arraycopy(event.values, 0, accel, 0, 3);
                acceleration = meanFilterAcceleration.filterFloat(accel);
                calculateAccMagOrientation();
                break;
         
            case Sensor.TYPE_GYROSCOPE:
                //gyroFunction(event);
                break;
         
            case Sensor.TYPE_MAGNETIC_FIELD:
            	System.arraycopy(event.values, 0, magnet, 0, 3);
            	magnetic = meanFilterMagnetic.filterFloat(magnet);
            	break;
               
            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, linAccel, 0, 3);
                break;
            }        	
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        //************************************************************************************************        
    	public void setAlpha(float a){
    		this.lowPassFilterAlpha = a;	
    	}
    	
    	public float getAlpha(){		
    		return this.lowPassFilterAlpha;
    	}
        
    	//************************************************************************************ saveToFile
    	
        public void saveToFile(float direction, float[] a, float accVect, float[] al, float accLinVect){
            if(startFlag)
            {                	
                if (isFirstSet) {
                    startTime = System.currentTimeMillis();
                    isFirstSet = false;
                 
    	            try {
    	            	myFile = new File("/sdcard/test/test-"+startTime+".txt");
    	            	myFile.createNewFile();
    	            	
    	            } catch (Exception e) {
    	            	Log.e("MAKE_FILE","make file error!");
    	            }
                }
                currentTime = System.currentTimeMillis();
                
            	if (!stopFlag) {
                    try {
                    	index++;
                    	myBufferedWriter = new BufferedWriter(new FileWriter(myFile, true));
                        myBufferedWriter.append(currentTime - startTime + "," +
                    			a[0] + "," + a[1] + "," + a[2] + "," +
                    			//al[0] + "," + al[1] + "," + al[2] + "," + 
                    			//direction + "," + 
                    			//accVect + "," + 
                    			accLinVect+ "\r\n");                            
                        myBufferedWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}                      
                }
                else {
                	index=0;                    	

                }	
            	
            	gyroTxtView.setText("probka: "+index);
        	}  
        }
    
        public void saveToFile(float[] a, float accVect, long ctime){
            if(startFlag)
            {                	
                if (isFirstSet) {
                    //startTime = System.currentTimeMillis();
                    isFirstSet = false;
                 
    	            try {
    	            	//myFile = new File("/sdcard/test/test-"+startTime+".txt");
    	            	myFile = new File("/sdcard/test/test-"+firstStartTime+".txt");
    	            	myFile.createNewFile();
    	            	
    	            } catch (Exception e) {
    	            	Log.e("MAKE_FILE","make file error!");
    	            }
                }
                //currentTime = System.currentTimeMillis();
                
            	if (!stopFlag) {
                    try {
                    	index++;
                    	myBufferedWriter = new BufferedWriter(new FileWriter(myFile, true));
                    	//String s = (currentTime - startTime);

                    	
                    	//long diffTime = ctime - firstStartTime;
                    	//Log.e("GRAPH","diffTime: "+diffTime);
                    	//Log.e("GRAPH","ctime: "+ctime);
                    	StringBuilder strB = new StringBuilder();
                    	strB.append(ctime);
                    	strB.append(",");
                    	strB.append(a[0]);
                    	strB.append(",");
                    	strB.append(a[1]);
                    	strB.append(",");
                    	strB.append(a[2]);
                    	strB.append(",");
                    	strB.append(accVect);
                    	strB.append("\r\n");
                    	myBufferedWriter.append(strB);
                        //myBufferedWriter.append(currentTime - startTime + "," +
                    	//		a[0] + "," + a[1] + "," + a[2] + "," + accVect+ "\r\n");                            
                        myBufferedWriter.close();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}                      
                }
                else {
                	index=0;                    	

                }	        	
            	gyroTxtView.setText("probka: "+index);
        	}  
        }

      //************************************************************************************************
        float threshold = 0.08f;
        
        public float calcAzimutch(float fo){
            azimuth = (float) Math.toDegrees(fo); // orientation
            azimuth = (azimuth + 360) % 360;
            return azimuth; 
        }
        
        private List<float[]> accList = new ArrayList<float[]>();
        float accLinVectValuelast = 0f;
        boolean alphaStatic = false;
        private float dt = 0;
        private float timeConstant = 0.18f;
        private float timestamp2;
    	private float timestampOld;
    	private int count = 0;
    	private float alpha = 0.1f;
    	
    	float linX=0;
    	float linY=0;
    	float linZ=0;
    	
    	float gravityX=0;
    	float gravityY=0;
    	float gravityZ=0;
    	float newLinAcc = 0f;
    	
        SharedValue mIndoorTrackSettings;
    	private List<float[]> accelList = new ArrayList<float[]>(); // Acceleration list
    	private List<float[]> gyroOrientationList = new ArrayList<float[]>(); // Gyro direction list
  
    	
    	boolean isFirstRead = true;
	     // simple low-pass filter
    	
        float lowPass(float current, float last)
        {
        	return last * (1.0f - lowPassFilterAlpha) + current * lowPassFilterAlpha;
        }  
             
        public float[] calculateOrientation(){
        
        	float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;               
            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }
            
            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }
            
            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }
     
            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
            
            return fusedOrientation;
        }
          	
        public void calculateAccMagOrientation() {
    	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
    	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
    	        azimuth = (float) Math.toDegrees(accMagOrientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;
                testTextBox2.setText("** azimuth: "+d.format(azimuth));
    	    }
    	}
        
        private void getRotationVectorFromGyro(float[] gyroValues,
                float[] deltaRotationVector,
                float timeFactor)
    	{
    		float[] normValues = new float[3];
    		
    		// Calculate the angular speed of the sample
    		float omegaMagnitude =
    		(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
    		gyroValues[1] * gyroValues[1] +
    		gyroValues[2] * gyroValues[2]);
    		
    		// Normalize the rotation vector if it's big enough to get the axis
    		if(omegaMagnitude > EPSILON) {
    		normValues[0] = gyroValues[0] / omegaMagnitude;
    		normValues[1] = gyroValues[1] / omegaMagnitude;
    		normValues[2] = gyroValues[2] / omegaMagnitude;
    		}
    		
    		// Integrate around this axis with the angular speed by the timestep
    		// in order to get a delta rotation from this sample over the timestep
    		// We will convert this axis-angle representation of the delta rotation
    		// into a quaternion before turning it into the rotation matrix.
    		float thetaOverTwo = omegaMagnitude * timeFactor;
    		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
    		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
    		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
    		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
    		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
    		deltaRotationVector[3] = cosThetaOverTwo;
    	}

        public void gyroFunction(SensorEvent event) {
            // don't start until first accelerometer/magnetometer orientation has been acquired
            if (accMagOrientation == null)
                return;
         
            // initialisation of the gyroscope based rotation matrix
            if(initState) {
                float[] initMatrix = new float[9];
                initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
                float[] test = new float[3];
                SensorManager.getOrientation(initMatrix, test);
                gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
                initState = false;
            }
         
            // copy the new gyro values into the gyro array
            // convert the raw gyro data into a rotation vector
            float[] deltaVector = new float[4];
            if(timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
            }
         
            // measurement done, save current time for next interval
            timestamp = event.timestamp;
         
            // convert rotation vector into rotation matrix
            float[] deltaMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
         
            // apply the new rotation interval on the gyroscope based rotation matrix
            gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
         
            // get the gyroscope based orientation from the rotation matrix
            SensorManager.getOrientation(gyroMatrix, gyroOrientation);
            
            /*
            float[] fo = calculateOrientation();    
            float dir = calcAzimutch(fo[0]);
            testTextBox.setText("filter: "+d.format(dir));
            calcSteps(fo, dir);
            */
        }
                     
        private float[] getRotationMatrixFromOrientation(float[] o) {
            float[] xM = new float[9];
            float[] yM = new float[9];
            float[] zM = new float[9];
         
            float sinX = (float)Math.sin(o[1]);
            float cosX = (float)Math.cos(o[1]);
            float sinY = (float)Math.sin(o[2]);
            float cosY = (float)Math.cos(o[2]);
            float sinZ = (float)Math.sin(o[0]);
            float cosZ = (float)Math.cos(o[0]);
         
            // rotation about x-axis (pitch)
            xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
            xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
            xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
         
            // rotation about y-axis (roll)
            yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
            yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
            yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
         
            // rotation about z-axis (azimuth)
            zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
            zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
            zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
         
            // rotation order is y, x, z (roll, pitch, azimuth)
            float[] resultMatrix = matrixMultiplication(xM, yM);
            resultMatrix = matrixMultiplication(zM, resultMatrix);
            return resultMatrix;
        }
              
        private float[] matrixMultiplication(float[] A, float[] B) {
            float[] result = new float[9];
         
            result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
            result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
            result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
         
            result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
            result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
            result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
         
            result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
            result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
            result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
         
            return result;
        }
       
    	private void calculateLinearAcceleration(float[] tmporientation) {
    		//System.arraycopy(gyroOrientation, 0, absoluteFrameOrientation, 0, 3);
    		System.arraycopy(accel, 0, tmpAccel, 0, 3);
    		
    		
    		// values[0]: azimuth, rotation around the Z axis.
    		// values[1]: pitch, rotation around the X axis.
    		// values[2]: roll, rotation around the Y axis.

    		// Find the gravity component of the X-axis
    		// = g*-cos(pitch)*sin(roll);
    		components[0] = (float) (SensorManager.GRAVITY_EARTH
    				* -Math.cos(tmporientation[1]) * Math
    				.sin(tmporientation[2]));

    		// Find the gravity component of the Y-axis
    		// = g*-sin(pitch);
    		components[1] = (float) (SensorManager.GRAVITY_EARTH * -Math
    				.sin(tmporientation[1]));

    		// Find the gravity component of the Z-axis
    		// = g*cos(pitch)*cos(roll);
    		components[2] = (float) (SensorManager.GRAVITY_EARTH
    				* Math.cos(tmporientation[1]) * Math
    				.cos(tmporientation[2]));

    		// Subtract the gravity component of the signal
    		// from the input acceleration signal to get the
    		// tilt compensated output.
    		linearAcceleration[0] = (tmpAccel[0] - components[0]);
    		linearAcceleration[1] = (tmpAccel[1] - components[1]);
    		linearAcceleration[2] = (tmpAccel[2] - components[2]);

    		linearAcceleration = meanFilterLinearAcceleration
    				.filterFloat(linearAcceleration);
    		
    		accVectValue = (float)Math.sqrt((tmpAccel[0]*tmpAccel[0])+(tmpAccel[1]*tmpAccel[1])+(tmpAccel[2]*tmpAccel[2]));	
    		newLinAccVectValue2 = (float)Math.sqrt((components[0]*components[0])+(components[1]*components[1])+(components[2]*components[2]));    		
    		newLinAccVectValue = accVectValue - newLinAccVectValue2;
    		
    		
    		accVectValue = (float)Math.sqrt((linearAcceleration[0]*linearAcceleration[0])+(linearAcceleration[1]*linearAcceleration[1])+(linearAcceleration[2]*linearAcceleration[2]));
    		
        	mHandler.post(new Runnable() {
                public void run() {
                	exampleSeries.appendData(new GraphViewData(graphIndex, newLinAccVectValue), true, 1024);
                	//exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
                	//testTextBox2.setText("sample: "+index);
                	//adjustArrow();
                	testTextBox.setText("filter: "+d.format(azimuth));
                    graphIndex++; 
                }
       });
            //exampleSeries.appendData(new GraphViewData(graphIndex, newLinAccVectValue), true, 1024);
    		//graphIndex++;
    		
    		//saveToFile(linearAcceleration, newLinAccVectValue);
    		    		
    	}

// ******************************************************************************************** step detect
    	private MeanFilter meanFilterAcceleration;
    	private MeanFilter meanFilterMagnetic;
    	
    	private StdDev varianceAccel;
    	
    	private float[] r = new float[9];
    	// The gravity components of the acceleration signal.
    	private float[] components2 = new float[3];

    	private float[] linearAcceleration2 = new float[]
    	{ 0, 0, 0 };

    	private float[] acceleration = new float[]
    	{ 0, 0, 0 };
    	private float[] magnetic = new float[]
    	{ 0, 0, 0 };
    	
    	private boolean firstStartFlag = true;
// ******************************************************************************************** step detect
    	
    	//public float[] calculate(float[] acceleration, float[] magnetic)
    	public void calculate(float[] acceleration, float[] magnetic)
        {
            // Get a local copy of the sensor values
            System.arraycopy(acceleration, 0, this.accel, 0,
                    acceleration.length);
     
            // Get a local copy of the sensor values
            System.arraycopy(magnetic, 0, this.magnet, 0, acceleration.length);
     
            // Get the rotation matrix to put our local device coordinates
            // into the world-coordinate system.
            if (SensorManager.getRotationMatrix(r, null, acceleration, magnetic))
            {
                // values[0]: azimuth/yaw, rotation around the Z axis.
                // values[1]: pitch, rotation around the X axis.
                // values[2]: roll, rotation around the Y axis.
                float[] values = new float[3];
     
                // NOTE: the reference coordinate-system used is different
                // from the world coordinate-system defined for the rotation
                // matrix:
                // X is defined as the vector product Y.Z (It is tangential
                // to the ground at the device's current location and
                // roughly points West). Y is tangential to the ground at
                // the device's current location and points towards the
                // magnetic North Pole. Z points towards the center of the
                // Earth and is perpendicular to the ground.
                SensorManager.getOrientation(r, values);
     
                float magnitude = (float) (Math.sqrt(Math.pow(this.acceleration[0],2)
                        + Math.pow(this.acceleration[1], 2)
                        + Math.pow(this.acceleration[2], 2)) / SensorManager.GRAVITY_EARTH);
     
                double var = varianceAccel.addSample(magnitude);
                 

                
                // Attempt to estimate the gravity components when the device is
                // stable and not experiencing linear acceleration.
                if (var < 0.05)
                {
                    //values[0]: azimuth, rotation around the Z axis.
                    //values[1]: pitch, rotation around the X axis.
                    //values[2]: roll, rotation around the Y axis.
                     
                    // Find the gravity component of the X-axis
                    // = g*-cos(pitch)*sin(roll);
                	components2[0] = (float) (SensorManager.GRAVITY_EARTH * -Math.cos(values[1]) * Math.sin(values[2]));
                	//components2[0] = (float) (-Math.cos(values[1]) * Math.sin(values[2]));
                     
                    // Find the gravity component of the Y-axis
                    // = g*-sin(pitch);
                	components2[1] = (float) (SensorManager.GRAVITY_EARTH * -Math.sin(values[1]));
                	//components2[1] = (float) (-Math.sin(values[1]));
     
                    // Find the gravity component of the Z-axis
                    // = g*cos(pitch)*cos(roll);
                	components2[2] = (float) (SensorManager.GRAVITY_EARTH * Math.cos(values[1]) * Math.cos(values[2]));
                	//components2[2] = (float) (Math.cos(values[1]) * Math.cos(values[2]));

                	
            		newLinAccVectValue2 = (float)Math.sqrt((components[0]*components[0])+(components[1]*components[1])+(components[2]*components[2]));    		
            		
                }
                accVectValue = (float)Math.sqrt((this.acceleration[0]*this.acceleration[0])+(this.acceleration[1]*this.acceleration[1])+(this.acceleration[2]*this.acceleration[2]));	
                //newLinAccVectValue = ((accVectValue - newLinAccVectValue2)-SensorManager.GRAVITY_EARTH)/ SensorManager.GRAVITY_EARTH;
                newLinAccVectValue = ((accVectValue - newLinAccVectValue2)- SensorManager.GRAVITY_EARTH)/ SensorManager.GRAVITY_EARTH;
                
                // Subtract the gravity component of the signal
                // from the input acceleration signal to get the
                // tilt compensated output.
                linearAcceleration2[0] = (this.acceleration[0] - components2[0])/SensorManager.GRAVITY_EARTH;
                linearAcceleration2[1] = (this.acceleration[1] - components2[1])/SensorManager.GRAVITY_EARTH;
                linearAcceleration2[2] = (this.acceleration[2] - components2[2])/SensorManager.GRAVITY_EARTH;
                
               // newLinAccVectValue = (float)Math.sqrt((linearAcceleration2[0]*linearAcceleration2[0])+(linearAcceleration2[1]*linearAcceleration2[1])+(linearAcceleration2[2]*linearAcceleration2[2]));
                exampleSeries.appendData(new GraphViewData(graphIndex, newLinAccVectValue), true, 1024);
            	graphIndex++;
            	
            	long timeDiff = System.currentTimeMillis()-firstStartTime; 
            	Datalist.add(new dataPoints(newLinAccVectValue, timeDiff));
            	
            	saveToFile(linearAcceleration2, newLinAccVectValue, timeDiff);
            	//checkForStep(newLinAccVectValue);
            	if(Datalist.size()>=W)
    			{
    				//ArrayList<ArrayList<Peaks>> tmpPeaks = 
    				findExtremaLocal(Datalist, 0.07f);
    				//checkPeaksForSteps(tmpPeaks);				
    				Datalist.clear();	
    				
    				compassTextBox.setText("steps: " + stepCounter);
    			}
            }

            
            //return linearAcceleration2;
        }
    	
// **************************************************************************************** check step stuff
    	public void resetData(){
    		//allLastMinPeaks.clear();
    		//allLastMaxPeaks.clear();
    		//Datalist.clear();
    		stepCounter=0;
    		//startTime = System.currentTimeMillis();
    		//firstStartTime = startTime;
    		mn = Float.MAX_VALUE; 
    		mx = Float.MIN_VALUE;
    		minp=0;
    		maxp=0;
    		lastIndex=0;
    		lastTimeStep=0;
    	} 

    	class dataPoints { 
    	    public float val;
    	    long timestamp;
    	    public dataPoints(float v, long t){
    	    	this.val=v;
    	    	this.timestamp = t;
    	    }
    	}
    	
    	class Peaks {
    	    public int mxpos;
    	    public float mx;
    	    long timestamp;
    	    public Peaks(int mxpos, float mx, long timestamp){
    	    	this.mxpos=mxpos;
    	    	this.mx=mx;
    	    	this.timestamp = timestamp;
    	    }
    	}

    	long lastTimeStep=0;
    	int lastIndex=0;
    	dataPoints lastPoint;
    	int lookformax = 1;
    	float mn = Float.MAX_VALUE; 
    	float mx = Float.MIN_VALUE;
    	int minp=0;
    	int maxp=0;
    	int W = 100;
    	long lastPeakTime=0;
    	int stepCounter=0;
    	Peaks lastPeak;
    	
    	ArrayList<Peaks> allLastMinPeaks = new ArrayList<Peaks>();
        ArrayList<Peaks> allLastMaxPeaks = new ArrayList<Peaks>();
        ArrayList<dataPoints> Datalist = new ArrayList<dataPoints>();
        
// *********************************************************************************** check step functions       
    	
        public void findExtremaLocal(ArrayList<dataPoints> pointList, float delta){
    		//lastPoint = pointList.get(pointList.size()-1);
    		ArrayList<Peaks> minPeaks = new ArrayList<Peaks>();
    		ArrayList<Peaks> maxPeaks = new ArrayList<Peaks>();	
    		//ArrayList<ArrayList<Peaks>> extremas = new ArrayList<ArrayList<Peaks>>();
    		Peaks currentPeak = new Peaks(0,0,0);
    		
    		
    		int num = pointList.size();
    		//float mn = Float.MAX_VALUE; 
    		//float mx = Float.MIN_VALUE;
    		int mnpos = -1; 
    		int mxpos = -1;
    		
    		//int W = 10;
    		
    		for(int i=0;i<num;i++)
    		{
    			float currentValue = pointList.get(i).val;
    			long time = pointList.get(i).timestamp;
    			
    			if(currentValue > mx)
    			{
    				mx = currentValue;
    				mxpos = i;
    			}
    			if(currentValue < mn)
    			{
    				mn = currentValue; 
    				mnpos = i;
    			}
    			
    			if(lookformax == 1)
    			{
    				if(currentValue<(mx-delta))
    				{
    					//Log.e("GRAPH", "findExtremaLocal: maxPeaks= "+(mxpos+lastIndex)+" , val: "+mx + " , t: " +time );
    					mn = currentValue;
    					mnpos = i;
    					currentPeak = new Peaks(mxpos, mx, time);
    					maxPeaks.add(currentPeak);    				        				   
    				    lookformax = 0;
    				    maxp++;
    				    
    				    //lastPeakTime=System.currentTimeMillis();    				    
    				}
    			}
    			else
    			{
    				if(currentValue>(mn+delta))
    				{
    					//Log.e("GRAPH", "findExtremaLocal: minPeaks= "+(mnpos+lastIndex)+" , val: "+mn + " , t: " +time );
    					mx = currentValue; 
    					mxpos = i;
    					currentPeak = new Peaks(mnpos, mn, time);
    					minPeaks.add(currentPeak);    					    				        				    
    				    lookformax = 1;
    				    minp++;
    				    allLastMinPeaks.add(currentPeak);
    				    //lastPeakTime=System.currentTimeMillis();
    				}
    			}
    			
    			
        		// jezeli wykryje krok, nulluje lastPeak
        		if(lastPeak == null)
        		{
        			lastPeak = currentPeak;
        			Log.e("GRAPH", "no last peak !!!" );
        		}
        		else
        		{
        			if(currentPeak != null)
        			if(currentPeak.timestamp != 0)
        			{
    	    			long dPeaks = currentPeak.timestamp - lastPeak.timestamp;
    	    			Log.e("GRAPH", "----------------------------------------------------");
    	    			Log.e("GRAPH", "lastPeak time: " + lastPeak.timestamp + " , curent: " +  currentPeak.timestamp);
    	    			Log.e("GRAPH", "lastPeak val: " + lastPeak.mx + " , curent: " +  currentPeak.mx);
    	    			Log.e("GRAPH", "----------------------------------------------------");
    	    			
    	    			//if(lastPeak.mx > delta && currentPeak.mx < -delta)
    	    			if(lastPeak.mx > 0 && currentPeak.mx < 0)
    		    		{
    	    				float dV = Math.abs(currentPeak.mx - lastPeak.mx);
    	    				if(dV > delta)
    	    				{
    	    					if(dPeaks < 500 && dPeaks > 150) // mozliwy krok
    	    					{
    	    						// 	sprawdz     					
    	    						stepCounter++;
    	    						lastPeak=null;
    	    	    			}
    	    				}
    	        		}    			
    	    			lastPeak = currentPeak;
    	    			currentPeak = null;
        			}
        		}
    		}

    		
    		
    		Log.e("GRAPH", "findExtremaLocal: stepCounter ->" + stepCounter);
    	}
}
 