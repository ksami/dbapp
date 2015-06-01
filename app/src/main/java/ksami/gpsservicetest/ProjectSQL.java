package ksami.gpsservicetest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;
import android.util.Log;

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
                + day + ", '"
                + area + "')");

        String query = "INSERT OR REPLACE INTO %s (%s, %s, %s, %s) VALUES(%d, %d, '%s', COALESCE((SELECT %s FROM %s WHERE %s = %d AND %s = %d and %s = '%s'), 1))";

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

//    public LinkedList<QueryResult> defaultQuery() {
//        SQLiteDatabase db = this.getReadableDatabase();
////        String query = "select day, hour, area, grid_x, grid_y from coordination_transit natural join (select day, hour, area, rank() over (partition by day, hour order by sum(count) desc) as c_rank from future_position group by day, hour, area having c_rank = 1)";
//        String query = "select day_of_week, hour, area_name, grid_x, grid_y from coordination_transit natural join ( select day_of_week, hour, area_name, (( select count(*) from future_position as B where A.day_of_week = B.day_of_week and A.hour = B.hour and A.count > B.count) + 1 ) as c_rank from future_position as A where c_rank = 1)";
//        Cursor cursor = db.rawQuery(query, null);
//        LinkedList<QueryResult> list = new LinkedList<QueryResult>();
//        QueryResult r = null;
//
//        if(cursor.moveToFirst()) {
//            do{
//                int v1 = cursor.getInt(0);
//                int v2 = cursor.getInt(1);
//                String v3 = cursor.getString(2);
//                int v4 = cursor.getInt(3);
//                int v5 = cursor.getInt(4);
//                r = new QueryResult(v1, v2, v3, v4, v5);
//                list.add(r);
//            } while(cursor.moveToNext());
//        }
//        db.close();
//        return list;
//    }

    public LinkedList<QueryResult> defaultQuery() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "select day_of_week, hour, area_name, grid_x, grid_y from coordination_transit natural join ( select day_of_week, hour, area_name, (( select count(*) from future_position as B where A.day_of_week = B.day_of_week and A.hour = B.hour and A.count > B.count) + 1 ) as c_rank from future_position as A where c_rank = 1)";
        Cursor cursor = db.rawQuery(query, null);
        Log.d("ProjectSQL", String.format("cursor.getCount: %d", cursor.getCount()));
        LinkedList<QueryResult> list = new LinkedList<QueryResult>();
        QueryResult r = null;

        if(cursor.moveToFirst()) {
            while(cursor.moveToNext()){
                int v1 = cursor.getInt(0);
                int v2 = cursor.getInt(1);
                String v3 = cursor.getString(2);
                int v4 = cursor.getInt(3);
                int v5 = cursor.getInt(4);
                r = new QueryResult(v1, v2, v3, v4, v5);
                list.add(r);
            }
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




    class updata {
        public String date;
        public int hour;
        public int day;
        public String area;
        public int x,y;

        public void mod(String date, int hour, int day, String area, int x, int y) {
            this.date = date;
            this.hour = hour;
            this.day = day;
            this.area = area;
            this.x = x;
            this.y = y;
        }
    }

    public void createData() {
        updata data = new updata();
        data.mod("2015-05-17", 0, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 3, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 6, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 9, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 12, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 15, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 18, 1, "¼­¿ïÆ¯º°½Ã Áß¾Óµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-17", 21, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-18", 0, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-18", 3, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-18", 6, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-18", 9, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-18", 12, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-18", 15, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-18", 18, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-18", 21, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-19", 0, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-19", 3, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-19", 6, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-19", 9, 3, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-19", 12, 3, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-19", 15, 3, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-19", 18, 3, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-19", 21, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-20", 0, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-20", 3, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-20", 6, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-20", 9, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-20", 12, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-20", 15, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-20", 18, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-20", 21, 4, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 0, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 3, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 6, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 9, 5, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 12, 5, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-21", 15, 5, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-21", 18, 5, "¼­¿ïÆ¯º°½Ã ³«¼º´ëµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-21", 21, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 0, 6, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 3, 6, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 6, 6, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 9, 6, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 12, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-22", 15, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-22", 18, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-22", 21, 6, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-22", 24, 6, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-23", 3, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-23", 6, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-23", 9, 7, "¼­¿ïÆ¯º°½Ã Çà´çµ¿", 61, 127);
        createDatum(data);
        data.mod("2015-05-23", 12, 7, "¼­¿ïÆ¯º°½Ã »ç±Ùµ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-23", 15, 7, "¼­¿ïÆ¯º°½Ã È­¾çµ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-23", 18, 7, "¼­¿ïÆ¯º°½Ã ¼º¼ö2°¡3µ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-23", 21, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 0, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 3, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 6, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 9, 1, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 12, 1, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 15, 1, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 18, 1, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-24", 21, 1, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-25", 0, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-25", 3, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-25", 6, 2, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-25", 9, 2, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-25", 12, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-25", 15, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-25", 18, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-25", 21, 2, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-26", 0, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 3, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 6, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 9, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 12, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 15, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 18, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-26", 21, 3, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-27", 0, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-27", 3, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-27", 6, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-27", 9, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-27", 12, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-27", 15, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-27", 18, 4, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-27", 21, 4, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 0, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 3, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 6, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 9, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 12, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 15, 5, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 18, 5, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-28", 21, 5, "¼­¿ïÆ¯º°½Ã ³«¼º´ëµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-29", 0, 6, "¼­¿ïÆ¯º°½Ã ³«¼º´ëµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-29", 3, 6, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-29", 6, 6, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-29", 9, 6, "¼­¿ïÆ¯º°½Ã Ã»·æµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-29", 12, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-29", 15, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-29", 18, 6, "¼­¿ïÆ¯º°½Ã ´ëÇÐµ¿", 59, 124);
        createDatum(data);
        data.mod("2015-05-29", 21, 6, "¼­¿ïÆ¯º°½Ã ºÀÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-30", 0, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-30", 3, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-30", 6, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-30", 9, 7, "¼­¿ïÆ¯º°½Ã »ç±Ùµ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-30", 12, 7, "¼­¿ïÆ¯º°½Ã »ç±Ùµ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-30", 15, 7, "¼­¿ïÆ¯º°½Ã »ç±Ùµ¿", 61, 126);
        createDatum(data);
        data.mod("2015-05-30", 18, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
        data.mod("2015-05-30", 21, 7, "¼­¿ïÆ¯º°½Ã ÀºÃµµ¿", 59, 125);
        createDatum(data);
    }

//    public void createDatum(updata data) {
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
//                + data.date + " , "
//                + data.hour + ", "
//                + data.day + ", "
//                + data.area + ")");
//
//        String query = "INSERT OR REPLACE INTO %s (%s, %s, %s, %s) VALUES(%d, %d, %s, COALESCE((SELECT %s FROM %s WHERE %s = %d AND %s = %d and %s = '%s'), 1)";
//
//        db.execSQL(String.format(query, TABLE_NAME_FUTPOS,
//                KEY_DAY_OF_WEEK, KEY_HOUR, KEY_AREA, KEY_COUNT,
//                data.day, data.hour, data.area,
//                KEY_COUNT + "+1", TABLE_NAME_FUTPOS,
//                KEY_DAY_OF_WEEK, data.day, KEY_HOUR, data.hour, KEY_AREA, data.area));
//
//        String subquery = "SELECT %s FROM %s WHERE %s = '%s'";
//        String queryA = String.format(subquery, KEY_X, TABLE_NAME_COORD, KEY_AREA, data.area);
//        String queryB = String.format(subquery, KEY_Y, TABLE_NAME_COORD, KEY_AREA, data.area);
//        query = "INSERT OR REPLACE INTO %s (%s, %s, %s) VALUES('%s', COALESCE(%s, %d), COALESCE(%s, %d))";
//        db.execSQL(String.format(query,
//                TABLE_NAME_COORD, KEY_AREA, KEY_X, KEY_Y,
//                data.area, queryA, data.x, queryB, data.y));
//        db.close();
//    }
    public void createDatum(updata data) {
        SQLiteDatabase db = this.getWritableDatabase();

//        Calendar calendar = Calendar.getInstance();
//        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int hours = data.hour;
        String area = data.area;
        int x = data.x;
        int y = data.y;

        //round to next 3hrs
        switch (hours % 3) {
            case 0:
                break;
            case 1:
                hours += 2;
                break;
            case 2:
                hours += 1;
                break;
        }

//        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int day = data.day;
        String date = data.date;
        db.execSQL("INSERT INTO " + TABLE_NAME_USERPOS + " VALUES ("
                + date + ", "
                + hours + ", "
                + day + ", '"
                + area + "')");

        String query = "INSERT OR REPLACE INTO %s (%s, %s, %s, %s) VALUES(%d, %d, '%s', COALESCE((SELECT %s FROM %s WHERE %s = %d AND %s = %d and %s = '%s'), 1))";

        db.execSQL(String.format(query, TABLE_NAME_FUTPOS,
                KEY_DAY_OF_WEEK, KEY_HOUR, KEY_AREA, KEY_COUNT,
                day, hours, area,
                KEY_COUNT + "+1", TABLE_NAME_FUTPOS,
                KEY_DAY_OF_WEEK, day, KEY_HOUR, hours, KEY_AREA, area));

        String subquery = "SELECT %s FROM %s WHERE %s = '%s'";
        String queryA = String.format(subquery, KEY_X, TABLE_NAME_COORD, KEY_AREA, area);
        String queryB = String.format(subquery, KEY_Y, TABLE_NAME_COORD, KEY_AREA, area);
        query = "INSERT OR REPLACE INTO %s (%s, %s, %s) VALUES('%s', COALESCE((%s), %d), COALESCE((%s), %d))";
        db.execSQL(String.format(query,
                TABLE_NAME_COORD, KEY_AREA, KEY_X, KEY_Y,
                area, queryA, x, queryB, y));
        db.close();
    }
}
