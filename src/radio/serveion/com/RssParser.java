package radio.serveion.com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

public class RssParser {

	public String xmlParsing(String rssFeed, Context c) {
		String filename = null;
		String skeleton = null;
		URL url = null;
		try {
			url = new URL(rssFeed);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Log.d("I got Exception", "3");
			e.printStackTrace();
		}
		skeleton = GetSkeleton(c, "skeleton.html");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
			Log.d("I m Here", "4");
		}
		Document doc = null;
		try {
			doc = db.parse(new InputSource(url.openStream()));
		} catch (SAXException e2) {
			// TODO Auto-generated catch block
			Log.d("I m Here", "5");
			e2.printStackTrace();
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			Log.d("I m Here", "6");
			e3.printStackTrace();
		}
		
		// Check if needed files exists
		String assetName = "jquery.js";
		File f = new File(c.getExternalFilesDir("rss"), assetName);
		if(!f.exists())
			CopyFile(assetName, f, c);
		assetName = "rssreader.css";
		f = new File(c.getExternalFilesDir("rss"), assetName);
		if(!f.exists())
			CopyFile(assetName, f, c);
		assetName = "rssreader.js";
		f = new File(c.getExternalFilesDir("rss"), assetName);
		if(!f.exists())
			CopyFile(assetName, f, c);
		
		org.w3c.dom.Element elt = doc.getDocumentElement();
		NodeList nodeList = elt.getElementsByTagName("item");
		OutputStreamWriter out;
		try {
			out = new OutputStreamWriter(c.openFileOutput("feeds.html",
					Context.MODE_WORLD_READABLE));
			File file = new File(c.getExternalFilesDir("rss"), "feeds.html");
			filename = "file://" + file.getAbsolutePath();
			Log.e("RSS_FEEDS Filename", filename);
			FileOutputStream out1 = new FileOutputStream(file);
			String[] htmlBlocks = skeleton.split("__FEED_ITEMS__");
			out.write(htmlBlocks[0]);
			out1.write(htmlBlocks[0].getBytes());
			String element = "";
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				Node title = ((Element) node).getElementsByTagName("title")
						.item(0);
				Node description = ((Element) node).getElementsByTagName(
						"description").item(0);
				Node link = ((Element) node).getElementsByTagName("link").item(
						0);
				element = "<li><div style=\"display: table-cell;\"><b>"
						+ title.getTextContent()
						+ "</b><br><p style=\"display: hidden;\">"
						+ description.getTextContent()
						+ "</p><p style=\"cursor: pointer;\" onclick=\"window.open('"
						+ link.getTextContent()
						+ "', '_blanck');\">READ MORE...</p></div></li>";
				out.write(element);
				out1.write(element.getBytes());
			 Log.e("ELEMENT", element);
			}
			out.write(htmlBlocks[1]);
			out1.write(htmlBlocks[1].getBytes());
			out.flush();
			out1.flush();
			out.close();
			out1.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.e("Saved html", GetSkeleton(c, "feeds.html"));
		return (filename);

	}

	private void CopyFile(String assetFileName, File sdFile, Context c) {
		String str = "";
		InputStreamReader reader;
		try {
			FileOutputStream out = new FileOutputStream(sdFile);
			reader = new InputStreamReader(c.getAssets().open(assetFileName));

			BufferedReader br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null) {
				out.write(line.getBytes());
			}
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String GetSkeleton(Context c, String fname) {
		String str = "";
		InputStreamReader reader;
		try {
			reader = new InputStreamReader(c.getAssets().open(fname));

			BufferedReader br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null) {
				str += line;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}

}
