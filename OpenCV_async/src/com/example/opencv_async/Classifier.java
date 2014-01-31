package com.example.opencv_async;

import java.util.LinkedList;

public class Classifier {
     private static final int BUFFER_SIZE = 15;
     private static final int SAMPLING_MAXIMUM = 15;
     private static final double MINIMUM_THREASHOLD = 130.0;
     private static final double ALPHA = 0.97;
     private static final double BETA = 0.998;
     
     private double adaptiveThreashold = 500.0;
     private long sample_counter = 0;
     
     private double last_X, last_Y, last_Z;
     
     private LinkedList<Double> motionBuffer = new LinkedList<Double>();
     
     public boolean isStep(double x, double y, double z) {
                 if( motionBuffer.size() >= BUFFER_SIZE ) motionBuffer.removeFirst();
                                 
                 //how much axis changed since last sample
                 double dx = x - last_X;
                 double dy = y - last_Y;
                 double dz = z - last_Z;
                 
                 //update last values
                 last_X = x;
                 last_Y = y;
                 last_Z = z;
                 
                 motionBuffer.addLast(dx*dx + dy*dy + dz*dz);
                 
                 double D = 0.0;
                 for( double dt: motionBuffer) D+=dt;
                 
                 if( D > adaptiveThreashold ) {
                         adaptiveThreashold *= ALPHA;
                         adaptiveThreashold+= (1-ALPHA) * D;
                         
                         if( sample_counter > SAMPLING_MAXIMUM ) {
                                 sample_counter = 0;
                                 return true;
                         }
                 } else {
                         sample_counter++;
                         adaptiveThreashold = Math.max( adaptiveThreashold * BETA, MINIMUM_THREASHOLD );
                 }
                 return false;
     }
}