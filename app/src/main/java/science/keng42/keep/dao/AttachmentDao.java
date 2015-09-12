package science.keng42.keep.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import science.keng42.keep.bean.Attachment;

/**
 * Created by Keng on 2015/6/1
 */
public class AttachmentDao {
    private static final String ID = "_id";
    private static final String ENTRY_ID = "entry_id";
    private static final String FILE_NAME = "filename";
    private static final String ATTACHMENTS = "attachments";

    private JKiSQLiteOpenHelper mHelper;

    public AttachmentDao(Context context) {
        this.mHelper = new JKiSQLiteOpenHelper(context);
    }

    public final void restore(Attachment attachment) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ID, attachment.getId());
        values.put(ENTRY_ID, attachment.getEntryId());
        values.put(FILE_NAME, attachment.getFilename());
        db.insert(ATTACHMENTS, null, values);
        db.close();
    }

    public final void insert(Attachment attachment) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ENTRY_ID, attachment.getEntryId());
        values.put(FILE_NAME, attachment.getFilename());

        long id = db.insert(ATTACHMENTS, null, values);
        attachment.setId(id);
        db.close();
    }

    public final int delete(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(ATTACHMENTS, "_id=?", new String[]{id + ""});

        db.close();
        return count;
    }

    public final int update(Attachment attachment) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ENTRY_ID, attachment.getEntryId());
        values.put(FILE_NAME, attachment.getFilename());

        int count = db.update(ATTACHMENTS, values, "_id=?", new String[]{
                attachment.getId() + ""});
        db.close();
        return count;
    }

    public final List<Attachment> queryAll() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ATTACHMENTS, null, null, null, null, null, "_id desc");
        List<Attachment> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            long entryId = c.getLong(c.getColumnIndex(ENTRY_ID));
            String fileName = c.getString(c.getColumnIndex(FILE_NAME));
            list.add(new Attachment(id, entryId, fileName));
        }
        c.close();
        db.close();
        return list;
    }

    public final Attachment query(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ATTACHMENTS, null, "_id=?", new String[]{id + ""},
                null, null, null);
        Attachment attachment = null;
        if (c.moveToNext()) {
            long entryId = c.getLong(c.getColumnIndex(ENTRY_ID));
            String fileName = c.getString(c.getColumnIndex(FILE_NAME));

            attachment = new Attachment(id, entryId, fileName);
        }
        c.close();
        db.close();
        return attachment;
    }

    /**
     * 查询条目的所有图片
     */
    public final List<Attachment> queryAllOfEntry(long entryId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor c = db.query(ATTACHMENTS, null, "entry_id=?", new String[]{entryId + ""}, null, null, "_id");
        List<Attachment> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex("_id"));
            String fileName = c.getString(c.getColumnIndex(FILE_NAME));
            list.add(new Attachment(id, entryId, fileName));
        }
        c.close();
        db.close();
        return list;
    }
}
