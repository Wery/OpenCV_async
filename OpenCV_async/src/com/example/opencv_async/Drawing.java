package com.example.opencv_async;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;

public class Drawing extends View {
    Paint paintMagentaFill = new Paint();
    Paint paintBlue = new Paint();
    float x=0;
    float y=0;
    float x_offset=0;
    float y_offset=0;
    public ArrayList<PointF> mPointers;
    
    public Drawing(Context context) {
        super(context);
        paintMagentaFill.setColor(Color.GREEN);;
        paintMagentaFill.setStyle(Paint.Style.FILL); 
        paintMagentaFill.setColor(Color.BLUE);
        mPointers = new ArrayList<PointF>();
    }
    
    // SET VALUES
    public void setX(float newX)
    {
    	this.x = newX;
    }
    public void setY(float newY)
    {
    	this.y = newY;
    }
    public void setXoffset(float newX)
    {
    	this.x_offset = newX;
    }
    public void setYoffset(float newY)
    {
    	this.y_offset = newY;
    }

    // GET VALUES
    public float getX(){
    	return this.x;
    }
    public float getY(){
    	return this.y;
    }
    public float getXoffset(){
    	return this.x_offset;
    }
    public float getYoffset(){
    	return this.y_offset;
    }
    
    public void drawCircle(float x, float y){
    	PointF pt = new PointF(x, y);
    	mPointers.add(pt);    	
    	//this.x =x;
    	//this.y = y;
    	//m_Canvas.drawCircle(x, y, 10, paintMagentaFill);
    	invalidate();  
    }
    
    public void addCircle(float x, float y)
    {
    	
    }
    
    @Override
    public void onDraw(Canvas canvas) {
    	for (PointF pf : mPointers) {
    		canvas.drawCircle(pf.x, pf.y, 10, paintMagentaFill);	
		}    	
    }

}