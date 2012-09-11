package org.kidfolk.flyinmenusample;

import org.kidfolk.flyinmenu.RootView;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FlyinMenuSampleActivity extends FragmentActivity {
	private RootView mRootView;
	private static final String TAG = "FlyInMenuActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		getSupportFragmentManager().beginTransaction()
				.add(R.id.host, new HostFragment()).commit();

		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(new NavigationItemAdapter(this
				.getApplicationContext()));

		mRootView = (RootView) findViewById(R.id.root);
	}

	RootView getRootView() {
		return mRootView;
	}

	private class NavigationItemAdapter extends BaseAdapter {

		NavigationItem[] items = { new NavigationItem("section1", "title1"),
				new NavigationItem("section1", "title2"),
				new NavigationItem("section2", "title1"),
				new NavigationItem("section2", "title2"),
				new NavigationItem("section2", "title3"),
				new NavigationItem("section2", "title4"),
				new NavigationItem("section2", "title5"),
				new NavigationItem("setcion3", "title1"),
				new NavigationItem("section4", "title1"),
				new NavigationItem("section4", "title2"),
				new NavigationItem("section4", "title3") };

		class NavigationItem {
			String group;
			String content;

			public NavigationItem(String group, String content) {
				this.group = group;
				this.content = content;
			}
		}

		private static final int STATE_UNKNOW = 0;
		private static final int STATE_SECTIONED_CELL = 1;
		private static final int STATE_REGULAR_CELL = 2;
		private Context mContext;
		private int[] mCellStates = new int[items.length];

		public NavigationItemAdapter(Context context) {
			this.mContext = context;
		}

		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public Object getItem(int position) {
			return items[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = LayoutInflater.from(mContext).inflate(
					R.layout.menu_list_item, parent, false);
			TextView separator = (TextView) view.findViewById(R.id.separator);
			TextView naviItem = (TextView) view.findViewById(R.id.navi_item);
			boolean needSeparator = false;
			switch (mCellStates[position]) {
			case STATE_SECTIONED_CELL:
				needSeparator = true;
				break;
			case STATE_REGULAR_CELL:
				needSeparator = false;
				break;
			case STATE_UNKNOW:
			default:
				if (position == 0) {
					needSeparator = true;
				} else {
					if (!items[position - 1].group
							.equals(items[position].group)) {
						needSeparator = true;
					}
				}

				mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL
						: STATE_REGULAR_CELL;
				break;
			}

			if (needSeparator) {
				separator.setText(items[position].group);
				separator.setVisibility(View.VISIBLE);
			} else {
				separator.setVisibility(View.GONE);
			}

			naviItem.setText(items[position].content);

			return view;
		}

	}
}