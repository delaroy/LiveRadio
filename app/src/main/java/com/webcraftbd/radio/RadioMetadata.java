/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RadioMetadata {
	protected URL streamUrl;
	private Map<String, String> metadata;
	private boolean isError;
	private Map<String, String> data;

	public RadioMetadata() {
		isError = false;
	}

	/**
	 * Get artist using stream's title
	 * 
	 * @return String
	 * @throws IOException
	 */
	public String getArtist() throws IOException {
		data = getMetadata();

		if (data == null)
			return "";

		if (!data.containsKey("StreamTitle"))
			return "";

		try {
			String streamTitle = data.get("StreamTitle");
			int end = streamTitle.indexOf("-");
			if (end <= 0)
				end = streamTitle.indexOf(":");

			String title;
			if (end > 0)
				title = streamTitle.substring(0, end);
			else
				title = streamTitle;
			return title.trim();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Get streamTitle
	 * 
	 * @return String
	 * @throws IOException
	 */
	public String getStreamTitle() throws IOException {
		data = getMetadata();

		if (!data.containsKey("StreamTitle"))
			return "";

		return data.get("StreamTitle");
	}

	/**
	 * Get title using stream's title
	 * 
	 * @return String
	 * @throws IOException
	 */
	public String getTitle() throws IOException {
		data = getMetadata();

		if (data == null)
			return "";

		if (!data.containsKey("StreamTitle"))
			return "";

		try {

			String streamTitle = data.get("StreamTitle");
			int start = streamTitle.indexOf("-") + 1;
			if (start <= 0)
				start = streamTitle.indexOf(":") + 1;

			String track;
			if (start > 0)
				track = streamTitle.substring(start);
			else
				track = streamTitle;

			int end = streamTitle.indexOf("(");
			if (end > 0)
				track = streamTitle.substring(start, end);

			end = streamTitle.indexOf("[");
			if (end > 0)
				track = streamTitle.substring(start, end);

			return track.trim();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public Map<String, String> getMetadata() throws IOException {
		if (metadata == null) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						refreshMeta();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			thread.start();
		}

		return metadata;
	}

	synchronized public void refreshMeta() throws IOException {
		retreiveMetadata();
	}

	synchronized private void retreiveMetadata() throws IOException {
		URLConnection con = streamUrl.openConnection();
		con.setRequestProperty("Icy-MetaData", "1");
		con.setRequestProperty("Connection", "close");
		con.setRequestProperty("Accept", null);
		con.connect();
		int metaDataOffset = 0;
		Map<String, List<String>> headers = con.getHeaderFields();
		InputStream stream = con.getInputStream();

		try {
			if (headers.containsKey("icy-metaint")) {
				// Headers are sent via HTTP
				metaDataOffset = Integer.parseInt(headers.get("icy-metaint")
						.get(0));
			} else {
				// Headers are sent within a stream
				StringBuilder strHeaders = new StringBuilder();
				char c;
				try {
					while ((c = (char) stream.read()) != -1) {
						try {
							strHeaders.append(c);
						} catch (OutOfMemoryError e) {
							e.printStackTrace();
							isError = true;
							return;
						}
						if (strHeaders.length() > 5
								&& (strHeaders.substring(
										(strHeaders.length() - 4),
										strHeaders.length()).equals("\r\n\r\n"))) {
							// end of headers
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					isError = true;
					return;
				}

				// Match headers to get metadata offset within a stream
				Pattern p = Pattern
						.compile("\\r\\n(icy-metaint):\\s*(.*)\\r\\n");
				Matcher m = p.matcher(strHeaders.toString());
				if (m.find()) {
					metaDataOffset = Integer.parseInt(m.group(2));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			isError = true;
			return;
		}

		// In case no data was sent
		if (metaDataOffset == 0) {
			isError = true;
			return;
		}

		// Read metadata
		int b;
		int count = 0;
		int metaDataLength = 4080; // 4080 is the max length
		boolean inData = false;
		StringBuilder metaData = new StringBuilder();
		// Stream position should be either at the beginning or right after
		// headers
		while ((b = stream.read()) != -1) {
			count++;

			// Length of the metadata
			if (count == metaDataOffset + 1) {
				metaDataLength = b * 16;
			}

			if (count > metaDataOffset + 1
					&& count < (metaDataOffset + metaDataLength)) {
				inData = true;
			} else {
				inData = false;
			}
			if (inData) {
				if (b != 0) {
					metaData.append((char) b);
				}
			}
			if (count > (metaDataOffset + metaDataLength)) {
				break;
			}
		}

		// Set the data
		metadata = RadioMetadata.parseMetadata(metaData.toString());

		// Close
		stream.close();

	}

	public boolean isError() {
		return isError;
	}

	public URL getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(URL streamUrl) {
		this.metadata = null;
		this.streamUrl = streamUrl;
		this.isError = false;
		this.data = null;
	}

	public static Map<String, String> parseMetadata(String metaString) {
		Map<String, String> metadata = new HashMap<String, String>();
		String[] metaParts = metaString.split(";");
		Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'([^\\']*)\\'$");
		Matcher m;
		for (int i = 0; i < metaParts.length; i++) {
			m = p.matcher(metaParts[i]);
			if (m.find()) {
				metadata.put(m.group(1), m.group(2));
			}
		}

		return metadata;
	}
}
