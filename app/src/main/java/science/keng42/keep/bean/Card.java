package science.keng42.keep.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Keng on 2015/7/17
 */
public class Card implements Parcelable {

    private long id;
    private long date; // 13位时间戳
    private String title;
    private String text;

    public Card(long id, long date, String title, String text) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.text = text;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.date);
        dest.writeString(this.title);
        dest.writeString(this.text);
    }

    protected Card(Parcel in) {
        this.id = in.readLong();
        this.date = in.readLong();
        this.title = in.readString();
        this.text = in.readString();
    }

    public static final Parcelable.Creator<Card> CREATOR = new Parcelable.Creator<Card>() {
        public Card createFromParcel(Parcel source) {
            return new Card(source);
        }

        public Card[] newArray(int size) {
            return new Card[size];
        }
    };
}
