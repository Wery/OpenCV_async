package com.example.opencv_async;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG = "SqLiteSensorDataManager";
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "sensorDatabase.db";
    
    // 3 tabele dla 3 czujnikow        
    // id  |  time  |  X  |  Y  |  Z
    
    public static final String KEY_ID = "_id";
    public static final String ID_OPTIONS = "INTEGER PRIMARY KEY AUTOINCREMENT";
    public static final int ID_COLUMN = 0;
    
    public static final String KEY_TIME = "Time";
    public static final String TIME_OPTIONS = "TEXT";
    public static final int TIME_COLUMN = 4;
            
    public static final String KEY_X = "X";
    public static final String X_OPTIONS = "TEXT";
    public static final int X_COLUMN = 2;
    
    public static final String KEY_Y = "Y";
    public static final String Y_OPTIONS = "TEXT";
    public static final int Y_COLUMN = 3;
    
    public static final String KEY_Z = "Z";
    public static final String Z_OPTIONS = "TEXT";
    public static final int Z_COLUMN = 4;
    
    
    private static final String DB_ACC_TABLE = "AccData";
    private static final String DB_CREATE_ACC_TABLE =
            "CREATE TABLE " + DB_ACC_TABLE + "( " +
            KEY_ID + " " + ID_OPTIONS + ", " +
            KEY_TIME + " " + TIME_OPTIONS + ", " +
            KEY_X + " " + X_OPTIONS + ", " +
            KEY_Y + " " + Y_OPTIONS + ", " +
            KEY_Z + " " + Z_OPTIONS +
            ");";
    
    private static final String DB_GYRO_TABLE = "GyroData";
    private static final String DB_CREATE_GYRO_TABLE =
            "CREATE TABLE " + DB_GYRO_TABLE + "( " +
            KEY_ID + " " + ID_OPTIONS + ", " +
            KEY_TIME + " " + TIME_OPTIONS + ", " +
            KEY_X + " " + X_OPTIONS + ", " +
            KEY_Y + " " + Y_OPTIONS + ", " +
            KEY_Z + " " + Z_OPTIONS +
            ");";
    
    private static final String DB_MAGNET_TABLE = "MagnetData";
    private static final String DB_CREATE_MAGNET_TABLE =
            "CREATE TABLE " + DB_MAGNET_TABLE + "( " +
            KEY_ID + " " + ID_OPTIONS + ", " +
            KEY_TIME + " " + TIME_OPTIONS + ", " +
            KEY_X + " " + X_OPTIONS + ", " +
            KEY_Y + " " + Y_OPTIONS + ", " +
            KEY_Z + " " + Z_OPTIONS +
            ");";
    
    // tabela dla kompasu
    // id  |  time  |  azimuth
           
    public static final String KEY_AZIMUTH = "azimuth";
    public static final String AZIMUTH_OPTIONS = "INTEGER";
    public static final int AZIMUTH_COLUMN = 4;
    
    private static final String DB_COMPASS_TABLE = "CompassData";
    private static final String DB_CREATE_COMPASS_TABLE =
            "CREATE TABLE " + DB_COMPASS_TABLE + "( " +
            KEY_ID + " " + ID_OPTIONS + ", " +
            KEY_TIME + " " + TIME_OPTIONS + ", " +
            KEY_AZIMUTH + " " + AZIMUTH_OPTIONS +
            ");";
    
    
    private static final String DROP_ACC_TABLE =
            "DROP TABLE IF EXISTS " + DB_ACC_TABLE;
    private static final String DROP_GYRO_TABLE =
            "DROP TABLE IF EXISTS " + DB_GYRO_TABLE;
    private static final String DROP_MAGNET_TABLE =
            "DROP TABLE IF EXISTS " + DB_MAGNET_TABLE;
    private static final String DROP_COMPASS_TABLE =
            "DROP TABLE IF EXISTS " + DB_COMPASS_TABLE;
    
        
    public DatabaseHelper(Context context) {
        //super(context, DB_NAME, null, DB_VERSION);
    	super(context, Environment.getExternalStorageDirectory()
                + File.separator + "/DataBase/" + File.separator
                + DB_NAME, null, DB_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE_ACC_TABLE);
        db.execSQL(DB_CREATE_GYRO_TABLE);
        db.execSQL(DB_CREATE_MAGNET_TABLE);
        db.execSQL(DB_CREATE_COMPASS_TABLE);
 
        Log.d(DEBUG_TAG, "Database creating...");
        Log.d(DEBUG_TAG, "Table " + DB_ACC_TABLE + " ver." + DB_VERSION + " created");
        Log.d(DEBUG_TAG, "Table " + DB_GYRO_TABLE + " ver." + DB_VERSION + " created");
        Log.d(DEBUG_TAG, "Table " + DB_MAGNET_TABLE + " ver." + DB_VERSION + " created");
        Log.d(DEBUG_TAG, "Table " + DB_COMPASS_TABLE + " ver." + DB_VERSION + " created");
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_ACC_TABLE);
        db.execSQL(DROP_GYRO_TABLE);
        db.execSQL(DROP_MAGNET_TABLE);
        db.execSQL(DROP_COMPASS_TABLE);
        
        
        Log.d(DEBUG_TAG, "Database updating...");
        Log.d(DEBUG_TAG, "Table " + DB_ACC_TABLE + " updated from ver." + oldVersion + " to ver." + newVersion);
        Log.d(DEBUG_TAG, "Table " + DB_GYRO_TABLE + " updated from ver." + oldVersion + " to ver." + newVersion);
        Log.d(DEBUG_TAG, "Table " + DB_MAGNET_TABLE + " updated from ver." + oldVersion + " to ver." + newVersion);
        Log.d(DEBUG_TAG, "Table " + DB_COMPASS_TABLE + " updated from ver." + oldVersion + " to ver." + newVersion);
        Log.d(DEBUG_TAG, "All data is lost.");
 
        onCreate(db);
    }
    

    // ------------------------ "ACC SENDOR" table methods ----------------//
    public void createAccSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, getDateTime());
        values.put(KEY_X, Float.toString(SensorData.getX()));
        values.put(KEY_Y, Float.toString(SensorData.getY()));
        values.put(KEY_Z, Float.toString(SensorData.getZ()));
        
        // insert row
        db.insert(DB_ACC_TABLE, null, values);
        db.close();
    }
 
    /**
     * get single SensorData
     */
    public SensorData getAccSensorData(long data_id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        String selectQuery = "SELECT  * FROM " + DB_ACC_TABLE + " WHERE "
                + KEY_ID + " = " + data_id;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c != null)
            c.moveToFirst();
 
        SensorData td = new SensorData();
        td.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
        td.setX((c.getInt(c.getColumnIndex(KEY_X))));
        td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
        td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
        return td;
    }
 
    /**
     * getting all SensorDatas
     * */
    public List<SensorData> getAllAccSensorDatas() {
        List<SensorData> SensorDatas = new ArrayList<SensorData>();
        String selectQuery = "SELECT  * FROM " + DB_ACC_TABLE;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	SensorData td = new SensorData();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
                td.setX((c.getInt(c.getColumnIndex(KEY_X))));
                td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
                td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
                // adding to SensorData list
                SensorDatas.add(td);
            } while (c.moveToNext());
        }
 
        return SensorDatas;
    }
 

    /**
     * getting SensorData count
     */
    public int getAccSensorDataCount() {
        String countQuery = "SELECT  * FROM " + DB_ACC_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
 
        int count = cursor.getCount();
        cursor.close();
 
        // return count
        return count;
    }
 
    /**
     * Updating a SensorData
     */
    public int updateAccSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, SensorData.getTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
        
        // updating row
        return db.update(DB_ACC_TABLE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(SensorData.getId()) });
    }
 
    /**
     * Deleting a SensorData
     */
    public void deleteAccSensorData(long _id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DB_ACC_TABLE, KEY_ID + " = ?",
                new String[] { String.valueOf(_id) });
    }
 
    // ------------------------ "tags" table methods ----------------//
    public void createGyroSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, getDateTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
 
        // insert row
        db.insert(DB_GYRO_TABLE, null, values);
    }
 
    /**
     * get single SensorData
     */
    public SensorData getGyroSensorData(long data_id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        String selectQuery = "SELECT  * FROM " + DB_GYRO_TABLE + " WHERE "
                + KEY_ID + " = " + data_id;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c != null)
            c.moveToFirst();
 
        SensorData td = new SensorData();
        td.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
        td.setX((c.getInt(c.getColumnIndex(KEY_X))));
        td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
        td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
        return td;
    }
 
    /**
     * getting all SensorDatas
     * */
    public List<SensorData> getAllGyroSensorDatas() {
        List<SensorData> SensorDatas = new ArrayList<SensorData>();
        String selectQuery = "SELECT  * FROM " + DB_GYRO_TABLE;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	SensorData td = new SensorData();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
                td.setX((c.getInt(c.getColumnIndex(KEY_X))));
                td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
                td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
                // adding to SensorData list
                SensorDatas.add(td);
            } while (c.moveToNext());
        }
 
        return SensorDatas;
    }
 

    /**
     * getting SensorData count
     */
    public int getGyroSensorDataCount() {
        String countQuery = "SELECT  * FROM " + DB_GYRO_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
 
        int count = cursor.getCount();
        cursor.close();
 
        // return count
        return count;
    }
 
    /**
     * Updating a SensorData
     */
    public int updateGyroSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, SensorData.getTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
        
        // updating row
        return db.update(DB_GYRO_TABLE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(SensorData.getId()) });
    }
 
    /**
     * Deleting a SensorData
     */
    public void deleteGyroSensorData(long _id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DB_GYRO_TABLE, KEY_ID + " = ?",
                new String[] { String.valueOf(_id) });
    }
    
    // ------------------------ "tags" table methods ----------------//
    public void createMagnetSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, getDateTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
 
        // insert row
        db.insert(DB_MAGNET_TABLE, null, values);
    }
 
    /**
     * get single SensorData
     */
    public SensorData getMagnetSensorData(long data_id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        String selectQuery = "SELECT  * FROM " + DB_MAGNET_TABLE + " WHERE "
                + KEY_ID + " = " + data_id;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c != null)
            c.moveToFirst();
 
        SensorData td = new SensorData();
        td.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
        td.setX((c.getInt(c.getColumnIndex(KEY_X))));
        td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
        td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
        return td;
    }
 
    /**
     * getting all SensorDatas
     * */
    public List<SensorData> getAllMagnetSensorDatas() {
        List<SensorData> SensorDatas = new ArrayList<SensorData>();
        String selectQuery = "SELECT  * FROM " + DB_MAGNET_TABLE;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	SensorData td = new SensorData();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
                td.setX((c.getInt(c.getColumnIndex(KEY_X))));
                td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
                td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
                // adding to SensorData list
                SensorDatas.add(td);
            } while (c.moveToNext());
        }
 
        return SensorDatas;
    }
 

    /**
     * getting SensorData count
     */
    public int getMagnetSensorDataCount() {
        String countQuery = "SELECT  * FROM " + DB_MAGNET_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
 
        int count = cursor.getCount();
        cursor.close();
 
        // return count
        return count;
    }
 
    /**
     * Updating a SensorData
     */
    public int updateMagnetSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, SensorData.getTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
        
        // updating row
        return db.update(DB_MAGNET_TABLE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(SensorData.getId()) });
    }
 
    /**
     * Deleting a SensorData
     */
    public void deleteMagnetSensorData(long _id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DB_MAGNET_TABLE, KEY_ID + " = ?",
                new String[] { String.valueOf(_id) });
    }
    
    // ------------------------ "tags" table methods ----------------//
    public void createCompassSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, getDateTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
        
 
        // insert row
        db.insert(DB_COMPASS_TABLE, null, values);
    }
 
    /**
     * get single SensorData
     */
    public SensorData getCompassSensorData(long data_id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        String selectQuery = "SELECT  * FROM " + DB_COMPASS_TABLE + " WHERE "
                + KEY_ID + " = " + data_id;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c != null)
            c.moveToFirst();
 
        SensorData td = new SensorData();
        td.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
        td.setX((c.getInt(c.getColumnIndex(KEY_X))));
        td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
        td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
        return td;
    }
 
    /**
     * getting all SensorDatas
     * */
    public List<SensorData> getAllCompassSensorDatas() {
        List<SensorData> SensorDatas = new ArrayList<SensorData>();
        String selectQuery = "SELECT  * FROM " + DB_COMPASS_TABLE;
 
        Log.e(DEBUG_TAG, selectQuery);
 
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	SensorData td = new SensorData();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setTime((c.getString(c.getColumnIndex(KEY_TIME))));
                td.setX((c.getInt(c.getColumnIndex(KEY_X))));
                td.setY((c.getInt(c.getColumnIndex(KEY_Y))));
                td.setZ((c.getInt(c.getColumnIndex(KEY_Z))));
 
                // adding to SensorData list
                SensorDatas.add(td);
            } while (c.moveToNext());
        }
 
        return SensorDatas;
    }
 

    /**
     * getting SensorData count
     */
    public int getCompassSensorDataCount() {
        String countQuery = "SELECT  * FROM " + DB_COMPASS_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
 
        int count = cursor.getCount();
        cursor.close();
 
        // return count
        return count;
    }
 
    /**
     * Updating a SensorData
     */
    public int updateCompassSensorData(SensorData SensorData) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_ID, SensorData.getId());
        values.put(KEY_TIME, SensorData.getTime());
        values.put(KEY_X, SensorData.getX());
        values.put(KEY_Y, SensorData.getY());
        values.put(KEY_Z, SensorData.getZ());
        
        // updating row
        return db.update(DB_COMPASS_TABLE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(SensorData.getId()) });
    }
 
    /**
     * Deleting a SensorData
     */
    public void deleteCompassSensorData(long _id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DB_COMPASS_TABLE, KEY_ID + " = ?",
                new String[] { String.valueOf(_id) });
    }
    
    
    // ----------------------------------------------------------------------------------
    /**
     * get datetime
     * */
    public String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }
}
