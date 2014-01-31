package com.example.opencv_async;

public class SensorData {

	int id;
    String time;
    float x;
    float y;
    float z;
 
    // constructors
    public SensorData() {
    }
 
    public SensorData(String t, float x, float y, float z) {
        this.time = t;
        this.x = x;
        this.y = y;
        this.z = z;
    }
 
    public SensorData(int id, String t, float x, float y, float z) {
        this.id = id;
        this.time = t;
        this.x = x;
        this.y = y;
        this.z = z;
    }
 
    // setters
    public void setId(int id) {
        this.id = id;
    }
 
    public void setTime(String t) {
        this.time = t;
    }
 
    public void setX(float x) {
        this.x = x;
    }
    
    public void setY(float y) {
        this.y = y;
    } 
    
    public void setZ(float z) {
        this.z = z;
    }
 
    // getters
    public long getId() {
        return this.id;
    }
 
    public String getTime() {
        return this.time;
    }
 
    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getZ() {
        return this.z;
    }

}
