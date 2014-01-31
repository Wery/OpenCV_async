package com.example.opencv_async;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
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
        float azimuth2 = 0f;
        private float currectAzimuth = 0;


        
        // compass arrow to rotate
        public ImageView arrowView = null;
        public TextView compassTextBox = null;
        public TextView testTextBox = null;
        public TextView testTextBox2 = null;

        DatabaseHelper db;
        
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
        
        // ********************************************************************************************
        public Compass(Context context,ImageView iv, TextView tv,GraphView gv, GraphViewSeries series) {
        		
        	db = new DatabaseHelper(context);
        	this.gv = gv;
        	this.exampleSeries = series;
        	
        	arrowView = iv;
        	compassTextBox = tv;
        	compassTextBox.setText("compas azimuth");
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
            

        }

        public void start() {
        		sensorManager.registerListener(this, accSensor,
        						SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gsensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, msensor,
                                SensorManager.SENSOR_DELAY_FASTEST);
                
        	
        	// 1s = 1000000 us
        	// 100Hz = 10000 us
                /*
        		sensorManager.registerListener(this, accSensor,10000);
                sensorManager.registerListener(this, gsensor, 10000);
                sensorManager.registerListener(this, msensor, 10000);
                */
              //  testTextBox.setText("COMPASS STARTED!");
        }

        public void stop() {
        	db.closeDB();    
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
                currectAzimuth = azimuth;
          
                an.setDuration(5);
                an.setRepeatCount(0);
                an.setFillAfter(true);

                arrowView.startAnimation(an);
        }

        public static final float EPSILON = 0.000000001f;
        
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
        
        private static final float NS2S = 1.0f / 1000000000.0f;
        private float timestamp;
        private boolean initState = true;
         
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
        
        final float lowPassFilterAlpha = 0.77f;
    	float ax;
    	float ay;
    	float az;
    	
    	class calculateFusedOrientationTask extends TimerTask {
            public void run() {
            	               
            	float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
                fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                    + oneMinusCoeff * accMagOrientation[0];
         
                fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                    + oneMinusCoeff * accMagOrientation[1];
         
                fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                    + oneMinusCoeff * accMagOrientation[2];
         
                // overwrite gyro matrix and orientation with fused orientation
                // to comensate gyro drift
                gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
                System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
                azimuth2 = (float) Math.toDegrees(fusedOrientation[0]); // orientation
                azimuth2 = (azimuth2 + 360) % 360;
                
            	//**************************************************************************************

                ax = ax + lowPassFilterAlpha  * (accel[0] - ax);
                ay = ay + lowPassFilterAlpha  * (accel[1] - ay);
                az = az + lowPassFilterAlpha  * (accel[2] - az);
                
                accVectValue = (float)Math.sqrt((ax*ax)+(ay*ay)+(az*az));
               
                if(startFlag)
                {                	
	                if (isFirstSet) {
	                    startTime = System.currentTimeMillis();
	                    isFirstSet = false;
	                 
	    	            try {
	    	            	myFile = new File("/sdcard/test/test-"+startTime+".txt");
	    	            	myFile.createNewFile();
	    	            	
	    	            	
	    	            	
	    	            	//fOut = new FileOutputStream(myFile,true);
	    	            	//myOutWriter = new OutputStreamWriter(fOut);
	    	            	
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
	                    	testTextBox2.setText("sample: "+index);
	                    	adjustArrow();
	                    	compassTextBox.setText("filter: "+azimuth2);
	                    	//exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
	                        graphIndex++; 
	                    }
	           });
            	
            	//**************************************************************************************
            }
        }
        
        public void calculateAccMagOrientation() {
            if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
                SensorManager.getOrientation(rotationMatrix, accMagOrientation);
            }
        }
        
        float accVectValue = 0f;
        float accVectValue2 = 0f;
        float accVectValue3 = 0f;
        
        GraphViewData[] graphData = new GraphViewData[1024];
        private int graphIndex=0;
        private int index=0;
        long lastTime=0;
        
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
            
/*            
                final float alpha = 0.97f;
               // compassTextBox.setText("");
                synchronized (this) {
                        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                                                * event.values[0];
                                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                                                * event.values[1];
                                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                                                * event.values[2];

                                accVectValue3 = (float)Math.sqrt((mGravity[0]*mGravity[0])+(mGravity[1]*mGravity[1])+(mGravity[2]*mGravity[2]));
                                //fftData[fftIndex] = accVectValue;
                                accVectValue = (float)Math.sqrt((event.values[0]*event.values[0])+(event.values[1]*event.values[1])+(event.values[2]*event.values[2]));
                                
                                accVectValue2 = alpha * accVectValue2 + (1 - alpha) * accVectValue;
                                // mGravity = event.values;

                                // Log.e(TAG, Float.toString(mGravity[0]));
                        }

                        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                                // mGeomagnetic = event.values;

                                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                                                * event.values[0];
                                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                                                * event.values[1];
                                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                                                * event.values[2];
                                // Log.e(TAG, Float.toString(event.values[0]));

                        }
                        
                        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                        	System.arraycopy(event.values, 0, mGyro, 0, 3);
                        }
                        	

                        float R[] = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                                        mGeomagnetic);
                        if (success) {
                                float orientation[] = new float[3];
                                SensorManager.getOrientation(R, orientation);
                                // Log.d(TAG, "azimuth (rad): " + azimuth);
                                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                                //compassTextBox.append("a1: "+azimuth);
                                azimuth = (azimuth + 360) % 360;
                                testTextBox2.setText("normal: "+azimuth);
                                // Log.d(TAG, "azimuth (deg): " + azimuth);
                                adjustArrow();
                        }
                }*/
/*                
                if (isFirstSet) {
                    startTime = System.currentTimeMillis();
                    isFirstSet = false;
                }
           
                currentTime = System.currentTimeMillis();
                
                if(startFlag)
                {
	                if (isFirstSet) {
	                    startTime = System.currentTimeMillis();
	                    isFirstSet = false;
	                }
	                currentTime = System.currentTimeMillis();
	                if(index < 2)
	                {
	                	index++;
	                }
	                else
	                {	      
	                	
	                	index=0;
	                	if (!stopFlag) {
	                        save();                      
	                    }
	                    else {
	                        try {
	                            myOutWriter.close();
	                        } catch (IOException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
	                        } catch (NullPointerException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
	                        }
	                        try {
	                            fOut.close();
	                        } catch (IOException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
	                        } catch (NullPointerException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
	                        }
	                    }	                	
                	}
                }                             
                
                exampleSeries.appendData(new GraphViewData(graphIndex, accVectValue), true, 1024);
                graphIndex++;
                fftIndex++;
*/
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        
        
        //************************************************************************************************ DATABASE
}
 