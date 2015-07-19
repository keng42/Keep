package science.keng42.keep.bean;

/**
 * Created by Keng on 2015/6/1
 */
public class Tag {

    private long id;
    private String title;

    public Tag(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Tag(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
