package science.keng42.keep.bean;

/**
 * Created by Keng on 2015/6/1
 */
public class Location {

    private long id;
    private String title;
    private String address;
    private String description;
    private String lat;
    private String lon;

    public Location(long id, String title, String address, String description, String lat, String lon) {
        this.id = id;
        this.title = title;
        this.address = address;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }

    public Location(String title, String address, String description,
                    String lat, String lon) {
        this.title = title;
        this.address = address;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }
}
