package ksami.gpsservicetest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

import java.util.LinkedList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
    private static final String KEY_COUNT = "Count";

    /* 
     * DataTypes
     */
    public class UserPos{
        public Date date;
        public int time;
        public int day;
        public int x;
        public int y;
        public String area;

        UserPos(){
            date = null;
            time = -1;
            day = -1;
            x = -1;
            y = -1;
            area = null;
        }

        UserPos(Date date, int time, int day, int x, int y, String area){
            this.date = date;
            this.time = time;
            this.day = day;
            this.x = x;
            this.y = y;
            this.area = area;
        }
    }

    public class NewPrediction {
        public int hour;
        public int day_of_week;
        public String area;

        NewPrediction(){
            hour = -1;
            day_of_week = -1;
            area = null;
        }

        NewPrediction(int hour, int day_of_week, String fut_area_name){
            this.hour = hour;
            this.day_of_week = day_of_week;
            this.area = fut_area_name;
        }
    }

    public ProjectSQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createUPDB = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s(%s, %s))".format(TABLE_NAME_USERPOS, KEY_DATE, "DATETIME", KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_X, "INTEGER", KEY_Y, "INTEGER", KEY_AREA, "TEXT", "PRIMARY KEY", KEY_DATE, KEY_HOUR);
        String createFPDB = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, %s %s)".format(TABLE_NAME_FUTPOS, KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_X, "INTEGER", KEY_Y, "INTEGER", KEY_AREA, "TEXT", KEY_COUNT, "INTEGER");
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

    /* 1. Method to record user position */
    public void addUserPos(int x, int y, String area) {
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
                + y + ", "
                + area + ")");
        db.close();
    }

    public void addUserPos(UserPos x) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
                + "date('now'), "
                + x.time + ", "
                + x.day + ", "
                + x.x + ", "
                + x.y + ")");
        db.close();
    }
