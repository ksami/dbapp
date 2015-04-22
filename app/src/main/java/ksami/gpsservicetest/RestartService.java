package ksami.gpsservicetest;

import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        /* �������� service ��� ���� */
        ActivityManager am = (ActivityManager)context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);

        boolean isRun = false;
        for(int i=0; i<rs.size(); i++){
            ActivityManager.RunningServiceInfo rsi = rs.get(i);
            if(rsi.service.getClassName().equals("ksami.gpsservicetest.GpsService")) isRun = true;
            //Log.d("run service","Package Name : " + rsi.service.getClassName());
        }
        if(!isRun){
            Log.e("RestartService", "GpsService is dead");
            Intent service = new Intent(context, GpsService.class);
            context.startService(service);
            Log.e("RestartService", "Restart GpsService");
        }
        else{
            //Log.e("RestartService", "SleepService is alivce");
        }
    }
}