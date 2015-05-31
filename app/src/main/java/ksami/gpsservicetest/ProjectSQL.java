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


    public ProjectSQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createUPDB = String.format("CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, PRIMARY KEY (%s, %s))", TABLE_NAME_USERPOS, KEY_DATE, "DATETIME", KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_AREA, "TEXT", KEY_DATE, KEY_HOUR);
        String createFPDB = String.format("CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, PRIMARY KEY (%s, %s, %s))", TABLE_NAME_FUTPOS, KEY_HOUR, "INTEGER", KEY_DAY_OF_WEEK, "INTEGER",
                KEY_AREA, "TEXT", KEY_COUNT, "INTEGER", KEY_DAY_OF_WEEK, KEY_HOUR, KEY_AREA);
        String createCTDB = String.format("CREATE TABLE %s (%s %s PRIMARY KEY, %s %s, %s %s)", TABLE_NAME_COORD, KEY_AREA, "TEXT", KEY_X, "INTEGER", KEY_Y, "INTEGER");
//        String createCTDB = "CREATE TABLE coordination_transit (Area_name TEXT PRIMARY KEY, Grid_X INTEGER, Grid_Y INTEGER)";
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


    public void insert_temp(int x, int y, String area) {
        SQLiteDatabase db = this.getWritableDatabase();

        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        //round to next 3hrs
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
