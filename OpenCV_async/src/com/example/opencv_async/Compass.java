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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

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
        
        public static final int TIME_CONSTANT = 30;
        public static final float FILTER_COEFFICIENT = 0.98f;
        private Timer fuseTimer = new Timer();
        
        // angular speeds from gyro
        private float[] gyro = new float[3];
     
        // rotation matrix from gyro data
        private float[] gyroMatrix = new float[9];
     
        // orientation angles from gyro matrix
        private float[] gyroOrientation = new float[3];
     
        // magnetic field vector
        private float[] magnet = new float[3];
     
        // accelerometer vector
        private float[] accel = new float[3];
     
        // orientation angles from accel and magnet
        private float[] accMagOrientation = new float[3];
     
        // final orientation angles from sensor fusion
        private float[] fusedOrientation = new float[3];
     
        // accelerometer and magnetometer based rotation matrix
        private float[] rotationMatrix = new float[9];
        
        private float[] mGravity = new float[3];        
        private float[] mGeomagnetic = new float[3];
        private float[] mGyro = new float[3];  
        
        private float azimuth = 0f;
        private float currectAzimuth = 0;


        
        // compass arrow to rotate
        public ImageView arrowView = null;
        public TextView compassTextBox = null;
        public TextView testTextBox = null;
        public TextView testTextBox2 = null;

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
            
            swSize = mIndoorTrackSettings.getSensitivity();
    		stepCount = 0;
        }

        public void start() {
        		sensorManager.registerListener(this, accSensor,
        						SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gsensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, msensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
        
            	compassTextBox.setText("Compass started. Steps: "+stepCount);  	
        }

        public void stop() {
        	sensorManager.unregisterListener(this);                
        }

        private void adjustArrow() {
                if (arrowView == null) {
                        Log.i(TAG, "arrow view is not set");
                        return;
                }
         
                Animation an = new RotateAnimation(-currectAzimuth, -azimuth,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f);
                        
               // compassTextBox.setText("Image az: "+azimuth);
                currectAzimuth = azimuth;
                        
                
                an.setDuration(50);
                an.setRepeatCount(0);
                an.setFillAfter(true);
                arrowView.startAnimation(an);                                            
        }

        public static final float EPSILON = 0.000000001f;
        
        private static final float NS2S = 1.0f / 1000000000.0f;
        private float timestamp;
        private boolean initState = true;
         
       
        final float lowPassFilterAlpha = 0.77f;
    	float ax;
    	float ay;
    	float az;
        float accVectValue = 0f;
        float accVectValue2 = 0f;
        float accVectValue3 = 0f;
        
        GraphViewData[] graphData = new GraphViewData[1024];
        private int graphIndex=0;
        private int index=0;
        long lastTime=0;
        
       	class calculateFusedOrientationTask extends TimerTask {
            public void run() {

                float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
                
                /*
                 * Fix for 179° <--> -179° transition problem:
                 * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
                 * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
                 * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
                 */
                
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

            	
                azimuth = (float) Math.toDegrees(fusedOrientation[0]); // orientation
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
        		System.arraycopy(fusedOrientation, 0, tempGyroOrientation, 0, 3);
        		gyroOrientationList.add(tempGyroOrientation);
        		
        		
        		if(gyroOrientationList.size() > swSize) {
  
       				checkForStep(gyroOrientationList);
        		
        			for(int i = 0; i < swSize - 35; i++) {
        				accelList.remove(0);
        				gyroOrientationList.remove(0);
        			}
        		}
        		
        		
                //*************************************************************************************
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
                        			accel[0] + "," + accel[1] + "," + accel[2] + "," + 
                            		azimuth + "," + accVectValue + "\r\n");                            
                            myBufferedWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}                      
                    }
                    else {
                    	index=0;                    	

                    }	                	
            	}                                                        
                          	
            	mHandler.post(new Runnable() {
	                    public void run() {
	                    	exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
	                    	//testTextBox2.setText("sample: "+index);
	                    	adjustArrow();
	                    	testTextBox.setText("filter: "+d.format(azimuth));
	                       // graphIndex++; 
	                    }
	           });
            	
            	//**************************************************************************************
            }
        }
        
    	@Override
        public void onSensorChanged(SensorEvent event) {

        	switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
         
            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
            	//System.arraycopy(event.values, 0, gyro, 0, 3);
                gyroFunction(event);
                break;
         
            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        
        
        //************************************************************************************************ 
     // calculates orientation angles from accelerometer and magnetometer output
    	public void calculateAccMagOrientation() {
    	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
    	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
    	    }
    	}
    	
    	// This function is borrowed from the Android reference
    	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    	// It calculates a rotation vector from the gyroscope angular speed values.
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
    	
        // This function performs the integration of the gyroscope data.
        // It writes the gyroscope based orientation into gyroOrientation.
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

