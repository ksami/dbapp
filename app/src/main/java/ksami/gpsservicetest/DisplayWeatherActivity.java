package ksami.gpsservicetest;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;


public class DisplayWeatherActivity extends ActionBarActivity {

    //database
    MyApplication appState;
    ProjectSQL sql;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_weather);

        new Thread() {
            public void run() {
                connect();
            }
        }.start();
//        connect();
        Toast toast = Toast.makeText(getApplicationContext(), "Requesting information, please wait...", Toast.LENGTH_LONG);
        toast.show();
    }

    public void connect() {
        appState = (MyApplication) getApplicationContext();
        sql = appState.getDb();
//        sql.onUpgrade(sql.getWritableDatabase(), 1, 1);
//        sql.createData();
//        Log.d("DisplayWeatherActivity", "data inserted");

//        Intent intent = getIntent();
//        int gridx = Integer.parseInt(intent.getStringExtra(MainActivity.GRIDX));
//        int gridy = Integer.parseInt(intent.getStringExtra(MainActivity.GRIDY));
//        String area = intent.getStringExtra(MainActivity.AREA);

        final ArrayList<KmaData> kmaList = new ArrayList<KmaData>();
        LinkedList<ProjectSQL.QueryResult> futurePos = sql.defaultQuery();
        Log.d("DisplayWeatherActivity", String.format("futurePos.size(): %d", futurePos.size()));

        try {
            //extract result for specified hour
            for (int i = 0; i < Math.min(8, futurePos.size()); i++) {
                ProjectSQL.QueryResult res = futurePos.get(i);
                int hour = res.hour;
                int gridx = res.grid_x;
                int gridy = res.grid_y;
                String area = res.area;
                KmaData kmadata = new KmaData();

                ArrayList<KmaData> temp = XmlParser2.parsing(gridx, gridy);
                if (temp.size() == 0) {
                    throw new Exception("no connection");
                }

                Log.d("DisplayWeatherActivity", String.format("temp.get(%d).hour: %s", i, temp.get(i).hour));
                //search for matching hour
                for (int j = 0; j < 8; j++) {
                    int obtainedHour = Integer.parseInt(temp.get(j).hour);
                    Log.d("DisplayWeatherActivity", String.format("j: %d", j));
                    if (obtainedHour == hour) {
                        Log.d("DisplayWeatherActivity", "hour match");
                        kmadata = temp.get(j);
                        kmadata.area = area;
                    }
                }
                kmaList.add(kmadata);
            }

            handler.post(new Runnable() {
                public void run() {
                    display(kmaList);
                }
            });

        } catch (Exception e) {
            Log.e("DisplayWeatherActivity", "No connection");
            e.printStackTrace();
        }
    }

    public void display(final ArrayList<KmaData> kmaList) {
        try {

            if (kmaList.size() == 0) {
                throw new Exception("no result");
            }
            Toast toast = Toast.makeText(getApplicationContext(), "Data obtained, displaying data", Toast.LENGTH_LONG);
            toast.show();

            //display in a table
            TableLayout layout = (TableLayout) findViewById(R.id.tableLayout);
            TableLayout.LayoutParams layoutParam = new TableLayout.LayoutParams();
            TableRow[] row = new TableRow[8];
            layoutParam.setMargins(1, 1, 1, 1);
            layoutParam.weight = 1;

            for (int i = 0; i < kmaList.size(); i++) {
                row[i] = new TableRow(this);

                ImageView image = new ImageView(this);
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(96, 96);
                image.setLayoutParams(rowParam);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);

                TextView text = new TextView(this);
                String weatherInfo = "";
                if((Integer.parseInt(kmaList.get(i).hour))<12){
                    weatherInfo += "0" + kmaList.get(i).hour + "00: \n";
                } else {
                    weatherInfo += kmaList.get(i).hour + "00: \n";
                }
                weatherInfo += kmaList.get(i).area +"\n";
                weatherInfo += kmaList.get(i).temp + "°C, ";
                weatherInfo += kmaList.get(i).wfEn;

                String weather = kmaList.get(i).wfEn.toLowerCase();
                Boolean clear = weather.contains("clear");
                Boolean cloud = weather.contains("cloudy");
                Boolean partly = weather.contains("partly") || weather.contains("mostly");
                Boolean rain = weather.contains("rain");
                Boolean snow = weather.contains("snow");

                if (clear)
                    image.setImageResource(R.drawable.sunny);
                else if (cloud && partly)
                    image.setImageResource(R.drawable.half_cloudy);
                else if (cloud)
                    image.setImageResource(R.drawable.cloudy);
                else if (snow)
                    image.setImageResource(R.drawable.snowy);
                else if (rain)
                    image.setImageResource(R.drawable.rainy);

                //add image
                row[i].addView(image);

                //add weatherinfo
                text.setText(weatherInfo);
                row[i].addView(text);

                //add row to table
                layout.addView(row[i]);
            }
        }
        catch(Exception e){
            e.printStackTrace();
            Log.e("DisplayWeatherActivity", "no result");

            TableLayout layout = (TableLayout) findViewById(R.id.tableLayout);
            TableRow row = new TableRow(this);
            TextView text = new TextView(this);
            text.setText("No Results");
            row.addView(text);
            layout.addView(row);

            Toast toast = Toast.makeText(getApplicationContext(), "No Results", Toast.LENGTH_LONG);
            toast.show();
        }

    }
        /*
        String weatherInfo = "";
        for (int i = 0; i < 8; i++)  //display result
        {
            weatherInfo += i + " ";
            weatherInfo += "/day " + kmaList.get(i).day;

            weatherInfo += "/wfEn " +kmaList.get(i).wfEn + " ";
            weatherInfo += "/temp " +kmaList.get(i).temp + " ";
            //weatherInfo += "/tmn " +kmaList.get(i).tmn + " ";
            //weatherInfo += "/tmx " +kmaList.get(i).tmx + " ";

            weatherInfo += "/hour " +kmaList.get(i).hour + " ";
            weatherInfo += "\n";
        }

        TextView tv = (TextView) findViewById(R.id.textViewWeatherInfo);
        tv.setText(weatherInfo);*/



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_weather, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
