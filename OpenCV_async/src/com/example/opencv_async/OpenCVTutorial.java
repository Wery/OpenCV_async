package com.example.opencv_async;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Size;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

/**
 * @author Erik Hellman <erik.hellman@sonymobile.com>
 */
public class OpenCVTutorial extends Activity implements OpenCVWorker.ResultCallback,
        SurfaceHolder.Callback, View.OnTouchListener, GestureDetector.OnDoubleTapListener {
    
	public static final int DRAW_RESULT_BITMAP = 10;
    private Handler mUiHandler;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Rect mSurfaceSize;
    private OpenCVWorker mWorker;
    private double mFpsResult;
    private Paint mFpsPaint;
    private GestureDetector mGestureDetector;

    private GLLayer glView;
    
    
    private SeekBar seekbar;
    private Button resetBtn;
    private TextView textview;
    private AlertDialog thresholdTypeDialog;
    
    
    // COMPAS STUFF
    private ImageView comapsImage;   
    private LayoutInflater inflater = null;    
    private LayoutInflater inflaterGraph = null;    
    private View compasView;
    private View graphView;
    private View dotView;
    private float currentDegree = 0f;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView stopnieTxtView;
    Float azimut;
    //CustomDrawableView mCustomDrawableView;
    
    Sensor linearAccelerometer;
    Sensor rotationSensor;
    Sensor gyroscope;
    Sensor gravitySensor;
    
    TextView accTxtView;
    TextView gyroTxtView;
    TextView magnetTxtView;
    TextView wifiTB;
    TextView compassTB;
    TextView tv;
    
    Button startBtn;
    Button stopBtn;
    ImageView reddot;
    ImageView yellowdot;
    
    private SeekBar alphaSeekBar;
    
    WifiManager wifimanager;
    private WifiReceiver receiverWifi;
    private Timer timer;
    private IntentFilter i;
    private BroadcastReceiver receiver;
    
    Pedometer pedometer;
    
    GraphView gv;
    GraphViewSeries exampleSeries;
    GraphViewSeries gyroSeries;
    
    private Compass compass;
    
    class WifiReceiver extends BroadcastReceiver { 
    	public void onReceive(Context c, Intent intent) { 
	    	List<ScanResult> wifiList = wifimanager.getScanResults(); 
	    	wifiTB.setText("");
	    	//	WifiInfo info = wifimanager.getConnectionInfo();
	    	for (int i = 0; i < wifiList.size(); i++) { 
		    	ScanResult scanResult = wifiList.get(i); 
		    	 
		    	try{ 
		    		wifiTB.append("SSID: "+scanResult.SSID+" lvl: "+scanResult.level+"\n"); 
		    	} 
	    		catch(Exception e){}
	    	}
    	} 
    	
    	
   	}

    
    /***
     * 
     */
    FrameLayout overlayFramelayout;
    /***
     * 
     */
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
        mGestureDetector = new GestureDetector(new MyOnGestureListener());
        mGestureDetector.setOnDoubleTapListener(this);
        mGestureDetector.setIsLongpressEnabled(false);

        
        
        
       // wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE); 
       // receiverWifi = new WifiReceiver(); 
       // registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); 
       // wifimanager.startScan();
    	
        mFpsPaint = new Paint();
        mFpsPaint.setColor(Color.GREEN);
        mFpsPaint.setDither(true);
        mFpsPaint.setFlags(Paint.SUBPIXEL_TEXT_FLAG);
        mFpsPaint.setTextSize(18);
        mFpsPaint.setTypeface(Typeface.SANS_SERIF);

        glView=new GLLayer(this);        
        
        overlayFramelayout = new FrameLayout(this);
       
        setContentView(overlayFramelayout);
       
        overlayFramelayout.addView(glView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        //setContentView(glView);
   
        mSurfaceView = new SurfaceView(this);

        overlayFramelayout.addView(mSurfaceView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        //addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));            
        
        mSurfaceHolder = mSurfaceView.getHolder();
        
        // Create a Handler that we can post messages to so we avoid having to use anonymous Runnables
        // and runOnUiThread() instead
        mUiHandler = new Handler(getMainLooper(), new UiCallback());

        // compas overlay        
         //inflater = LayoutInflater.from(this);
         inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	     //compasView = inflater.inflate(R.layout.compas, null);
	     compasView = inflater.inflate(R.layout.compas, overlayFramelayout,false);
	     //compasView.bringToFront();
	     
	     overlayFramelayout.addView(compasView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	     //addContentView(compasView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	     
	     
	     alphaSeekBar = (SeekBar)findViewById(R.id.seekBar1); // make seekbar object
	     //textview = (TextView) findViewById(R.id.textView4);        
// -*--*-*-*-*-*-*-*-*-*-*-*--*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
	      exampleSeries =  new GraphViewSeries("LINE ONE", new GraphViewSeriesStyle(Color.BLUE, 2), 
				  new GraphViewData[] { new GraphViewData(0, 0) }); 
    		
	      gyroSeries =  new GraphViewSeries("LINE ONE", new GraphViewSeriesStyle(Color.RED, 2), 
	    				  new GraphViewData[] { new GraphViewData(0, 0) }); 
	      
	      
	    gv = new LineGraphView(
	    	      this // context
	    	      , "GraphViewDemo" // heading
	    );
	    
	    gv.addSeries(exampleSeries);
	    gv.addSeries(gyroSeries);
	    gv.setScrollable(true);
	    gv.setViewPort(1, 500);
	    gv.setScalable(true);
	   
  
// -*--*-*-*-*-*-*-*-*-*-*-*--*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-* 
	     comapsImage = (ImageView) compasView.findViewById(R.id.radar);
	     
	     startBtn = (Button) findViewById(R.id.btn_start);
	     stopBtn = (Button) findViewById(R.id.btn_stop);
	     resetBtn = (Button) findViewById(R.id.reset);
	     
	     accTxtView = (TextView) findViewById(R.id.textView1); 
	     gyroTxtView = (TextView) findViewById(R.id.gyro); 
	     magnetTxtView = (TextView) findViewById(R.id.magnetometer); 	     
	     wifiTB = (TextView) findViewById(R.id.wifiTB); 
	     compassTB = (TextView) findViewById(R.id.textView4);
	     textview = (TextView) findViewById(R.id.textView3);
	     tv = (TextView) findViewById(R.id.textView2);
	     	     
	     compass = new Compass(this,compassTB,gv,exampleSeries);
	     compass.testTextBox = tv;
	     compass.testTextBox2 = compassTB;
	     compass.arrowView = comapsImage;
	     compass.start();
	     	     	     	    
	     //inflaterGraph = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//LayoutInflater.from(this);
	     //dotView = inflater.inflate(R.layout.dot,null);
	     dotView = inflater.inflate(R.layout.dotlayout, overlayFramelayout,false);
	     dotView.bringToFront();
	     overlayFramelayout.addView(dotView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	     //addContentView(dotView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	     reddot = (ImageView) dotView.findViewById(R.id.red);
	     yellowdot = (ImageView) dotView.findViewById(R.id.yellow);
	     
	     //overlayFramelayout.addView(gv, new LayoutParams(500,200));
	     //addContentView(gv, new LayoutParams(500,200));
	     
	     pedometer = new Pedometer(this,reddot);
	     //pedometer.redDot = reddot;
	     pedometer.exampleSeries = exampleSeries;
	     pedometer.gyroSeries = gyroSeries;
	     pedometer.compassTextBox = compassTB;
	     pedometer.testTextBox2 = magnetTxtView;	     
	     pedometer.start();	     
	     	
	     
	     //mCustomDrawableView = new CustomDrawableView(this);
	     //setContentView(mCustomDrawableView); 
        // compas overlay end
	     
	     i = new IntentFilter();
	     i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	     receiver = new BroadcastReceiver(){
	    	 public void onReceive(Context c, Intent i){
	    	  wifimanager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
	    	  List<ScanResult> l = wifimanager.getScanResults();
	    	  StringBuilder sb = new StringBuilder("Scan Results:\n");
	    	  sb.append("-----------------------\n");
	    	  for (ScanResult r : l) {
	    	   sb.append(r.SSID + " " + r.level + " dBM\n");
	    	  }
	    	 // wifiTB.setText(sb.toString());
	    	 }
	    	};   
    
	     initButtons();
	     draw = new Drawing(this);
	     overlayFramelayout.addView(draw);	     
    }  
       
    @Override
    protected void onResume() {
        super.onResume();

	   timer = new Timer(true);
	   timer.scheduleAtFixedRate(new TimerTask(){
	    	 @Override
	    	 public void run() {
	    		 runOnUiThread(new Runnable() {

	                    @Override
	                    public void run() {
	          	    	  wifiTB.setText("");
	        	    	  wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	        	    	  if (wifimanager.isWifiEnabled()) {
	        	    	   WifiInfo info = wifimanager.getConnectionInfo();
	        	    	   if (info != null) {
	        	    		   wifiTB.setText("Associated with " + info.getSSID() +
	        	    	    "\nat " + info.getLinkSpeed() +
	        	    	    WifiInfo.LINK_SPEED_UNITS +
	        	    	    " (" + info.getRssi() + " dBM)");
	        	    	   } else {
	        	    		   wifiTB.setText("Not currently associated.");
	        	    	   }
	        	    	   wifimanager.startScan();
	        	    	  } else {
	        	    		  wifiTB.setText("WIFI is disabled.");
	        	    	  }
	                    }
	                }); 	    		 
	    	 }
	    	}, 0, 2000);
	    	registerReceiver(receiver, i );
        
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setOnTouchListener(this);        
       
        setContentView(overlayFramelayout);

        /*
        setContentView(glView);
        addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        addContentView(compasView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        addContentView(gv, new LayoutParams(500,200));
         */
        
        compass.start();      
        pedometer.start();
      }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        unregisterReceiver(receiver);
        mWorker.stopProcessing();
        mWorker.removeResultCallback(this);

        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(this);
        }
        
        glView.onPause();
        //mSensorManager.unregisterListener(this);
       
        compass.stop();
        pedometer.stop();
    }

  //**************************************************************************** BUTTONS
    
    public void initButtons(){
    	View.OnClickListener btnStartHandler = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				compass.startFlag = true;
				compass.stopFlag = false;
				
				//pedometer.startFlag = true;
				//pedometer.stopFlag = false;
				Toast.makeText(getBaseContext(), "Start logging...", Toast.LENGTH_SHORT).show();			
			}
		};
		
    	View.OnClickListener btnStopHandler = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				compass.stopFlag = true;	
				compass.startFlag = false;
				compass.isFirstSet = true;

				//pedometer.stopFlag = true;	
				//pedometer.startFlag = false;
				//pedometer.isFirstSet = true;
				Toast.makeText(getBaseContext(), "Stop logging...", Toast.LENGTH_SHORT).show();			
			}
		};
		
		
    	View.OnClickListener btnResetHandler = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				pedometer.stepCounter = 0;
				Toast.makeText(getBaseContext(), "Step counter reseted...", Toast.LENGTH_SHORT).show();			
			}
		};
		
		alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		            //Do something here with new value
		        	pedometer.setAlpha((float)((float)progress/(float)100));
		        	textview.setText("alpha: " + pedometer.getAlpha());
		        }

				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
		    });
		
		startBtn.setOnClickListener(btnStartHandler);
		stopBtn.setOnClickListener(btnStopHandler);
		resetBtn.setOnClickListener(btnResetHandler);
    }

  //**************************************************************************** MENU
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("MENU", "called onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "color / bw");
        menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, "threshold type");
        menu.add(Menu.NONE, Menu.FIRST+2, Menu.NONE, "canny");
        menu.add(Menu.NONE, Menu.FIRST+3, Menu.NONE, "wifi");
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       // Log.i("MENU", "called onOptionsItemSelected; selected item: " + item);
        switch(item.getItemId())
        {
            case 1:
            	Log.i("MENU", "onOptionsItemSelected; selected item: " + item);
            	dialogshow();
            return true;
            case 2:
            	showThresholdTypeDialog();
            	Log.i("MENU", "onOptionsItemSelected; selected item: " + item);
            return true;
            case 3:
            	Log.i("MENU", "onOptionsItemSelected; selected item: " + item);
            	mWorker.setThresholdType(3);
            return true;
            case 4:
            	Log.i("MENU", "onOptionsItemSelected; selected item: " + item);
            	wifiInfo();
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }              
    }

    public void showThresholdTypeDialog()
    {
    	final CharSequence[] items = {" adaptacyjny "," globalny "," manualy "};
    	
    	 AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle("Wybierz metode progowania:");
         builder.setSingleChoiceItems(items, mWorker.getThresholdType(), new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item) {
             switch(item)
             {
                 case 0:
                        mWorker.setThresholdType(0);
                        showThresholdAdaptiveValueDialog();
                        break;
                 case 1:                        
                	 	mWorker.setThresholdType(1);
                        break;
                 case 2:
                	 	mWorker.setThresholdType(2);
                	 	showThresholdManualValueDialog();
                        break;                 
             }
             thresholdTypeDialog.dismiss();    
             }
         });
         thresholdTypeDialog = builder.create();
         thresholdTypeDialog.show();
    }

    public void showThresholdManualValueDialog()
    {
    	AlertDialog.Builder popDialog = new AlertDialog.Builder(this);    	
   
    	final SeekBar seek = new SeekBar(this);
		seek.setMax(255);

		popDialog.setIcon(android.R.drawable.btn_star_big_on);
		popDialog.setTitle("Please Select Rank 1-255 ");
		popDialog.setView(seek);
		
		seek.setProgress(mWorker.getmanualThresholdValue());
		seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		            //Do something here with new value
		        	mWorker.setmanualThresholdValue(progress);
		        }

				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
		    });
		 

		// Button OK
		popDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});


		popDialog.create();
		popDialog.show();
		
    }
    
    public void dialogshow()
    {
    	final CharSequence[] items = {" b / w "," rgb ", " rgb CUBE ON "};
    	
    	 AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle("Wybierz metode progowania:");
         builder.setSingleChoiceItems(items, mWorker.getbwRGB(), new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item) {
             switch(item)
             {
                 case 0:
                	 	mWorker.setbwRGB(mWorker.FRAME_BW);                        
                        break;
                 case 1:                        
                	 	mWorker.setbwRGB(mWorker.FRAME_RGB);
                	 	glView.setCubeStatus(glView.CUBE_OFF);
                        break;      
                 case 2:                        
                	 	glView.setCubeStatus(glView.CUBE_ON);
             	 		break;
             }
             thresholdTypeDialog.dismiss();    
             }
         });
         thresholdTypeDialog = builder.create();
         thresholdTypeDialog.show(); 
        
    }

    
    private Bitmap mBitmap;
    Drawing draw;
    private void wifiInfo()
    {
    	mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.red);
   	
    	xr=reddot.getLeft();
    	yr=reddot.getTop();
    	x_move=xr+40;
    	y_move=yr+100;
    	
    	Random r=new Random();
    	float o1=(r.nextFloat()*10);
    	float o2=(r.nextFloat()*10);
    	
    	draw.drawCircle(100+o1, 50+o2);
    	draw.drawCircle(300+o1, 150+o2);
    	draw.drawCircle(400+o1, 250+o2);

    	RelativeLayout.LayoutParams redMarginParams = new  RelativeLayout.LayoutParams(reddot.getLayoutParams());
    	redMarginParams.setMargins(x_move, y_move, 0, 0);
        reddot.setLayoutParams(redMarginParams);
        
        
        xy=yellowdot.getLeft();
    	yy=yellowdot.getTop();
    	x_move=xy+10;
    	y_move=yy+10;
        RelativeLayout.LayoutParams yellowMarginParams = new  RelativeLayout.LayoutParams(reddot.getLayoutParams());
        yellowMarginParams.setMargins(x_move, y_move, 0, 0);
        yellowdot.setLayoutParams(yellowMarginParams);
        
        
        Animation an = new RotateAnimation(0, 180,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
		an.setDuration(5);
		an.setRepeatCount(0);
		an.setFillAfter(true);
		
		comapsImage.startAnimation(an);

    	tv.setText("dot view size:"+draw.mPointers.size());

    	int linkSpeed = wifimanager.getConnectionInfo().getRssi();	
    	Method[] methods = wifimanager.getClass().getDeclaredMethods();
    	for (Method m: methods) {           
    	    if (m.getName().equals("getWifiApConfiguration")) {
    	    	WifiConfiguration config;
				try {
					config = (WifiConfiguration)m.invoke(wifimanager);
					wifiTB.setText("wifi:\nSSID: "+config.SSID+"\nBSSID: "+config.BSSID
							+"\nstatus: "+config.status+"\nlinkSpeed= "+linkSpeed);
	    	        
				} catch (Exception e) {
					e.printStackTrace();
				}
    	        
    	        
    	     //   config.BSSID
    	            // here, the "config" variable holds the info, your SSID is in
    	            // config.SSID
    	    }
    	}
    }
    int xr = 0;
    int yr = 0;
    int xy = 0;
    int yy = 0;
    int x_move = 0;
    int y_move = 0;

    
    public void showThresholdAdaptiveValueDialog()
    {
    	AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
    	
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        
        final View Viewlayout = inflater.inflate(R.layout.settings_menu,
                (ViewGroup) findViewById(R.id.layout_dialog));       

        
        
		popDialog.setIcon(android.R.drawable.btn_star_big_on);
		popDialog.setTitle("Please Select Rank 1-100 ");
		popDialog.setView(Viewlayout);
		
		//  seekBar1
		SeekBar seek1 = (SeekBar) Viewlayout.findViewById(R.id.seekBar1);		
		seek1.setMax(200);
		seek1.setProgress(mWorker.getAdaptiveThresholdBlokSize());		
		seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		            //Do something here with new value
		        	progress = ((int)Math.round(progress/2 ))*2 + 1;
		        	seekBar.setProgress(progress);
		        	mWorker.setadaptiveThresholdBlokSize(progress);		        	
		        }

				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
		    });
		
		//  seekBar2
		SeekBar seek2 = (SeekBar) Viewlayout.findViewById(R.id.seekBar2);
		seek2.setMax(100);
		seek2.setProgress(mWorker.getAdaptiveThresholdValue_C());
		seek2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		            //Do something here with new value
		        	mWorker.setadaptiveThresholdValue_C(progress);
		        }

				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
		    });
		 

		// Button OK
		popDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});


		popDialog.create();		
		popDialog.show();
		
		
		
		//popDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
  
    //**************************************************************************** MENU END
    
    @Override
    public void onResultMatrixReady(Bitmap resultBitmap) {
        mUiHandler.obtainMessage(DRAW_RESULT_BITMAP, resultBitmap).sendToTarget();
    }

    @Override
    public void onFpsUpdate(double fps) {
        mFpsResult = fps;
    }
  
    private void initCameraView() {
        mWorker = new OpenCVWorker(OpenCVWorker.FIRST_CAMERA);
        mWorker.addResultCallback(this);
        new Thread(mWorker).start();
    }

        @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // Initializing OpenCV is done asynchronously. We do this after our SurfaceView is ready.
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, new OpenCVLoaderCallback(this));
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceSize = new Rect(0, 0, width, height);
    }

    @Override
    
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }
    
    //**************************************************************************** TOUCH STUFF
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        pickColorFromTap(event);
        return true;
    }

    private void pickColorFromTap(MotionEvent event) {
        // Calculate the point in the preview frame from the tap point on the screen
        Size previewSize = mWorker.getPreviewSize();
        double xFactor = previewSize.width / mSurfaceView.getWidth();
        double yFactor = previewSize.height / mSurfaceView.getHeight();
        mWorker.setSelectedPoint((int) (event.getX() * xFactor), (int) (event.getY() * yFactor));
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        mWorker.clearSelectedColor();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return false;
    }
    //**************************************************************************** TOUCH STUFF END
    
    private static final class OpenCVLoaderCallback extends BaseLoaderCallback {
        private Context mContext;

        public OpenCVLoaderCallback(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    ((OpenCVTutorial) mContext).initCameraView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }

    }

    
    //**************************************************************************** SENSORS STUFF
    private class UiCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == DRAW_RESULT_BITMAP) {
                Bitmap resultBitmap = (Bitmap) message.obj;
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas();
                    canvas.drawBitmap(resultBitmap, null, mSurfaceSize, null);
                    canvas.drawText(String.format("FPS: %.2f", mFpsResult), 35, 45, mFpsPaint);
                    String msg = "threshold: " + mWorker.getThresholdType() + " , =" + mWorker.getAdaptiveThresholdBlokSize() + " , = " + mWorker.getAdaptiveThresholdValue_C();
                    float width = mFpsPaint.measureText(msg);
                    canvas.drawText(msg, mSurfaceView.getWidth() / 2 - width / 2,
                            mSurfaceView.getHeight() - 30, mFpsPaint);
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                    // Tell the worker that the bitmap is ready to be reused
                    mWorker.releaseResultBitmap(resultBitmap);
                }
            }
            return true;
        }
    }

    private class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
    }

    
    //**************************************************************************** SENSOR STUFF


}
