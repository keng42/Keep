package science.keng42.keep.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import science.keng42.keep.bean.Entry;

/**
 * Created by Keng on 2015/6/1
 */
public class EntryDao {
    private static final String DATE = "date";
    private static final String TITLE = "title";
    private static final String TEXT = "text";
    private static final String FOLDER_ID = "folder_id";
    private static final String LOCATION_ID = "location_id";
    private static final String TAGS = "tags";
    private static final String ARCHIVED = "archived";
    private static final String ENCRYPTED = "encrypted";
    private static final String ENTRIES = "entries";
    private static final String DATE_DESC = "date desc";
    private static final String ID = "_id";
    private JKiSQLiteOpenHelper mHelper;

    public EntryDao(Context context) {
        this.mHelper = new JKiSQLiteOpenHelper(context);
    }

    public void insert(Entry entry) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DATE, entry.getDate());
        values.put(TITLE, entry.getTitle());
        values.put(TEXT, entry.getText());
        values.put(FOLDER_ID, entry.getFolderId());
        values.put(LOCATION_ID, entry.getLocationId());
        values.put(TAGS, entry.getTags());
        values.put(ARCHIVED, entry.getArchived());
        values.put(ENCRYPTED, entry.getEncrypted());

        long id = db.insert(ENTRIES, null, values);
        entry.setId(id);
        db.close();
    }

    public int delete(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(ENTRIES, "_id=?", new String[]{id + ""});

        db.close();
        return count;
    }

    public int update(Entry entry) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DATE, entry.getDate());
        values.put(TITLE, entry.getTitle());
        values.put(TEXT, entry.getText());
        values.put(FOLDER_ID, entry.getFolderId());
        values.put(LOCATION_ID, entry.getLocationId());
        values.put(TAGS, entry.getTags());
        values.put(ARCHIVED, entry.getArchived());
        values.put(ENCRYPTED, entry.getEncrypted());

        int count = db.update(ENTRIES, values, "_id=?", new String[]{
                entry.getId() + ""});
        db.close();
        return count;
    }

    public List<Entry> queryAll() {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        Cursor c = db.query(ENTRIES, null, "archived=?", new String[]{"0"}, null, null, DATE_DESC);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    public List<Entry> queryArchive() {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        Cursor c = db.query(ENTRIES, null, "archived=?", new String[]{"1"}, null, null, DATE_DESC);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    public Entry query(Long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "_id=?", new String[]{id + ""},
                null, null, null);
        Entry entry = null;
        if (c.moveToNext()) {
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));

            entry = new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount);
        }
        c.close();
        db.close();
        return entry;
    }

    /**
     * 获取所有条目的数量
     */
    public int getEntriesCount() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, null, null, null, null, null);
        int count = c.getCount();
        c.close();
        db.close();
        return count;
    }

    /**
     * 获取该文件夹的条目
     */
    public List<Entry> getEntriesByFolder(long folderId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "folder_id=? and archived=?",
                new String[]{"" + folderId, "0"}, null, null, DATE_DESC);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    /**
     * 获取该标签的条目
     */
    public List<Entry> getEntriesByTags(String[] tagList) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("tags LIKE ?");
        for (int i = 1; i < tagList.length; i++) {
            sb.append(" and tags LIKE ?");
        }
        sb.append(" and archived=?");

        String[] tagList2 = new String[tagList.length + 1];
        for (int i = 0; i < tagList.length; i++) {
            tagList2[i] = "%" + tagList[i] + "%";
        }
        tagList2[tagList.length] = "0";

        Cursor c = db.query(ENTRIES, null, sb.toString(), tagList2,
                null, null, DATE_DESC);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    /**
     * 获取该标签的条目
     */
    public List<Entry> getEntriesByTag(String tag) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "tags LIKE ?", new String[]{"%" + tag + "%"},
                null, null, null);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    /**
     * 获取该文件夹的条目
     */
    public List<Entry> getEntriesByLocations(String[] locationsId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("location_id=?");
        for (int i = 1; i < locationsId.length; i++) {
            sb.append(" or location_id=?");
        }
        sb.append(" and archived=?");
        String[] locationsId2 = Arrays.copyOf(locationsId, locationsId.length + 1);
        locationsId2[locationsId.length] = "0";
        Cursor c = db.query(ENTRIES, null, sb.toString(), locationsId2,
                null, null, DATE_DESC);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
            long locationId = c.getLong(c.getColumnIndex(LOCATION_ID));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    /**
     * 获取该文件夹的条目
     */
    public List<Entry> getEntriesByLocation(long locationId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "location_id=?", new String[]{locationId + ""},
                null, null, null);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
//            long locationId = c.getLong(c.getColumnIndex("location_id"));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));

            list.add(new Entry(id, date, title, text, folderId, locationId,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }

    /**
     * 获取该文件夹的条目数量
     */
    public int getEntriesCountByFolder(long folderId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "folder_id=?",
                new String[]{"" + folderId}, null, null, null);
        int count = c.getCount();
        c.close();
        db.close();
        return count;
    }

    /**
     * 修改相关条目的标签
     * 标签被修改时调用
     */
    public int changeTitle(String oldTitle, String newTitle) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "tags LIKE ? COLLATE NOCASE", new String[]{"%" + oldTitle + "%"},
                null, null, null);
        int count = 0;
        while (c.moveToNext()) {
            String str = c.getString(c.getColumnIndex(TAGS));
            str = "," + str + ",";
            if (str.contains("," + oldTitle + ",")) {
                String tags = str.replace("," + oldTitle + ",", "," + newTitle + ",");
                Entry entry = query(c.getLong(c.getColumnIndex(ID)));
                entry.setTags(tags.substring(1, tags.length() - 1));
                update(entry);
                count++;
            }
        }
        c.close();
        db.close();
        return count;
    }

    /**
     * 删除相关条目的标签
     * 标签被删除时调用
     */
    public int deleteLabel(String oldTitle) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "tags LIKE ? COLLATE NOCASE", new String[]{"%" + oldTitle + "%"},
                null, null, null);
        int count = 0;
        while (c.moveToNext()) {
            String str = c.getString(c.getColumnIndex(TAGS));
            str = "," + str + ",";
            if (str.contains("," + oldTitle + ",")) {
                String tags = str.replace("," + oldTitle + ",", ",");
                Entry entry = query(c.getLong(c.getColumnIndex(ID)));
                String newTags = "";
                if (!tags.equals(",")) {
                    newTags = tags.substring(1, tags.length() - 1);
                }
                entry.setTags(newTags);
                update(entry);
                count++;
            }
        }
        c.close();
        db.close();
        return count;
    }

    /**
     * 获取该文件夹的条目
     * 设置所有相关的条目位置设为 无位置
     */
    public List<Entry> deleteLocationOfEntries(long locationId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ENTRIES, null, "location_id=?", new String[]{locationId + ""},
                null, null, null);
        List<Entry> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(ID));
            long date = c.getLong(c.getColumnIndex(DATE));
            String title = c.getString(c.getColumnIndex(TITLE));
            String text = c.getString(c.getColumnIndex(TEXT));
//            long locationId = c.getLong(c.getColumnIndex("location_id"));
            String tags = c.getString(c.getColumnIndex(TAGS));
            int tagCount = c.getInt(c.getColumnIndex(ARCHIVED));
            int photoCount = c.getInt(c.getColumnIndex(ENCRYPTED));
            long folderId = c.getLong(c.getColumnIndex(FOLDER_ID));

            update(new Entry(id, date, title, text, folderId, 1,
                    tags, tagCount, photoCount));
        }
        c.close();
        db.close();
        return list;
    }
}