/*
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
                } catch (ParseException e) {
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
*/
    /* 4. Query by date */
    /*public LinkedList<UserPos> getUserPos(Date a, Date b) {
        SQLiteDatabase db = this.getReadableDatabase();
        SimpleDateFormat dateform = new SimpleDateFormat("yyyy-MM-dd");

        String d1 = dateform.format(a);
        String d2 = dateform.format(b);

        String query = "SELECT * FROM %s WHERE %s between '%s' and '%s'";
        query = String.format(query, TABLE_NAME_USERPOS, KEY_DATE, d1, d2);

        Cursor cursor = db.rawQuery(query, null);
        LinkedList<UserPos> list = new LinkedList<UserPos>();
        UserPos temp;

        if(cursor.moveToFirst()){
            do{
                Date v1 = null;
                try {
                    v1 = dateform.parse(cursor.getString(0));
                } catch (ParseException e) {
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
*/
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
    public void increase_count(NewPrediction newData)
    {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM %s WHERE %s = %d AND %s = %d AND %s = %s";
        String.format(query, TABLE_NAME_FUTPOS, KEY_HOUR, newData.hour, KEY_DAY_OF_WEEK, newData.day_of_week, KEY_AREA, newData.area);
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.getCount() == 0)
        {
            query = "INSERT INTO %s VALUES (%d, %d, %s, %d)";
            String.format(query, TABLE_NAME_FUTPOS, newData.hour, newData.day_of_week, newData.area, 1);
        }
        else
        {
            query = "UPDATE %s SET %s = %d WHERE %s = %d AND %s = %d AND %s =%s";
            String.format(query, TABLE_NAME_FUTPOS, KEY_COUNT, cursor.getInt(4)+1, KEY_HOUR, cursor.getInt(1),
                    KEY_DAY_OF_WEEK, cursor.getInt(2), KEY_AREA, cursor.getString(3));
        }

        db.execSQL(query);

        cursor.close();
        db.close();
    }

    public void init_table() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "DELETE * FROM %s";
        String.format(query, TABLE_NAME_FUTPOS);
        db.execSQL(query);

        //
        query = "SELECT %s, %s, %s, COUNT(*) FROM %s GROUP BY %s, %s, %s";
        String.format(query, KEY_HOUR, KEY_DAY_OF_WEEK, KEY_AREA, TABLE_NAME_USERPOS, KEY_HOUR, KEY_DAY_OF_WEEK, KEY_AREA);
        Cursor cursor = db.rawQuery(query, null);

        //
        while(cursor.moveToNext()){
            query = "INSERT INTO %s VALUES (%d %d %s %d)";
            String.format(query, TABLE_NAME_FUTPOS, cursor.getInt(1), cursor.getInt(2), cursor.getString(3), cursor.getInt(4));
            db.execSQL(query);
        }

        cursor.close();
        db.close();
    }

    public void insert_temp(int x, int y, String area) {
        SQLiteDatabase db = this.getWritableDatabase();

        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
                + "date('now'), "
                + hours + ", "
                + day + ", "
                + area + ")");

        String query = "INSERT OR REPLACE INTO %s (%s, %s, %s, %s) VALUES(%d, %d, %s, COALESCE((SELECT %s FROM %s WHERE %s = %d AND %s = %d and %s = '%s'), 1)";

        db.execSQL(String.format(query, TABLE_NAME_FUTPOS,
                KEY_DAY_OF_WEEK, KEY_HOUR, KEY_AREA, KEY_COUNT,
                day, hours, area,
                KEY_COUNT + "+1", TABLE_NAME_FUTPOS,
                KEY_DAY_OF_WEEK, day, KEY_HOUR, hours, KEY_AREA, area));

        String subquery = "SELECT %s FROM %s WHERE %s = '%s'";
        String queryA = String.format(subquery, KEY_X, TABLE_NAME_COORD, KEY_AREA, area);
        String queryB = String.format(subquery, KEY_Y, TABLE_NAME_COORD, KEY_AREA, area);
        query = "INSERT OR REPLACE INTO %s (%s, %s, %s) VALUES('%s', COALESCE(%s, %d), COALESCE(%s, %d))";
        db.execSQL(String.format(query,
                TABLE_NAME_COORD, KEY_AREA, KEY_X, KEY_Y,
                area, queryA, x, queryB, y));
        db.close();
    }

    public LinkedList<QueryResult> defaultQuery() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "select day, hour, area, grid_x, grid_y from coordination_transit natural joins (select day, hour, area, rank() over (partition by day, hour order by sum(count) desc) as c_rank from future_position group by day, hour, area having c_rank = 1)";

        Cursor cursor = db.rawQuery(query, null);
        LinkedList<QueryResult> list = new LinkedList<QueryResult>();
        QueryResult r = null;

        if(cursor.moveToFirst()) {
            do{
                int v1 = cursor.getInt(0);
                int v2 = cursor.getInt(1);
                String v3 = cursor.getString(2);
                int v4 = cursor.getInt(3);
                int v5 = cursor.getInt(4);
                r = new QueryResult(v1, v2, v3, v4, v5);
                list.add(r);
            } while(cursor.moveToNext());
        }
        db.close();
        return list;
    }


    public class QueryResult{
        public int day;
        public int hour;
        public String area;
        public int grid_x;
        public int grid_y;

        QueryResult(int day, int hour, String area, int grid_x, int grid_y){
            this.day = day;
            this.hour = hour;
            this.area = area;
            this.grid_x = grid_x;
            this.grid_y = grid_y;
        }
    }

    public QueryResult[] mymethod(){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        int myhour = hour;
        int myday = day;
        QueryResult[] data = new QueryResult[8];
        for(int i = 0; i < 8; i++)
            data[i] = null;
        int index = 0;

        if((hour%3)!=0) hour = (hour / 3) * 3 + 3;

        LinkedList<QueryResult> r = defaultQuery();
        QueryResult first = r.getFirst();
        QueryResult x = null;
        QueryResult last = r.getLast();

        for(int i = 0; i < 8; i++) {
            if(hour == 24) {
                hour = 0;
                day += 1;
            }
            if(day > Calendar.SATURDAY)
                day = Calendar.SUNDAY;

            for(int j = 0; ; j++) {
                x = r.get(j);

                if((x.hour == hour) && (x.day == day)) {
                    data[index] = x;
                    index++;
                    break;
                }

                if(x == last)
                    break;
            }

            hour += 3;
        }

        return data;
    }


}