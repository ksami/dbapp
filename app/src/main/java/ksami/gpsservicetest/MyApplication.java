package ksami.gpsservicetest;

import android.app.Application;

public class MyApplication extends Application {
    private ProjectSQL sql;
    public ProjectSQL getDb() {return sql;}
    public void setData(ProjectSQL sql) {this.sql = sql;}
}