package ksami.gpsservicetest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ProjectSQL extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1; // final?
    private static final String DATABASE_NAME = "FORECAST";

    private static final String TABLE_NAME_COORD = "coordination_transit";
    private static final String TABLE_NAME_USERPOS = "user_position";
    private static final String TABLE_NAME_FUTPOS = "future_position";
    private static final String KEY_DATE = "Timestamp";
    private static final String KEY_HOUR = "Hour";
    private static final String KEY_DAY_OF_WEEK = "Day_of_week";
    private static final String KEY_X = "Grid_X";
    private static final String KEY_Y = "Grid_Y";
    private static final String KEY_AREA = "Area_name";
    private static final String KEY_PROB = "Probability";


    public ProjectSQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createUPDB = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, %s(%s, %s))".format(TABLE_NAME_USERPOS, KEY_DATE, "DATETIME", KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_X, "INTEGER", KEY_Y, "INTEGER", "PRIMARY KEY", KEY_DATE, KEY_HOUR);
        String createFPDB = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, %s %s)".format(TABLE_NAME_FUTPOS, KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_X, "INTEGER", KEY_Y, "INTEGER", KEY_AREA, "TEXT", KEY_PROB, "DOUBLE");
        String createCTDB = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s(%s, %s))".format(TABLE_NAME_COORD, KEY_X, "INTEGER", KEY_Y, "INTEGER", KEY_AREA, "TEXT", "PRIMARY KEY", KEY_X, KEY_Y);

        db.execSQL(createCTDB);
        db.execSQL(createFPDB);
        db.execSQL(createUPDB);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_USERPOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_FUTPOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_COORD);
        this.onCreate(db);
    }

    /*
     * User position table manage section
     */
    public void addUserPos(UserPos x)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
                + "date('now'), "
                + x.time + ", "
                + x.day + ", "
                + x.x + ", "
                + x.y + ")");
        db.close();
    }

    public LinkedList<UserPos> getUserPos(UserPos x)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        int count = 0;
        if(x.date != null)
            count++;
        if(x.time != -1)
            count++;
        if(x.day != -1)
            count++;
        if(x.x != -1)
            count++;
        if(x.y != -1)
            count++;

        String query = "SELECT * FROM " + TABLE_NAME_USERPOS;

        if(count != 0)
        {
            query += " WHERE ";
            for(int i = 0; i < count; i++)
            {
                if(x.date != null) {
                    query += "Timestamp = '" + format.format(x.date) + "'";
                    x.date = null;
                } else if(x.time != -1) {
                    query += "hour = " + x.time;
                    x.time = -1;
                } else if(x.day != -1) {
                    query += "day = " + x.day;
                    x.day = -1;
                } else if(x.x != -1) {
                    query += "Grid_X = " + x.x;
                    x.x = -1;
                } else if(x.y != -1) {
                    query += "Grid_Y = " + x.y;
                    x.y = -1;
                }

                if(i == count-1)
                    query += ")";
                else
                    query += ", ";
            }
        }

        Cursor cursor = db.rawQuery(query, null);
        LinkedList<UserPos> list = new LinkedList<UserPos>();
        UserPos temp;

        if(cursor.moveToFirst()){
            do{
                Date v1 = null;
                try {
                    v1 = format.parse(cursor.getString(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int v2 = cursor.getInt(cursor.getInt(1));
                int v3 = cursor.getInt(cursor.getInt(2));
                int v4 = cursor.getInt(cursor.getInt(3));
                int v5 = cursor.getInt(cursor.getInt(4));
                temp = new UserPos(v1, v2, v3, v4, v5);
                list.add(temp);
            } while(cursor.moveToNext());
        }

        return list;
    }

    /*
     * Coordination transit table manage section
     */
    public void addCoord(int x, int y, String area)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO " + TABLE_NAME_COORD + " VALUES ("
                + x + ", " + y + ", " + area + ")");
        db.close();
    }

    public String getCoord(int x, int y)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + KEY_AREA
                + " FROM " + TABLE_NAME_COORD
                + " WHERE " + KEY_X + " = " + x + " AND " + KEY_Y + " = " + y;
        Cursor cursor = db.rawQuery(query, null);
        String area = null;

        if(cursor.moveToFirst())
        {
            area = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return area;
    }

    /*
     * Future position table manage section
     */
    public void updateFutpos(NewPrediction newData)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT %s FROM %s WHERE %s = %d AND %s = %d AND %s = %s".format
                (KEY_PROB, TABLE_NAME_FUTPOS, KEY_HOUR, newData.hour, KEY_DAY_OF_WEEK, newData.day_of_week, KEY_AREA, newData.fut_area_name);
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.getCount() == 0)
        {
            //insert newData
        }
        else
        {
            //Update with newData
        }

        cursor.close();
        db.close();
    }


    /* 1. Method to record user position */
    public void addUserPos(int x, int y) {
        SQLiteDatabase db = this.getWritableDatabase();

        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        // round to next multiple of 3
        switch(hours%3) {
            case 0:
                break;
            case 1:
                hours += 2;
                break;
            case 2:
                hours += 1;
                break;
        }
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
                + "date('now'), "
                + hours + ", "
                + day + ", "
                + x + ", "
                + y + ")");
        db.close();
    }


    /* 4. Query by date */
    public LinkedList<UserPos> getUserPos(Date a, Date b) {
        SQLiteDatabase db = this.getReadableDatabase();
        SimpleDateFormat dateform = new SimpleDateFormat("yyyy-MM-dd");

        String d1 = dateform.format(a);
        String d2 = dateform.format(b);

        String query = "SELECT * FROM %s WHERE %s between '%s' and '%s'".format(
                TABLE_NAME_USERPOS, KEY_DATE, d1, d2);

        Cursor cursor = db.rawQuery(query, null);
        LinkedList<UserPos> list = new LinkedList<UserPos>();
        UserPos temp;

        if(cursor.moveToFirst()){
            do{
                Date v1 = null;
                try {
                    v1 = dateform.parse(cursor.getString(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int v2 = cursor.getInt(cursor.getInt(1));
                int v3 = cursor.getInt(cursor.getInt(2));
                int v4 = cursor.getInt(cursor.getInt(3));
                int v5 = cursor.getInt(cursor.getInt(4));
                temp = new UserPos(v1, v2, v3, v4, v5);
                list.add(temp);
            } while(cursor.moveToNext());
        }

        return list;
    }
}
