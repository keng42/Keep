package science.keng42.keep.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import science.keng42.keep.bean.Tag;

/**
 * Created by Keng on 2015/6/1
 */
public class TagDao {

    private static final String TITLE = "title";
    private static final String TAGS = "tags";
    private JKiSQLiteOpenHelper mHelper;

    public TagDao(Context context) {
        this.mHelper = new JKiSQLiteOpenHelper(context);
    }

    public void insert(Tag tag) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, tag.getTitle());

        long id = db.insert(TAGS, null, values);
        tag.setId(id);
        db.close();
    }

    public int delete(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(TAGS, "_id=?", new String[]{id + ""});

        db.close();
        return count;
    }

    public int update(Tag tag) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TITLE, tag.getTitle());

        int count = db.update(TAGS, values, "_id=?", new String[]{
                tag.getId() + ""});
        db.close();
        return count;
    }

    public List<Tag> queryAll() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(TAGS, null, null, null, null, null, TITLE);
        List<Tag> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            String title = c.getString(c.getColumnIndex(TITLE));

            list.add(new Tag(id, title));
        }
        c.close();
        db.close();
        return list;
    }

    public Tag query(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(TAGS, null, "_id=?", new String[]{id + ""},
                null, null, null);
        Tag tag = null;
        if (c.moveToNext()) {
            String title = c.getString(c.getColumnIndex(TITLE));
            tag = new Tag(id, title);
        }
        c.close();
        db.close();
        return tag;
    }

    /**
     * 获取该标签的条目数量
     */
    public int getEntriesCount(String tagTitle) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query("entries", null, "tags LIKE ?", new String[]{"%" + tagTitle + "%"},
                null, null, null);
        int count = 0;
        while (c.moveToNext()) {
            String str = c.getString(c.getColumnIndex(TAGS));
            str = "," + str + ",";
            if (str.contains("," + tagTitle + ",")) {
                count++;
            }
        }
        c.close();
        db.close();
        return count;
    }

    /**
     * 根据标题获取 ID
     */
    public long getIdByTitle(String title) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(TAGS, null, "title=? COLLATE NOCASE", new String[]{title},
                null, null, null);
        long id = 0;
        if (c.moveToNext()) {
            id = c.getLong(c.getColumnIndex("_id"));
        }
        c.close();
        db.close();
        return id;
    }

    public List<Tag> queryAllByKeyword(String keyword) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(TAGS, null, "title LIKE ?",
                new String[]{"%" + keyword + "%"}, null, null, null);
        List<Tag> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            String title = c.getString(c.getColumnIndex(TITLE));

            list.add(new Tag(id, title));
        }
        c.close();
        db.close();
        return list;
    }


}
