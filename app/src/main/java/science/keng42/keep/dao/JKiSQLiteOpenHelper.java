package science.keng42.keep.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Keng on 2015/5/8
 */
public class JKiSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String PRIMARY_KEY = "_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,";

    public JKiSQLiteOpenHelper(Context context) {
        super(context, "jki.db", null, 1);
    }

    // 数据库第一次被创建时调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        String entries = "CREATE TABLE entries (" +
                PRIMARY_KEY +
                "date LONG DEFAULT 0 NOT NULL," +
                "title TEXT DEFAULT '' NOT NULL," +
                "text TEXT DEFAULT '' NOT NULL," +
                "folder_id TEXT DEFAULT '' NOT NULL," +
                "location_id TEXT DEFAULT '' NOT NULL," +
                "tags TEXT DEFAULT '' NOT NULL," +
                "archived INTEGER DEFAULT 0 NOT NULL," +
                "encrypted INTEGER DEFAULT 0 NOT NULL" +
                ");";

        String locations = "CREATE TABLE locations (" +
                PRIMARY_KEY +
                "title TEXT DEFAULT '' NOT NULL," +
                "address TEXT DEFAULT '' NOT NULL," +
                "lat TEXT DEFAULT '' NOT NULL," +
                "lon TEXT DEFAULT '' NOT NULL," +
                "description TEXT DEFAULT '' NOT NULL" +
                ");";

        String folders = "CREATE TABLE folders (" +
                PRIMARY_KEY +
                "title TEXT DEFAULT '' NOT NULL," +
                "color TEXT DEFAULT '' NOT NULL" +
                ");";

        String tags = "CREATE TABLE tags (" +
                PRIMARY_KEY +
                "title TEXT DEFAULT '' NOT NULL" +
                ");";

        String attachments = "CREATE TABLE attachments (" +
                PRIMARY_KEY +
                "entry_id INTEGER DEFAULT 1 NOT NULL," +
                "filename TEXT DEFAULT ''NOT NULL" +
                ");";

        db.execSQL(entries);
        db.execSQL(folders);
        db.execSQL(locations);
        db.execSQL(tags);
        db.execSQL(attachments);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
