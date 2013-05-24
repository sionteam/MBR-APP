package radio.serveion.com;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SlideMenuAdapter extends ArrayAdapter {

	private Context context;
	private ArrayList<SlideMenuItem> items;
	private LayoutInflater vi;

	public SlideMenuAdapter(Context context, String radio_name) {
		super(context, 0);
		Log.w("Slide Menu Adapter", "Radio Name: " + radio_name);

		this.context = context;
		vi = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// CreateApplicationMenu();
		if (items == null)
			items = new ArrayList<SlideMenuItem>();
		if (radio_name != "" && radio_name != null)
			CreateRadioMenu(radio_name);
	}

	@Override
	public int getCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SlideMenuItem i = items.get(position);
		View v;
		if (i.getType() == SlideMenuItem.TYPE.SECTION) {
			v = vi.inflate(R.layout.slide_menu_section, null);
			((TextView) v.findViewById(R.id.section_title)).setText(i
					.getTitle());
		} else {
			v = vi.inflate(R.layout.slide_menu_option, null);
			((TextView) v.findViewById(R.id.option_title))
					.setText(i.getTitle());
		}
		v.setTag(i);
		return v;
	}

	public void CreateRadioMenu(String radio_name) {
		Log.w("Slide Menu Adapter", "Building radio menu options..."
				+ radio_name);
		// int sindx = -1;
		// for(int i = 0; i < items.size(); i++){
		// if(items.get(i).title.equals("Radio")){
		// sindx = i;
		// break;
		// }
		// }
		//
		// if(sindx != -1){
		// int l = items.size();
		// items.subList(sindx, l-sindx-1).clear();
		// }

		items = new ArrayList<SlideMenuAdapter.SlideMenuItem>();

		items.add(new SlideMenuItem(SlideMenuItem.TYPE.SECTION, "Radio", null));
		DbHelper dbh = new DbHelper(context);
		Cursor c = dbh.GetRadioData(radio_name);
		c.moveToFirst();
		// "stream", "mail", "about", "logo", "wurl", "phone", "rating",
		// "rfbook"
		String opt;
		opt = c.getString(c.getColumnIndex("mail"));
		if (opt != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Contact Us",
					opt));
		opt = c.getString(c.getColumnIndex("wurl"));
		if (opt != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY,
					"Our Website", opt));
		opt = c.getString(c.getColumnIndex("rfbook"));
		if (opt != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
					opt));
		opt = c.getString(c.getColumnIndex("about"));
		if (opt != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "About "
					+ radio_name, opt));
		dbh.close();
		AddRssFeeds();
	}

	private void AddRssFeeds() {
		SharedPreferences sp = context.getSharedPreferences("mypreferences",
				Context.MODE_PRIVATE);
		String[] names = sp.getString("feed_name", "~~~~~~~~~").split("~~~");
		String[] urls = sp.getString("feed_urls", "~~~~~~~~~").split("~~~");
		boolean hasSection = false;
		for (int i = 0; i < names.length; i++) {
			Log.e("RSS FEEDS", names[i] + " ==> " + urls[i]);
			if (names[i] != "" && urls[i] != "") {
				if (!hasSection) {
					items.add(new SlideMenuItem(SlideMenuItem.TYPE.SECTION,
							"RSS Feeds", null));
					hasSection = true;
				}
				items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, names[i],
						"rssfeed://" + urls[i]));

			}
		}

	}

	private void CreateApplicationMenu() {
		Log.w("Slide Menu Adapter", "Building application menu options...");
		String aux = "";
		if (items == null)
			items = new ArrayList<SlideMenuItem>();
		items.add(new SlideMenuItem(SlideMenuItem.TYPE.SECTION, "Application",
				null));
		SharedPreferences sp = this.context.getSharedPreferences(
				"mypreferences", Context.MODE_PRIVATE);
		aux = sp.getString("app_mail", "");
		if (aux != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Contact Us",
					aux));
		aux = sp.getString("app_fbook", "");
		if (aux != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
					aux));
		aux = sp.getString("app_twitter", "");
		if (aux != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Twitter",
					aux));
		aux = sp.getString("app_phone", "");
		if (aux != "")
			items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Call Us",
					aux));
		// aux = sp.getString("admob_id", "");
		// if(aux != "")
		// items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
		// aux));
		// aux = sp.getString("show_admob", "");
		// if(aux != "")
		// items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
		// aux));
		// aux = sp.getString("bkg_color", "");
		// if(aux != "")
		// items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
		// aux));
		// aux = sp.getString("bkg_image", "");
		// if(aux != "")
		// items.add(new SlideMenuItem(SlideMenuItem.TYPE.ENTRY, "Facebook",
		// aux));

	}

	public static class SlideMenuItem {

		public enum TYPE {
			SECTION, ENTRY
		};

		private TYPE type;
		private String title;
		private String extra;

		public SlideMenuItem(TYPE type, String title, String extra) {
			this.type = type;
			this.title = title;
			this.extra = extra;
		}

		public TYPE getType() {
			return this.type;
		}

		public String getTitle() {
			return this.title;
		}

		public String getExtra() {
			return this.extra;
		}
	}
}
