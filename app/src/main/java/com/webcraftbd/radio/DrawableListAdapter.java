package com.webcraftbd.radio;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DrawableListAdapter extends BaseAdapter {
	private final List<Integer> list;
	private final String[] stationName;

	public DrawableListAdapter(Context context) {
		list = new ArrayList<Integer>();
		stationName = context.getResources().getStringArray(
				R.array.station_names);

		Resources rs = context.getResources();
		for (int i = 1; i <= rs.getStringArray(R.array.station_names).length; i++) {
			int resID = rs.getIdentifier("station_" + i, "drawable",
					context.getPackageName());
			if (resID == 0)
				resID = rs.getIdentifier("station_default", "drawable",
						context.getPackageName());
			list.add(resID);
		}
	}

	@Override
	public int getCount() {
		if (list == null)
			return 0;
		return list.size();
	}

	@Override
	public Object getItem(int arg0) {
		return list.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		try {
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(parent.getContext());
				convertView = inflater.inflate(R.layout.stations_list_item, parent,
						false);
			}
	
			Integer data = list.get(position);
	
			TextView nameView = (TextView) convertView.findViewById(R.id.title);
			nameView.setText(stationName[position]);
			ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
			try {
				imageView.setImageResource(data);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return convertView;
	}
}
