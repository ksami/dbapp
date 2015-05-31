package ksami.gpsservicetest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

@SuppressLint({ "SdCardPath", "HandlerLeak"})
public class GpsService extends Service {

    boolean quit;
    Location mLocation;
    Location pLocation;
    LocationManager mlocManager;
    private String provider;

    //database
    MyApplication appState;
    ProjectSQL sql;

    public void onCreate() {
        super.onCreate();

        appState = (MyApplication) getApplicationContext();
        sql = appState.getDb();

        this.mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.GPS_PROVIDER;
        mlocManager.requestLocationUpdates(provider, 3*60*60*1000, 10, new LocationListener() {
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

    //
    // LCC DFS 좌표변환을 위한 기초 자료
    // LCC DFS 좌표변환 위경도->좌표 ( v1:위도, v2:경도 )
    // Parameter: v1 Latitude, v2 Longitude
    // Returns int array [gridx, gridy]
    public int[] dfs_xy_conv(double v1, double v2) {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        int XO = 43; // 기준점 X좌표(GRID)
        int YO = 136; // 기1준점 Y좌표(GRID)

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (v1) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = v2 * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        Log.d("GPSService", "theta: " + theta);
        Log.d("GPSService", "double gridx: " + Math.floor(ra * Math.sin(theta) + XO + 0.5));

        int gridx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int gridy = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[] {gridx, gridy};
    }


    /** 위도와 경도 기반으로 주소를 리턴하는 메서드*/
    public String getAddress(double lat, double lng){
        String address = null;

        //위치정보를 활용하기 위한 구글 API 객체
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        //주소 목록을 담기 위한 HashMap
        List<Address> list = null;

        try{
            list = geocoder.getFromLocation(lat, lng, 1);
        } catch(Exception e){
            e.printStackTrace();
        }

        if(list == null){
            Log.e("GPSService.getAddress", "주소 데이터 얻기 실패");
            return null;
        }

        if(list.size() > 0){
            Address addr = list.get(0);
            address = addr.getLocality() + " "
                    + addr.getThoroughfare();
//            address = addr.getCountryName() + " "
//                    + addr.getPostalCode() + " "
//                    + addr.getLocality() + " "
//                    + addr.getThoroughfare() + " "
//                    + addr.getFeatureName();
        }

        return address;
    }


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
            int [] gridxy = dfs_xy_conv(latitude, longitude);
            writer.write("x: " + gridxy[0] + ", y: " + gridxy[1] + "\n\n");
            writer.close();
            Log.d("GPSService", "/sdcard/dlns/gps/" + getFileName());

            //database
            String area = getAddress(latitude, longitude);
            sql.insert_temp(gridxy[0], gridxy[1], area);
            Log.d("GPSService", "coord added to database");
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
