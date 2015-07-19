package science.keng42.keep.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import science.keng42.keep.bean.Location;

/**
 * Created by Keng on 2015/6/1
 */
public class LocationDao {

    private static final String TITLE = "title";
    private static final String ADDRESS = "address";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String DESCRIPTION = "description";
    private static final String LOCATIONS = "locations";
    private JKiSQLiteOpenHelper mHelper;

    public LocationDao(Context context) {
        this.mHelper = new JKiSQLiteOpenHelper(context);
    }

    public void insert(Location location) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, location.getTitle());
        values.put(ADDRESS, location.getAddress());
        values.put(LAT, location.getLat());
        values.put(LON, location.getLon());
        values.put(DESCRIPTION, location.getDescription());

        long id = db.insert(LOCATIONS, null, values);
        location.setId(id);
        db.close();
    }

    public int delete(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(LOCATIONS, "_id=?", new String[]{id + ""});

        db.close();
        return count;
    }

    public int update(Location location) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, location.getTitle());
        values.put(ADDRESS, location.getAddress());
        values.put(LAT, location.getLat());
        values.put(LON, location.getLon());
        values.put(DESCRIPTION, location.getDescription());

        int count = db.update(LOCATIONS, values, "_id=?", new String[]{
                location.getId() + ""});
        db.close();
        return count;
    }

    public List<Location> queryAll() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(LOCATIONS, null, null, null, null, null, TITLE);
        List<Location> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            String title = c.getString(c.getColumnIndex(TITLE));
            String address = c.getString(c.getColumnIndex(ADDRESS));
            String lat = c.getString(c.getColumnIndex(LAT));
            String lon = c.getString(c.getColumnIndex(LON));
            String description = c.getString(c.getColumnIndex(DESCRIPTION));

            list.add(new Location(id, title, address, description, lat, lon));
        }
        c.close();
        db.close();
        return list;
    }

    public Location query(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(LOCATIONS, null, "_id=?", new String[]{id + ""},
                null, null, null);
        Location location = null;
        if (c.moveToNext()) {
            String title = c.getString(c.getColumnIndex(TITLE));
            String address = c.getString(c.getColumnIndex(ADDRESS));
            String lat = c.getString(c.getColumnIndex(LAT));
            String lon = c.getString(c.getColumnIndex(LON));
            String description = c.getString(c.getColumnIndex(DESCRIPTION));

            location = new Location(id, title, address,description ,lat, lon );
        }
        c.close();
        db.close();
        return location;
    }

    /**
     * 获取该位置的条目数量
     */
    public int getEntriesCount(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query("entries", null, "location_id=?", new String[]{id + ""},
                null, null, null);
        int count = c.getCount();
        c.close();
        db.close();
        return count;
    }

    /**
     * 根据标题获取 ID
     */
    public long getIdByTitle(String title) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(LOCATIONS, null, "title=?", new String[]{title},
                null, null, null);
        long id = 0;
        if (c.moveToNext()) {
            id = c.getLong(c.getColumnIndex("_id"));
        }
        c.close();
        db.close();
        return id;
    }
}
