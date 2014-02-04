package com.example.opencv_async;

public class SharedValue {
	private String _myState;
	private static String _serverIPAddr="http://indoornav.esy.es/getData.php";
	private static String _serverPort = "8080";
	// 手机放置位置设定
	private final static int _HAND_HELD = 1;
	private final static int _TROUSER_POCKET = 2;
	private final static int MAGNETIC_BASED_ALGORITHM = 1;
	private final static int GYROSCOPE_BASED_ALGORITHM = 2;
	private final static int HDE_BASED_ALGORITHM = 3;
	private final static int PSP_BASED_ALGORITHM = 4;
	private int _SENSITIVITY = 100;
	private static SharedValue instance = null;

	public SharedValue() {
		_myState="quake0day";
	}
	public  String getServerAddr(){
		//_serverIPAddr = "128.205.39.117";
		return _serverIPAddr;
	}
	public  String getServerPort(){
		return _serverPort;
	}
	public boolean getStepLengthMode(){
		return true;
	}
	public int getPhonePosition(){
		return _HAND_HELD;
	}
	public float getStepLength(){
		return 40.5f;
	}
	public int getSensitivity(){
		return _SENSITIVITY;
	}
	public int getAlgorithms(){
		return HDE_BASED_ALGORITHM;
	}
	public String getState(){
		return _myState;
	}
	public void setState(String s){
		_myState = s;
	}

}
