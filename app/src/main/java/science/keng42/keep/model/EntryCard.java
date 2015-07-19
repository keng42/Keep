package science.keng42.keep.model;

import java.util.List;

/**
 * 用于直接填充卡片的数据包
 * 一般由 Entry 对象转换而来
 * Created by Keng on 2015/6/3
 */
public class EntryCard {

    // 条目 ID
    private long id;
    private String title;
    private String text;
    // 卡片颜色
    private int color;
    // 图片名和标签名的集合
    private List<String> names;
    private List<String> labels;

    public EntryCard(long id, String title, String text, int color,
                     List<String> names, List<String> labels) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.color = color;
        this.names = names;
        this.labels = labels;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
