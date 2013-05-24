package radio.serveion.com;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TrackListAdapter extends BaseAdapter {

	private String[] list;
	private Context context;
	private LayoutInflater layout;
	
	public TrackListAdapter(Context ctx, String[] _list) {
		this.list = _list;
		this.context = ctx;
		this.layout = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	public int getCount() {
		// TODO Auto-generated method stub
		return this.list.length;
	}

	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getView(int item, View arg1, ViewGroup arg2) {
		View v = layout.inflate(R.layout.list_row, null);
		Log.w("TrackListAdapter", list[item]);
		String[] data = list[item].split("-");
		if(data.length > 1){
			((TextView) v.findViewById(R.id.artist_name)).setText(data[0].trim()); 
			((TextView) v.findViewById(R.id.song_title)).setText(data[1].trim());
		} else {
			((TextView) v.findViewById(R.id.artist_name)).setText(""); 
			((TextView) v.findViewById(R.id.song_title)).setText(list[item].trim());
		}
		((TextView) v.findViewById(R.id.row_num)).setText("" + (item + 1));
		return v;
	}

}
