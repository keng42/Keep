package science.keng42.keep.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import science.keng42.keep.bean.Attachment;

/**
 * Created by Keng on 2015/5/30
 */
public class PhotoAdapter extends FragmentStatePagerAdapter {

    private List<Attachment> mPhotos;

    public PhotoAdapter(FragmentManager fm, List<Attachment> mPhotos) {
        super(fm);
        this.mPhotos = mPhotos;
    }

    @Override
    public Fragment getItem(int position) {
        return PhotoFragment.newInstance(mPhotos.get(position).getId());
    }

    @Override
    public int getCount() {
        return mPhotos.size();
    }
}
