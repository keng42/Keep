package science.keng42.keep.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import science.keng42.keep.fragment.SDCardFragment;

/**
 * Created by Keng on 2015/9/11
 */
public class SDCardFragmentPagerAdapter extends FragmentPagerAdapter {

    private final int PAGE_COUNT = 2;
    private String mTabTitles[] = new String[]{"SD card", "Dropbox"};
    private SDCardFragment[] fragments = new SDCardFragment[PAGE_COUNT];

    public SDCardFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (fragments[position] == null) {
            fragments[position] = SDCardFragment.newInstance(position);
        }
        return fragments[position];
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }
}
