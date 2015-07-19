package science.keng42.keep.bean;

/**
 * Created by Keng on 2015/6/1
 */
public class Attachment {

    private long id;
    private long entryId;
    private String filename;

    public Attachment(long id, long entryId, String filename) {
        this.id = id;
        this.entryId = entryId;
        this.filename = filename;
    }

    public Attachment(long entryId, String filename) {
        this.entryId = entryId;
        this.filename = filename;
    }

    public final long getId() {
        return id;
    }

    public final void setId(long id) {
        this.id = id;
    }

    public final long getEntryId() {
        return entryId;
    }

    public final void setEntryId(long entryId) {
        this.entryId = entryId;
    }

    public final String getFilename() {
        return filename;
    }

    public final void setFilename(String filename) {
        this.filename = filename;
    }
}
