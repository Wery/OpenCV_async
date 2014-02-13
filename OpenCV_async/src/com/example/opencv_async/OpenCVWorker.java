/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.example.opencv_async;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * @author Erik Hellman <erik.hellman@sonymobile.com>
 */
public class OpenCVWorker extends Mat implements Runnable {
	public static final String TAG = "OpenCVWorker";

	public static final int FIRST_CAMERA = 0;
	public static final int SECOND_CAMERA = 1;

	public static final int RESULT_MATRIX_BUFFER_SIZE = 3;

	/**
	 * Constant used to calculate FPS value (see measureFps())
	 */
	public static final int FPS_STEPS = 20;

	// The threshold value for the lower and upper color limits
	public static final double THRESHOLD_LOW = 35;
	public static final double THRESHOLD_HIGH = 35;

	/**
	 * Boolean
	 */
	private boolean mDoProcess;
	private int mCameraId = SECOND_CAMERA;
	private Size mPreviewSize;
	private VideoCapture mCamera;
	private Set<ResultCallback> mResultCallbacks = Collections
			.synchronizedSet(new HashSet<ResultCallback>());
	private ConcurrentLinkedQueue<Bitmap> mResultBitmaps = new ConcurrentLinkedQueue<Bitmap>();

	/**
	 * Matrices used to hold the actual image data for each processing step
	 */
	private Mat mCurrentFrame;
	private Mat mGrayFrame;
	private Mat mThresholdFrame;
	private Mat mCurrentFrameHsv;
	private Mat mBluredFrame;
	private Mat mCannyFrame;

	private int mFpsCounter;
	private double mFpsFrequency;
	private long mPrevFrameTime;
	private double mPreviousFps;

	private Point mSelectedPoint = null;

	private Scalar mLowerColorLimit;
	private Scalar mUpperColorLimit;

	// ***********************************************************************
	// ZMIENNE

	private int zmienna;
	private int zmienna2;

	public static final int CANNY = 3;
	public static final int THRESHOLD_MANUAL = 2;
	public static final int THRESHOLD_GLOBAL = 1;
	public static final int THRESHOLD_ADAPTIVE = 0;
	
	public static final int FRAME_BW = 0;
	public static final int FRAME_RGB = 1;
	
	private int thresholdType;
	private int manualThresholdValue;
	private int adaptiveThresholdValue_C;
	private int adaptiveThresholdBlokSize;
	private int bwRGB;

	// ********************************

	private final static double MIN_DISTANCE = 10;

	public int getThresholdType() {
		return this.thresholdType;
	}

	public void setThresholdType(int x) {
		if (x != 0 && x != 1 && x != 2 && x != 3)
			this.thresholdType = 0;
		else
			this.thresholdType = x;
	}

	public void setmanualThresholdValue(int x) {
		this.manualThresholdValue = x;
	}

	public int getmanualThresholdValue() {
		return this.manualThresholdValue;
	}

	public void setadaptiveThresholdValue_C(int x) {
		this.adaptiveThresholdValue_C = x;
	}

	public int getAdaptiveThresholdValue_C() {
		return this.adaptiveThresholdValue_C;
	}

	public void setadaptiveThresholdBlokSize(int x) {
		this.adaptiveThresholdBlokSize = x;
	}

	public int getAdaptiveThresholdBlokSize() {
		return this.adaptiveThresholdBlokSize;
	}

	public void setbwRGB(int x) {
		this.bwRGB = x;
	}

	public int getbwRGB() {
		return this.bwRGB;
	}
	
	public int getZmienna1() {
		return this.zmienna;
	}

	public void setZmienna1(int x) {
		this.zmienna = x;
	}

	public int getZmienna2() {
		return this.zmienna2;
	}

	public void setZmienna2(int x) {
		this.zmienna2 = x;
	}

	// ********************************

	public OpenCVWorker(int cameraId) {
		mCameraId = cameraId;
		// Default preview size
		mPreviewSize = new Size(540, 960);

		thresholdType = 0;
		manualThresholdValue = 100;
		adaptiveThresholdValue_C = 10;
		adaptiveThresholdBlokSize = 75;
		bwRGB = FRAME_RGB;
	}

