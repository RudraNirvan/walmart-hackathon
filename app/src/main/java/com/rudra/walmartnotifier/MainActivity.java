package com.rudra.walmartnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
    int interval =  15 * 60 * 1000; // default == 15 min
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("filename", Context.MODE_PRIVATE);

        if(prefs.getInt("time", -1) == -1) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("time", interval);
            editor.commit();
        } else {
            interval = prefs.getInt("time", 10);
        }

        Button bLogin = (Button) findViewById(R.id.btLogin);
        Button bLogout = (Button) findViewById(R.id.btLogout);
        final LinearLayout llLogin = (LinearLayout) findViewById(R.id.llLogin);
        final LinearLayout llLogout = (LinearLayout) findViewById(R.id.llLogout);

        // alarmUP - to check if alarm is already set
        boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 327,
                new Intent(getApplicationContext(), AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            llLogout.setVisibility(View.VISIBLE);
            llLogin.setVisibility(View.GONE);
        }

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hasInternet() == false) {
                    Toast.makeText(getBaseContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
                    return;
                }
                llLogin.setVisibility(View.GONE);
                llLogout.setVisibility(View.VISIBLE);

                Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 327,
                        new Intent(getApplicationContext(), AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
            }
        });

        bLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                llLogout.setVisibility(View.GONE);
                llLogin.setVisibility(View.VISIBLE);

                Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 327,
                        new Intent(getApplicationContext(), AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.cancel(pendingIntent);

                boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 327,
                        new Intent(getApplicationContext(), AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) != null);
                if(!alarmUp){
                    Toast.makeText(getBaseContext(), "up", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            int hour = (interval / 1000) / 3600;
            int min = ((interval / 1000) % 3600) / 60;

            TimePickerDialog picker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    interval = ((minute * 60) + (hourOfDay * 3600)) * 1000;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("time", interval);
                    editor.commit();
                }
            }, hour, min, true);
            picker.setTitle("Set recurring time");
            picker.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean hasInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /*
        For KitKat: menu key not working on that! :(
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode  == KeyEvent.KEYCODE_MENU) {
            int hour = (interval / 1000) / 3600;
            int min = ((interval / 1000) % 3600) / 60;

            TimePickerDialog picker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    interval = (minute + (hourOfDay * 60)) * 1000;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("time", interval);
                    editor.commit();
                }
            }, hour, min, true);
            picker.setTitle("Set recurring time");
            picker.show();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
