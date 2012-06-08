package org.kidfolk.flyinmenu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FlyInMenuActivity extends Activity {
	private RootView mRootView;
	private Button mToggle;
	private static final String TAG = "FlyInMenuActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(new NavigationItemAdapter(this
				.getApplicationContext()));

		mRootView = (RootView) findViewById(R.id.root);
		mToggle = (Button) findViewById(R.id.toggle);
		mToggle.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mRootView.animateToggle();
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode:"+keyCode);
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return super.dispatchTouchEvent(ev);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}

	class ImageAdapter extends BaseAdapter {

		private Context mContext;

		// references to our images
		private Integer[] mThumbIds = { R.drawable.sample_2,
				R.drawable.sample_3, R.drawable.sample_4, R.drawable.sample_5,
				R.drawable.sample_6, R.drawable.sample_7, R.drawable.sample_0,
				R.drawable.sample_1, R.drawable.sample_2, R.drawable.sample_3,
				R.drawable.sample_4, R.drawable.sample_5, R.drawable.sample_6,
				R.drawable.sample_7, R.drawable.sample_0, R.drawable.sample_1,
				R.drawable.sample_2, R.drawable.sample_3, R.drawable.sample_4,
				R.drawable.sample_5, R.drawable.sample_6, R.drawable.sample_7 };

		public ImageAdapter(Context context) {
			this.mContext = context;
		}

		@Override
		public int getCount() {
			return mThumbIds.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(8, 8, 8, 8);
			} else {
				imageView = (ImageView) convertView;
			}
			imageView.setImageResource(mThumbIds[position]);
			return imageView;
		}

	}

	private class NavigationItemAdapter extends BaseAdapter {

		NavigationItem[] items = { new NavigationItem("社区", "精选.活动.人物"),
				new NavigationItem("社区", "社区小组"),
				new NavigationItem("小明", "新鲜事"),
				new NavigationItem("小明", "我的箱子"),
				new NavigationItem("小明", "我的朋友"),
				new NavigationItem("小明", "消息"), new NavigationItem("小明", "主页"),
				new NavigationItem("小明", "收藏"),
				new NavigationItem("我的小组", "hello"),
				new NavigationItem("更多操作", "搜索"),
				new NavigationItem("更多操作", "设置"),
				new NavigationItem("更多操作", "休眠") };

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