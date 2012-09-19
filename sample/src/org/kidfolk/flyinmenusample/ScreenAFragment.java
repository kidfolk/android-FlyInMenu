package org.kidfolk.flyinmenusample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ScreenAFragment extends Fragment {

	public static ScreenAFragment newInstance(int position) {
		ScreenAFragment f = new ScreenAFragment();
		Bundle args = new Bundle();
		args.putInt("position", position);
		f.setArguments(args);
		return f;
	}

	private int position;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null)
			position = args.getInt("position");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_screen_a, container, false);
		TextView textView = (TextView) v.findViewById(R.id.textView);
		textView.append(": " + position);
		return v;
	}

}
