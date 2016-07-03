package com.webcraftbd.radio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.util.Log;

public class PlsParser {
	private BufferedReader reader;

	public PlsParser(String url) {
		URLConnection urlConnection = null;
		try {
			urlConnection = new URL(url).openConnection();
			this.reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getUrls() {
		ArrayList<String> urls = new ArrayList<String>();
		while (true) {
			try {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				String url = parseLine(line);
				if (url != null && !url.equals("")) {
					urls.add(url);
				}
			} catch (IOException e) {
				return null;
				// e.printStackTrace();
			} catch (Exception e) {
				return null;
			}
		}
		return urls;
	}

	private String parseLine(String line) {
		if (line == null) {
			return null;
		}
		String trimmed = line.trim();
		if (trimmed.indexOf("http") >= 0) {
			String url = trimmed.substring(trimmed.indexOf("http"));
			int end = url.indexOf("<");
			if (end > 0)
				url = url.substring(0, end);

			Log.d("PlsParser", "Playlist Url = " + url);
			return url;
		}
		return "";
	}
}
