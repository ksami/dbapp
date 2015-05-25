package ksami.gpsservicetest;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;


public class DisplayWeatherActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_weather);

        Intent intent = getIntent();
        int gridx = Integer.parseInt(intent.getStringExtra(MainActivity.GRIDX));
        int gridy = Integer.parseInt(intent.getStringExtra(MainActivity.GRIDY));

        ArrayList<KmaData> kmaList = XmlParser2.parsing(gridx, gridy);
        String weatherInfo = "displaying weather info:\n\n";
        for (int i = 0; i < 15; i++)  //display result
        {
            weatherInfo += i + " ";
            weatherInfo += "/day " + kmaList.get(i).day;

            weatherInfo += "/wfEn " +kmaList.get(i).wfEn + " ";
            weatherInfo += "/tmn " +kmaList.get(i).tmn + " ";
            weatherInfo += "/tmx " +kmaList.get(i).tmx + " ";

            weatherInfo += "/hour " +kmaList.get(i).hour + " ";
            weatherInfo += "\n";
        }

        TextView tv = (TextView) findViewById(R.id.textViewWeatherInfo);
        tv.setText(weatherInfo);
    }


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
