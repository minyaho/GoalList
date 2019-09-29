package yuntech.goalteam.goallist.Adapter;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class MyFragmentAdapter extends FragmentStatePagerAdapter {
    ArrayList<Fragment> mFragments;
    ArrayList<String> mTitle;

    public MyFragmentAdapter(FragmentManager fm, ArrayList<Fragment> mFragments, ArrayList<String> mTitle) {
        super(fm);
        this.mFragments = mFragments;
        this.mTitle = mTitle;
    }

    @Override
    public Fragment getItem(int i) {
        return mFragments.get(i);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mTitle.get(position);
    }
}