	public void releaseResultBitmap(Bitmap bitmap) {
		mResultBitmaps.offer(bitmap);
	}

	public void addResultCallback(ResultCallback resultCallback) {
		mResultCallbacks.add(resultCallback);
	}

	public void removeResultCallback(ResultCallback resultCallback) {
		mResultCallbacks.remove(resultCallback);
	}

	public void stopProcessing() {
		mDoProcess = false;
	}

	// Setup the camera
	private void setupCamera() {
		if (mCamera != null) {
			VideoCapture camera = mCamera;
			mCamera = null; // Make it null before releasing...
			camera.release();
		}

		mCamera = new VideoCapture(mCameraId);

		// Figure out the most appropriate preview size that this camera
		// supports.
		// We always need to do this as each device support different preview
		// sizes for their cameras
		List<Size> previewSizes = mCamera.getSupportedPreviewSizes();
		double largestPreviewSize = 1280 * 720; // We should be smaller than
												// this...
		double smallestWidth = 320; // Let's not get a smaller width than
									// this...
		for (Size previewSize : previewSizes) {
			
			Log.e("CAM_SIZE","w: " + previewSize.width + " ,h: "+previewSize.height);
			if (previewSize.area() < largestPreviewSize
					&& previewSize.width >= smallestWidth) {
				mPreviewSize = previewSize;
			}
		}

		Log.e("CAM_SIZE","CHOSEN: w: " + mPreviewSize.width + " ,h: "+mPreviewSize.height);
		mPreviewSize = new Size(640,368);
		
		mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mPreviewSize.width);
		mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mPreviewSize.height);

		mCamera.set(Highgui.CV_CAP_PROP_ANDROID_FOCUS_MODE,
				Highgui.CV_CAP_ANDROID_FOCUS_MODE_INFINITY);
	}

	/**
	 * Initialize the matrices and the bitmaps we will use to draw the result
	 */
	private void initMatrices() {
		mCurrentFrame = new Mat();
		mCurrentFrameHsv = new Mat();
		mThresholdFrame = new Mat();
		mGrayFrame = new Mat();
		mCannyFrame = new Mat();
		mBluredFrame = new Mat();

		// Since drawing to screen occurs on a different thread than the
		// processing,
		// we use a queue to handle the bitmaps we will draw to screen
		mResultBitmaps.clear();
		for (int i = 0; i < RESULT_MATRIX_BUFFER_SIZE; i++) {
			Bitmap resultBitmap = Bitmap.createBitmap((int) mPreviewSize.width,
					(int) mPreviewSize.height, Bitmap.Config.ARGB_8888);
			mResultBitmaps.offer(resultBitmap);
		}
	}

	/**
	 * The thread used to grab and process frames
	 */
	@Override
	public void run() {
		mDoProcess = true;
		Rect previewRect = new Rect(0, 0, (int) mPreviewSize.width,
				(int) mPreviewSize.height);
		double fps;
		mFpsFrequency = Core.getTickFrequency();
		mPrevFrameTime = Core.getTickCount();
		Mat hierarchy;

		setupCamera();

		initMatrices();
/*
		while (mDoProcess && mCamera != null) {
			boolean grabbed = mCamera.grab();
			if (grabbed) {
				// Retrieve the next frame from the camera in RGB format
				mCamera.retrieve(mCurrentFrame,
						Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);

				// Convert the RGB frame to HSV as it is a more appropriate
				// format when calling Core.inRange
				Imgproc.cvtColor(mCurrentFrame, mGrayFrame,
						Imgproc.COLOR_RGB2GRAY);

				// Imgproc.blur(mCurrentFrame, mCurrentFrame, new Size(5));

				if (thresholdType == THRESHOLD_ADAPTIVE) {
					Imgproc.adaptiveThreshold(mGrayFrame, mThresholdFrame, 127,
							Imgproc.ADAPTIVE_THRESH_MEAN_C,
							Imgproc.THRESH_BINARY, adaptiveThresholdBlokSize,
							adaptiveThresholdValue_C);
				//	Log.e("THRESHOLD", "adaptiveThreshold: "
				//			+ adaptiveThresholdBlokSize + " , "
				//			+ adaptiveThresholdValue_C);
				} else if (thresholdType == THRESHOLD_GLOBAL) {
					Imgproc.threshold(mGrayFrame, mThresholdFrame, 100, 127,
							Imgproc.THRESH_OTSU);
				//	Log.e("THRESHOLD", "threshold global.");
				} else  if (thresholdType == THRESHOLD_MANUAL)
				{
					Imgproc.threshold(mGrayFrame, mThresholdFrame,
							manualThresholdValue, 127, Imgproc.THRESH_BINARY);
				//	Log.e("THRESHOLD", "threshold manual: "
				//			+ manualThresholdValue);
				} else if (thresholdType == CANNY)
				{
					Size size = new Size(3, 3);
					Imgproc.blur(mGrayFrame, mBluredFrame, size);
					Imgproc.Canny(mBluredFrame, mThresholdFrame, 5.0, 10.0);
				//	Log.e("THRESHOLD", "CANNY edge detector");
				}

				// hold the list contour
				List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

				hierarchy = new Mat();

				Mat mContoursFrame = mThresholdFrame.clone();
				// finding all contours in the image
				Imgproc.findContours(mContoursFrame, contours, hierarchy,
						Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE,
						new Point(0, 0));

				for (int i = 0; i < contours.size(); i++) // Look at all the
															// contours
				{
					MatOfPoint contour = contours.get(i);

					int contourSize = (int) contour.total();

					if (contourSize > 100 && contourSize < 1200) {
						MatOfPoint2f approxCurve = new MatOfPoint2f();
						Imgproc.approxPolyDP(
								new MatOfPoint2f(contour.toArray()),
								approxCurve, contourSize * 0.05, true);

						if (approxCurve.total() == 4) {
							if (Imgproc.isContourConvex(new MatOfPoint(
									approxCurve.toArray()))) {
								Point[] corners = approxCurve.toArray();
								List<Point> cornerList = approxCurve.toList();
								
				

								double maxCosine = 0;

								for (int j = 2; j < 5; j++) {
									double cosine = Math.abs(angle(cornerList.get(j % 4), cornerList.get(j - 2), cornerList.get(j -1)));
									maxCosine = Math.max(maxCosine, cosine);
								}


								// cos:     0.5 - 0
								// stopnie: 60  - 90
								
								if (maxCosine < 0.5) {
									
									
									// SORTING CORNERS
									
									//Point center = centerPoint(cornerList);
									Point center = new Point(0,0);
									
									for (int j = 0; j < corners.length; j++)
									{
										center.x += cornerList.get(j).x;										
										center.y += cornerList.get(j).y;
									}
									
									center.x *= (1. / cornerList.size());
									center.y *= (1. / cornerList.size());

									cornerList = sortCorners(cornerList, center);
									
									
									
									// APPLY PERSPECTIVE transformation
									int markerSize = 12;
									int cellSize = 15;
									int size = cellSize * markerSize;
									
									// Log.e("!!!_size__!!!","size= " + size);
									
									Mat quad = new Mat(size,size,CvType.CV_8UC1);

								    Mat startM = Converters.vector_Point2f_to_Mat(cornerList);

								    List<Point> dst_pnt = new ArrayList<Point>();
								    Point p4 = new Point(0, 0);								    
								    Point p5 = new Point(size, 0);								    
								    Point p6 = new Point(size, size);								    
								    Point p7 = new Point(0, size);
								    
								    dst_pnt.add(p4);
								    dst_pnt.add(p5);
								    dst_pnt.add(p7);								    
								    dst_pnt.add(p6);
				
								    Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);
								    
									Mat transmtx = Imgproc.getPerspectiveTransform(startM, endM);									
									Imgproc.warpPerspective(mThresholdFrame, quad, transmtx, quad.size());
									
									// quad size = 180
									
									// left black (2 x 15)
									Mat cell = quad.submat(3, 177, 3, 27); 
						            int nonZero = Core.countNonZero(cell);
						          //  Log.e("!!!_size__!!!","nonZero= " + nonZero);
						            						            	
						            // left white (1 x 15)
						            Mat cell2 = quad.submat(33, 147, 33, 42);
						            int nonZero2 = Core.countNonZero(cell2);						            
						          //  Log.e("!!!_size__!!!","nonZero2= " + nonZero2);
						            
						            // right white (1 x 15)
						            Mat cell3 = quad.submat(33, 147, 138, 147);
						            int nonZero3 = Core.countNonZero(cell3);
						           // Log.e("!!!_size__!!!","nonZero3= " + nonZero3);
						            
						            // right black (2x15)
						            Mat cell4 = quad.submat(3, 177, 153, 177);
						            int nonZero4 = Core.countNonZero(cell4);
						          //  Log.e("!!!_size__!!!","nonZero4= " + nonZero4);
						            						     
									
						            if(nonZero > 800)
						            	continue;
						            if(nonZero2 < 800)
						            	continue;
						            if(nonZero3 < 800)
						            	continue;
						            if(nonZero4 > 800)
						            	continue;
						            
						           
							           
						         // ROZPOZNAC ID
						            int id = (int)rozpoznajID(quad);
						            // Log.e("!!!_ok_???_!!!","ID= " + id);
									if(id == -1)																		
									{
										//Log.e("!!!_ok__!!!","Marker not recognise 1 ! (rotate !)"); 
										Point centerQuad = new Point((quad.size().width/2),(quad.size().height/2));
										Mat rotImage = Imgproc.getRotationMatrix2D(centerQuad, 90, 1.0);										
										Imgproc.warpAffine(quad, quad, rotImage, quad.size());
										id = (int)rozpoznajID(quad);
										if(id == -1)	
										{
											//Log.e("!!!_ok__!!!","Marker not recognise 2 ! (rotate !)");
											Imgproc.warpAffine(quad, quad, rotImage, quad.size());
											id = (int)rozpoznajID(quad);
											if(id == -1)	
											{
												//Log.e("!!!_ok__!!!","Marker not recognise 3 ! (rotate !)"); 	
												Imgproc.warpAffine(quad, quad, rotImage, quad.size());
												id = (int)rozpoznajID(quad);
												if(id == -1)	
												{
													continue;
												}	
											}
										}										
									}
									 // Log.e("!!!_ok__!!!","IT IS OK !!!!");
									 
									 if(bwRGB == FRAME_RGB)
									 {									
							            Core.line(mCurrentFrame, cornerList.get(0),
												cornerList.get(1), new Scalar(255, 0, 0),
												2);
										Core.line(mCurrentFrame, cornerList.get(1),
												cornerList.get(3), new Scalar(255, 0, 0),
												2);
										Core.line(mCurrentFrame, cornerList.get(3),
												cornerList.get(2), new Scalar(255, 0, 0),
												2);
										Core.line(mCurrentFrame, cornerList.get(2),
												cornerList.get(0), new Scalar(255, 0, 0),
												2); 											
										
										String text = id+"";
										Scalar c = new Scalar(255, 0, 0, 255);   
										Point point = new Point(cornerList.get(0).x + 20 , cornerList.get(0).y + 20);
										Core.putText(mCurrentFrame, text, point, Core.FONT_HERSHEY_COMPLEX, 0.8, c );
									 }
									 else if(bwRGB == FRAME_BW)
									 {
										 Core.line(mThresholdFrame, cornerList.get(0),
													cornerList.get(1), new Scalar(255, 0, 0),
													2);
											Core.line(mThresholdFrame, cornerList.get(1),
													cornerList.get(3), new Scalar(255, 0, 0),
													2);
											Core.line(mThresholdFrame, cornerList.get(3),
													cornerList.get(2), new Scalar(255, 0, 0),
													2);
											Core.line(mThresholdFrame, cornerList.get(2),
													cornerList.get(0), new Scalar(255, 0, 0),
													2); 											
											
											String text = id+"";
											Scalar c = new Scalar(255, 0, 0, 255);   
											Point point = new Point(cornerList.get(0).x + 20 , cornerList.get(0).y + 20);
											Core.putText(mThresholdFrame, text, point, Core.FONT_HERSHEY_COMPLEX, 0.8, c );	 
									 }								
								}
							}
						}
					}
				}

				hierarchy.release();
				// Imgproc.drawContours(mThresholdFrame, contours, -1, new
				// Scalar(Math.random()*255, Math.random()*255,
				// Math.random()*255));

				if(bwRGB == FRAME_BW)
					notifyResultCallback(mThresholdFrame);			
				else if(bwRGB == FRAME_RGB)
					notifyResultCallback(mCurrentFrame);			
			 
				// Log.e("!!@!@!@!@!", "print");
				fps = measureFps();
				notifyFpsResult(fps);
			}
		}
*/
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	int rozpoznajID(Mat m)
	{
		//Log.e("!___CRC___","rozpoznajID..");
		 
	    int IDsize = 6;
        int cellSize = 15;
        
        Rect roi = new Rect(cellSize*3, cellSize*3, cellSize*IDsize, cellSize*IDsize);
        Mat cropped = new Mat(m, roi);
        cropped.convertTo(cropped, CvType.CV_8UC1); 

        int idTmp = 0;
		
		int IDCounter = 35;
		short CRC = 0;
		
		for (int row = 0; row < IDsize; row++)
        {
            for (int col = 0; col < IDsize; col++ )
            {			
				int cellX = (row*cellSize);
				int cellY = (col*cellSize);
				
				Mat cell = cropped.submat(cellX+3, cellX +(int)cellSize - 3, cellY + 3, cellY + (int)cellSize - 3); 
				int nonZero = Core.countNonZero(cell);

				if(nonZero > ((cellSize-3)*(cellSize-3))/2)
				{   
					if(IDCounter>15)
					{ 
						idTmp |= (int)(1L<<(IDCounter-16));
					}
					else
					{
						CRC |= (int)(1L<<IDCounter); 
					}
				}
				IDCounter--;
            }
        }
		

        //Log.e("!___CRC___","hex idTmp = " + Integer.toHexString(idTmp));
  	
		byte[] crcBytes = ByteBuffer.allocate(4).putInt(idTmp).array();
		int calculatedCRC2 = crc16(crcBytes);
		//System.out.printf("HEX id: %02X\n", idTmp);
	    //System.out.printf("calcualted CRC: %02X\n", calculatedCRC2);
	    //System.out.printf("readed CRC: %02X\n", CRC);
	    if(CRC == calculatedCRC2)
	    {
	    	//Log.e("!___CRC___","CRC match! Marker ID = "+idTmp);
	    	return idTmp;
	    }
		return (-1);
	}
	

	// *********************************************************************************************** CRC
	static int crc16(final byte[] buffer) {
		//CRC-CCITT (0xFFFF)
		
	    int crc = 0xFFFF;

	    for (int j = 0; j < buffer.length ; j++) {
	        crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
	        crc ^= (buffer[j] & 0xff);//byte to int, trunc sign
	        crc ^= ((crc & 0xff) >> 4);
	        crc ^= (crc << 12) & 0xffff;
	        crc ^= ((crc & 0xFF) << 5) & 0xffff;
	    }
	    crc &= 0xffff;
	    return crc;

	}
	// ***************************************************************************************************	
	
	
	List<Point> sortCorners(List<Point> corners, Point center)
	{
		//Log.e("SORTING","corners count= " + corners.size());
	    List<Point> cornersTmp = new ArrayList<Point>();
	    Vector<Point> top = new Vector<Point>();
	    Vector<Point> bot = new Vector<Point>();

	    for (int i = 0; i < corners.size(); i++)
	    {
	        if (corners.get(i).y >= center.y && bot.size() < 2 )
	            bot.add(corners.get(i));
	        else if (top.size() < 2 )
	            top.add(corners.get(i));
	        else
	        	bot.add(corners.get(i));
	    }

	  //  Log.e("SORTING","top: "+top.size()+" ,bot= "+bot.size());
	    
	    Point tl = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
	    Point tr = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
	    Point bl = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
	    Point br = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);

	    cornersTmp.clear();
	    cornersTmp.add(tl);
	    cornersTmp.add(tr);
	    cornersTmp.add(bl);
	    cornersTmp.add(br);

	    return cornersTmp;
	}

	Point centerPoint(List<Point> corners)
	{
		Point centerPoint = new Point(0,0);
		
		int xMin = Integer.MAX_VALUE; 
	    int xMax = Integer.MIN_VALUE;
	    int yMin = Integer.MAX_VALUE;
	    int yMax = Integer.MIN_VALUE;
		
	    for(int i = 0; i < 4; i++) 
	    {
	        if(corners.get(i).x > xMax) xMax = (int) corners.get(i).x;
	        if(corners.get(i).x < xMin) xMin = (int) corners.get(i).x;
	        if(corners.get(i).y > xMax) yMax = (int) corners.get(i).y;
	        if(corners.get(i).y < xMax) yMax = (int) corners.get(i).y;
	    }
	    centerPoint.x = xMax - xMin / 2;
	    centerPoint.y = yMax - yMin / 2;
	    
		return centerPoint;		
	}
	
	
	int unsignedByteToInt(byte b) {
	    return (int) b & 0xFF;
	    }

	boolean IsPointOnLine(Point linePointA, Point linePointB, Point point) 
	{
	   double EPSILON = 0.001;
	   double a = (linePointB.y - linePointA.y) / (linePointB.x - linePointB.x);
	   double b = linePointA.y - a * linePointA.x;
	   if ( Math.abs(point.y - (a*point.x+b)) < EPSILON)
	   {
	       return true;
	   }

	   return false;
	}
	
	boolean collinear(Point a, Point b, Point c){		
		return collinear(a.x, a.y, b.x, b.y, c.x, c.y); 
	}
	
	boolean collinear(double x1, double y1, double x2, double y2, double x3, double y3) 
	{
		//return (y1 - y2) * (x1 - x3) == (y1 - y3) * (x1 - x2);
		
		return ((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)) == 0;
	}
	
	
	double angle(Point pt1, Point pt2, Point pt0) {
		double dx1 = pt1.x - pt0.x;
		double dy1 = pt1.y - pt0.y;
		double dx2 = pt2.x - pt0.x;
		double dy2 = pt2.y - pt0.y;
		return (dx1 * dx2 + dy1 * dy2)
				/ Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
						+ 1e-10);
	}

	public double measureFps() {
		mFpsCounter++;
		if (mFpsCounter % FPS_STEPS == 0) {
			long time = Core.getTickCount();
			double fps = FPS_STEPS * mFpsFrequency / (time - mPrevFrameTime);
			mPrevFrameTime = time;
			mPreviousFps = fps;
		}
		return mPreviousFps;
	}

	private void notifyFpsResult(double fps) {
		for (ResultCallback resultCallback : mResultCallbacks) {
			resultCallback.onFpsUpdate(fps);
		}
	}

	private void notifyResultCallback(Mat result) {
		Bitmap resultBitmap = mResultBitmaps.poll();
		if (resultBitmap != null) {
			Utils.matToBitmap(result, resultBitmap, true);
			for (ResultCallback resultCallback : mResultCallbacks) {
				resultCallback.onResultMatrixReady(resultBitmap);
			}
		}
	}

	public void setSelectedPoint(double x, double y) {
		mLowerColorLimit = null;
		mUpperColorLimit = null;
		mSelectedPoint = new Point(x, y);
	}

	public void clearSelectedColor() {
		mLowerColorLimit = null;
		mUpperColorLimit = null;
		mSelectedPoint = null;
	}

	public Size getPreviewSize() {
		return mPreviewSize;
	}

	public interface ResultCallback {
		void onResultMatrixReady(Bitmap mat);

		void onFpsUpdate(double fps);
	}
}
