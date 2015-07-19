package science.keng42.keep.bean;

/**
 * Created by Keng on 2015/6/1
 */
public class Folder {

    private long id;
    private String title;
    private String color; // ffffffff

    public Folder(long id, String title, String color) {
        this.id = id;
        this.title = title;
        this.color = color;
    }

    public Folder(String title, String color) {
        this.title = title;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
