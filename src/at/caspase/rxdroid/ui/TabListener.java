package at.caspase.rxdroid.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/*
 * Copied from http://developer.android.com/reference/android/app/ActionBar.html#newTab%28%29
 */
public class TabListener<T extends Fragment> implements ActionBar.TabListener
{
	private final SherlockFragmentActivity mActivity;
	private final String mTag;
	private final Class<T> mClass;
	private final Bundle mArgs;
	private Fragment mFragment;

	public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz) {
		this(activity, tag, clz, null);
	}

	public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args)
	{
		mActivity = activity;
		mTag = tag;
		mClass = clz;
		mArgs = args;

		// Check to see if we already have a fragment for this tab, probably
		// from a previously saved state.  If so, deactivate it, because our
		// initial state is that a tab isn't shown.
		mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
		if(mFragment != null && !mFragment.isDetached())
		{
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
			ft.detach(mFragment);
			ft.commit();
		}
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if(mFragment == null)
		{
			mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
			ft.add(android.R.id.content, mFragment, mTag);
		}
		else
			ft.attach(mFragment);
	}

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        if(mFragment != null)
            ft.detach(mFragment);
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {

    }
}

