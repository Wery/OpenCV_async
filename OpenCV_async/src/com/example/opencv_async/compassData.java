package com.example.opencv_async;

public class compassData {
	private int id;
	private String time;
	private int azimuth;
 
    // constructors
    public compassData() {
    }
 
    public compassData(String t, int a) {
        this.time = t;
        this.azimuth = a;
    }
 
    public compassData(int id, String t, int a) {
        this.id = id;
        this.time = t;
        this.azimuth = a;
    }
 
    // setters
    public void setId(int id) {
        this.id = id;
    }
 
    public void setTime(String t) {
        this.time = t;
    }
 
    public void setAzimuth(int a) {
        this.azimuth = a;
    }
 
    // getters
    public long getId() {
        return this.id;
    }
 
    public String getTime() {
        return this.time;
    }
 
    public int getAzimuth() {
        return this.azimuth;
    }
}
