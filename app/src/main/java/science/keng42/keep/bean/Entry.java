package science.keng42.keep.bean;

/**
 * Created by Keng on 2015/6/1
 */
public class Entry {

    private long id;
    private long date; // 13位时间戳
    private String title;
    private String text;
    private long folderId;
    private long locationId;
    private String tags; // 多个标签用 , 分隔
    private int archived; // 是否存档 0/1
    private int encrypted; // 是否加密 0/1

    public Entry(long id, long date, String title, String text, long folderId,
                 long locationId, String tags, int archived, int encrypted) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.text = text;
        this.folderId = folderId;
        this.locationId = locationId;
        this.tags = tags;
        this.archived = archived;
        this.encrypted = encrypted;
    }

    public Entry(long date, String title, String text, long folderId,
                  long locationId, String tags, int archived, int encrypted) {
        // id 自动生成
        this.date = date;
        this.title = title;
        this.text = text;
        this.folderId = folderId;
        this.locationId = locationId;
        this.tags = tags;
        this.archived = archived;
        this.encrypted = encrypted;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getArchived() {
        return archived;
    }

    public void setArchived(int archived) {
        this.archived = archived;
    }

    public int getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(int encrypted) {
        this.encrypted = encrypted;
    }
}
