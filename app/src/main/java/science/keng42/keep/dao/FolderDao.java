package science.keng42.keep.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import science.keng42.keep.bean.Folder;

/**
 * Created by Keng on 2015/6/1
 */
public class FolderDao {

    private static final String TITLE = "title";
    private static final String COLOR = "color";
    private static final String FOLDERS = "folders";
    private JKiSQLiteOpenHelper mHelper;

    public FolderDao(Context context) {
        this.mHelper = new JKiSQLiteOpenHelper(context);
    }

    public void insert(Folder folder) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, folder.getTitle());
        values.put(COLOR, folder.getColor());

        long id = db.insert(FOLDERS, null, values);
        folder.setId(id);
        db.close();
    }

    public int delete(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(FOLDERS, "_id=?", new String[]{id + ""});

        db.close();
        return count;
    }

    public int update(Folder folder) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, folder.getTitle());
        values.put(COLOR, folder.getColor());

        int count = db.update(FOLDERS, values, "_id=?", new String[]{
                folder.getId() + ""});
        db.close();
        return count;
    }

    public List<Folder> queryAll() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(FOLDERS, null, null, null, null, null, "");
        List<Folder> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            String title = c.getString(c.getColumnIndex(TITLE));
            String color = c.getString(c.getColumnIndex(COLOR));
            list.add(new Folder(id, title, color));
        }
        c.close();
        db.close();
        return list;
    }

    public Folder query(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(FOLDERS, null, "_id=?", new String[]{id + ""},
                null, null, null);
        Folder folder = null;
        if (c.moveToNext()) {
            String title = c.getString(c.getColumnIndex(TITLE));
            String color = c.getString(c.getColumnIndex(COLOR));
            folder = new Folder(id, title, color);
        }
        c.close();
        db.close();
        return folder;
    }

    /**
     * 获取该文件夹的条目数量
     */
    public int getEntriesCount(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query("entries", null, "folder_id=?", new String[]{id + ""},
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
        Cursor c = db.query(FOLDERS, null, "title=?", new String[]{title},
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