//******************************************************************************************** step detect
        SharedValue mIndoorTrackSettings;
        
    	private static final int BLOCKSIZE = 8; // Threshold continuous or continuous 0 1
    	private int stepCount; // Detecting the number of footsteps
    	private double stepLength; //step
    	private boolean STEPDETECTED = false; // Footstep detection flag
    	private double priOrientation = 0f; // Previous step in the direction
    	

    	private final static boolean FIXED_STEP_LENGTH = true;
    	private final static int HAND_HELD = 1;
    	private final static int TROUSER_POCKET = 2;
    	
    	private int swSize; // Sliding window size
    	private static final int W = 15; // Local window size, used to calculate the local mean and the variance of the acceleration
    	
    	private List<float[]> accelList = new ArrayList<float[]>(); // Acceleration list
    	private List<float[]> orientationList = new ArrayList<float[]>(); // Magnetometer direction list
    	private List<float[]> gyroOrientationList = new ArrayList<float[]>(); // Gyro direction list
    	
        private void checkForStep(List<float[]> orientationList) {
   		
    		List<Float> magnitudeOfAccel = StepDetectionUtil.getMagnitudeOfAccel(accelList);
    		List<Float> localMeanAccel = StepDetectionUtil.getLocalMeanAccel(magnitudeOfAccel, W);
    		float threshold = StepDetectionUtil.getAverageLocalMeanAccel(localMeanAccel) + 0.5f;
    		List<Integer> condition = StepDetectionUtil.getCondition(localMeanAccel, threshold);
    		
    		int numOne = 0; // Records to determine the condition condition, the number of consecutive 1
    		int numZero = 0; // Records to determine the condition condition, the number of consecutive 0
    		boolean flag = false; // Record the current point is 1 or 0
    		
    		for(int i = 0, j = 1; i < swSize - 1 && j < swSize - W; i++, j++) {
    			flag = StepDetectionUtil.isOne(condition.get(i)); // Analyzing the condition before sample point i determine whether a
    			if((condition.get(i) == condition.get(j)) && flag == true) 
    			{				
    				numOne++;
    			}

    			if((condition.get(i) == condition.get(j)) && flag == false) 
    			{
    				numZero++;	
    			}

    			if((condition.get(i) != condition.get(j)) && j > W && j < swSize - W) {
    				if(numOne > BLOCKSIZE && numZero > BLOCKSIZE) {
    					STEPDETECTED = true;
    					stepCount++;
    					float meanA = StepDetectionUtil.getMean(localMeanAccel, j, W);
    					
    					if(!mIndoorTrackSettings.getStepLengthMode() == FIXED_STEP_LENGTH)
    						{
    						stepLength = StepDetectionUtil.getSL(0.33f, meanA);
    						}
    					else stepLength = mIndoorTrackSettings.getStepLength() / 100f;
    					
    					double meanOrientation = 0;
    					meanOrientation = StepDetectionUtil.getMeanOrientation(numOne, numZero, j, 
    							orientationList, mIndoorTrackSettings.getPhonePosition(), mIndoorTrackSettings.getAlgorithms());
    					
    					priOrientation = meanOrientation;

    	      			mHandler.post(new Runnable() {
    	                    public void run() {
    	                    	compassTextBox.setText("in F steps: "+stepCount);
    	                    }
         	           });
    					  					
    					
    					numOne = 0;
    					numZero = 0;
    				}
    			}
    		}
    		
    	}

}
 