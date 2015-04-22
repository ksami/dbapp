package ksami.gpsservicetest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

@SuppressLint({ "SdCardPath", "HandlerLeak"})
public class GpsService extends Service {

    boolean quit;
    Location mLocation;
    Location pLocation;
    LocationManager mlocManager;
    private String provider;

    @Override
    public void onCreate() {
        super.onCreate();

        this.mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.GPS_PROVIDER;
        mlocManager.requestLocationUpdates(provider, 60*1000, 10, new LocationListener() {
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
            public void onLocationChanged(Location location) {
                mLocation = location;
                handler.sendEmptyMessageDelayed(0, 0);
            }
        });
        Log.d("GPSService", "++onCreate++");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.d("GPSService", "++handleMessage++");
            try {
                saveFile(mLocation.getLongitude(), mLocation.getLatitude(), mLocation.getSpeed(), mLocation.getAccuracy());
                Log.d("GPSService", "����!");
            } catch (Exception e) {
                Log.e("GPSService", "����!");
                e.printStackTrace();
            }
        }
    };

    private FileWriter osw;
    private BufferedWriter writer;
    private void saveFile(double longitude, double latitude, double speed, double accuracy){
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/dlns/gps");
            dir.mkdirs();
            osw = new FileWriter("/sdcard/dlns/gps/" + getFileName(), true);
            writer = new BufferedWriter(osw);
            writer.write(getCurrentTime() + "\t" + longitude + "\t" + latitude + "\t" + speed + "\t" + accuracy + "\n");
            writer.close();
            Log.d("GPSService", "/sdcard/dlns/gps/" + getFileName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileName(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        String today = String.format("%04d_%02d_%02d", year, month, date);
        return today + ".txt";
    }

    public String getCurrentTime(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        if(calendar.get(Calendar.AM_PM) == Calendar.PM) hour += 12;
        String time = String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, date, hour, minute, second);
        return time;
    }
}
