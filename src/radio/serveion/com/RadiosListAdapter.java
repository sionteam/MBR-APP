package radio.serveion.com;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class RadiosListAdapter extends BaseAdapter {

	private Context context = null;
	private ArrayList<Object> radios_list = null;
	private int galleryViewWidth, galleryViewHeight;
	private LayoutInflater vi;

	public RadiosListAdapter(Context c) {
		this.context = c;
		if(this.IsSmallScreen()){
			this.galleryViewWidth = 156;
			this.galleryViewHeight = 150;
		}
		else {
			this.galleryViewWidth = 260;
			this.galleryViewHeight = 250;
		}
		vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LoadRadiosList();
	}

	private boolean IsSmallScreen() {
		int width, height, smaller;
		Display display = ((WindowManager) this.context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		height = display.getHeight();
		width = display.getWidth();
		smaller = Math.min(width, height);
		if (smaller >= 400)
			return false;
		else
			return true;
	}
	
	public int getCount() {
		if (radios_list == null)
			return 0;
		else
			return radios_list.size();
	}
//	//@Override
//	public int getCount() {
//		if (radios_list == null)
//			return 0;
//		else
//			return radios_list.size();
//	}

	////@Override
	public Object getItem(int index) {
		if (radios_list == null)
			return null;
		else
			return radios_list.get(index);
	}

	////@Override
	public long getItemId(int arg0) {
		return 0;
	}

	//@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Log.e("RadiosListAdapter", "Position: " + position);
		return (View)radios_list.get(position);
	}

	

	private Bitmap MakeRoundedCorners(Bitmap _bm) {
		Bitmap output = Bitmap.createBitmap(_bm.getWidth(), _bm.getHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, _bm.getWidth(), _bm.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = 15.0f;//pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(_bm, rect, rect, paint);

		return output;

	}

	private void LoadRadiosList() {
		boolean has_more = false;
		radios_list = new ArrayList<Object>();
		
		
		DbHelper dbh = new DbHelper(this.context);
		Cursor c = dbh.GetRadiosList();
		String val;
		has_more = c.moveToFirst();
		while (has_more) {
			View v = vi.inflate(R.layout.radios_list_row, null);
			val = c.getString(c.getColumnIndex("name"));
//			Log.w("RadiosListAdapter", val);
			v.setTag(val);
			((TextView) v.findViewById(R.id.radio_name)).setText(val);
			val = this.context.getExternalFilesDir("resources") + c.getString(c.getColumnIndex("logo"));
			((ImageView) v.findViewById(R.id.radio_avatar))
					.setImageBitmap(BitmapFactory
							.decodeFile(val));
			val = c.getString(c.getColumnIndex("genre")) + " [" + c.getString(c.getColumnIndex("lang")) + "]";
			((TextView) v.findViewById(R.id.radio_description)).setText(val);
			radios_list.add(v);
			has_more = c.moveToNext();
		}
		dbh.close();
	}


}
