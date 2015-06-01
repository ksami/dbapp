package ksami.gpsservicetest;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.os.StrictMode;
import android.widget.Toast;


public class MainActivity extends Activity {
//    public static final String GRIDX = "ksami.gpsservicetest.gridx";
//    public static final String GRIDY = "ksami.gpsservicetest.gridy";
//    public static final String AREA = "ksami.gpsservicetest.area";

    String TAG = "MainActivity";

    Button button_personalData;
    Button button_start;
    Button button_stop;

    ProjectSQL sql;
    MyApplication appState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // for accessing sql from anywhere
        sql = new ProjectSQL(MainActivity.this);
        appState = (MyApplication) getApplicationContext();
        appState.setData(sql);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        turnGPSOn();

        //button_personalData = (Button)findViewById(R.id.button1);
        button_start = (Button)findViewById(R.id.button1);
        button_stop = (Button)findViewById(R.id.button2);
		
		/*button_personalData.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i("onClick", "button1");
				Intent intentPersonalData = new Intent(MainActivity.this, PersonalData.class);
				startActivity(intentPersonalData);
			}
		});*/

        ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);

        boolean isRun = false;
        for(int i=0; i<rs.size(); i++){
            ActivityManager.RunningServiceInfo rsi = rs.get(i);
            if(rsi.service.getClassName().equals("ksami.gpsservicetest.GpsService")) isRun = true;
            //Log.d("run service","Package Name : " + rsi.service.getClassName());
        }
        if(!isRun){
            button_start.setEnabled(true);
            button_stop.setEnabled(false);
        }
        else{
            button_start.setEnabled(false);
            button_stop.setEnabled(true);
        }

        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            createGpsDisabledAlert();
        }
    }



    public void button1(View v){
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            createGpsDisabledAlert();
            return;
        }
        Intent intent = new Intent(MainActivity.this, GpsService.class);
        startService(intent);
        registerRestartAlarm();
        button_start.setEnabled(false);
        button_stop.setEnabled(true);
    }

    public void button2(View v){
        unregisterRestartAlarm();
        Intent intent = new Intent(MainActivity.this, GpsService.class);
        stopService(intent);
        button_start.setEnabled(true);
        button_stop.setEnabled(false);
    }

    // Display weather info
    public void button3(View v){
//        int gridx = 59;
//        int gridy = 125;
//        String area = "area";

        Intent intent = new Intent(this, DisplayWeatherActivity.class);
//        intent.putExtra(GRIDX, Integer.toString(gridx));
//        intent.putExtra(GRIDY, Integer.toString(gridy));
//        intent.putExtra(AREA, area);
        startActivity(intent);

        Toast toast = Toast.makeText(getApplicationContext(), "Requesting information, please wait...", Toast.LENGTH_LONG);
        toast.show();
    }

    void registerRestartAlarm() {
        Log.d(TAG, "registerRestartAlarm");
        Intent intent = new Intent(MainActivity.this, RestartService.class);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 1000; // 1�� �Ŀ� �˶��̺�Ʈ �߻�
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 60*1000, sender);
    }

    void unregisterRestartAlarm() {
        Log.d(TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(MainActivity.this, RestartService.class);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    private void turnGPSOn()
    {
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(!provider.contains("gps"))
        {
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }

    private void createGpsDisabledAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS is disabled! Would you like to enable it?");
        builder.setCancelable(false);
        builder.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                showGpsOptions();
            }
        });
        builder.setNegativeButton("Do nothing", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showGpsOptions(){
        Intent gpsOptionIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionIntent);
    }

}
