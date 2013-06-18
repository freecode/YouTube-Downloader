import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author l3eta
 * @version 1.0
 */

public class YouTube {
	/*
    Does not always download, retrying after a couple mins or 30 seconds sometimes helps
     */

	/**
	 * Used to prevent bad urls such as 403, 400 exceptions
	 */
	public static final ArrayList<String> BAD_KEYS = new ArrayList<String>();
	//public static final HashMap<Integer, String> FILE_TYPES = new HashMap<Integer, String>();

	static {
		BAD_KEYS.add("stereo3d");
		BAD_KEYS.add("type");
		BAD_KEYS.add("fallback_host");
		BAD_KEYS.add("quality");

		//These are here in case we ever add in different stream types.
        /*FILE_TYPES.put(36, "3GP_240p");
        FILE_TYPES.put(13, "3GP_144p");
        FILE_TYPES.put(17, "3GP_144p");
        FILE_TYPES.put(5, "FLV_240p");
        FILE_TYPES.put(6, "FLV_270p");
        FILE_TYPES.put(34, "FLV_360p");
        FILE_TYPES.put(35, "FLV_480p");
        FILE_TYPES.put(18, "MP4_(Max 480p)");
        FILE_TYPES.put(22, "MP4_720p");
        FILE_TYPES.put(37, "MP4_1080p");
        FILE_TYPES.put(160, "MP4_144p (Video Only)");
        FILE_TYPES.put(133, "MP4_240p (Video Only)");
        FILE_TYPES.put(134, "MP4_360p (Video Only)");
        FILE_TYPES.put(135, "MP4_480p (Video Only)");
        FILE_TYPES.put(136, "MP4_720p (Video Only)");
        FILE_TYPES.put(137, "MP4_1080p (Video Only)");
        FILE_TYPES.put(139, "M4A_48Kbps (Video Only)");
        FILE_TYPES.put(140, "M4A_128kbps (Audio Only)");
        FILE_TYPES.put(141, "M4A_256kbps (Audio Only)");
        FILE_TYPES.put(38, "MP4");
        FILE_TYPES.put(83, "MP4_3D 240p");
        FILE_TYPES.put(82, "MP4_3D 360p");
        FILE_TYPES.put(85, "MP4_3D 520p");
        FILE_TYPES.put(84, "MP4_3D 720p");
        FILE_TYPES.put(43, "WEBM_360p");
        FILE_TYPES.put(44, "WEBM_480p");
        FILE_TYPES.put(45, "WEBM_720p");
        FILE_TYPES.put(46, "WEBM_1080p");
        FILE_TYPES.put(100, "WEBM_3D 360p");
        FILE_TYPES.put(101, "WEBM_3D 360p");
        FILE_TYPES.put(102, "WEBM_3D 720p");*/
	}

	public static void main(String[] args) {
		downloadUrl("http://m.youtube.com/watch?v=dvmlXsBzxb8");
	}

	public static String STORAGE = "C:/Users/l3eta/Desktop/";
	/**
	 * Tested: 35, 5
	 * Working: 5
	 *
	 */
	private static int TARGET_QUALITY = 5;
	private static Progress progress = new Progress() { //debugging
		public void update(int current) {
			System.out.println(current + "/" + total);
		}
	};

	public static void setProgress(Progress progress) {
		YouTube.progress = progress;
	}

	public static void downloadUrl(String url) {
		int i = url.indexOf("v=") + 2;
		int x = url.indexOf("&", i);
		if (x < 0)
			x = url.length();
		download(url.substring(i, x));
	}

	public static void download(String id) {
		String[] flvUrls = getFLVFormatUrls(getURLS(id));
		if (flvUrls != null) {
			boolean downloaded = false;
			for (String flv : flvUrls) {
				int tag = getTag(flv);
				if (tag == TARGET_QUALITY) {//Download only target to avoid issues
					downloaded = downloadVideo(flv);
				}
				if (downloaded)
					break;
			}
		}
	}

	private static int getTag(String url) {
		int i;
		return Integer.parseInt(url.substring(i = url.indexOf("itag=") + 5, url.indexOf('&', i)));
	}

	private static String[] getFLVFormatUrls(String[] urls) {
		if(urls == null)
			return null;
		ArrayList<String> u = new ArrayList<String>();
		for (String url : urls) {
			int tag = getTag(url);
			if (tag == 35 || tag == 34 || tag == 6 || tag == 5) {
				u.add(url);
			}
		}
		Collections.sort(u, new Comparator<String>() {//my first attempt at actually using this class lolol
			public int compare(String url, String url1) {
				int tag = getTag(url), tag1 = getTag(url1);
				if (tag < tag1)
					return 1;
				if (tag > tag1)
					return -1;
				return 0;
			}
		});
		return u.toArray(new String[u.size()]);
	}

	public static InputStream getVideoInfo(String id) {
		try {
			HttpURLConnection connection = con("http://www.youtube.com/get_video_info?video_id=" + id + "&asv=3&el=detailpage&hl=en_US");
			connection.setRequestMethod("GET");
			return connection.getInputStream();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private static String[] getURLS(String id) {
		try {
			InputStream is = getVideoInfo(id);
			if(is == null) {
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			LinkedList<String> urls = new LinkedList<String>();
			String title = null;
			for (String l; (l = reader.readLine()) != null; ) {
				for (String p : l.split("&")) {
					String key = p.substring(0, p.indexOf('='));
					String value = p.substring(p.indexOf('=') + 1);
					if (key.equals("url_encoded_fmt_stream_map")) {
						value = decode(value);
						for (String u : value.split("url=")) {
							u = getCorrectURL(decode(u));
							if (!u.startsWith("http") && !isValid(u)) {
								continue;
							}
							urls.add(u);
						}
					} else if (key.equals("title")) {
						title = value.replace("+", "%20");
					}
				}
			}
			if (title == null) {
				throw new RuntimeException("Failed to find title can't complete url");
			}
			String[] url_map = urls.toArray(new String[urls.size()]);
			for (int i = 0; i < url_map.length; i++) {
				url_map[i] += url_map[i].endsWith("&") ? "title=" + title : "&title=" + title;
			}
			return url_map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean isValid(String url) {//kinda pointless lol
		return url.contains("signature=") && url.contains("factor=");
	}

	private static HttpURLConnection con(String url) throws Exception {
		URL u = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) u.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.3");
		return connection;
	}

	private static String getCorrectURL(String input) {
		StringBuilder builder = new StringBuilder(input.substring(0, input.indexOf('?') + 1));
		String[] params = input.substring(input.indexOf('?') + 1).split("&");
		LinkedList<String> keys = new LinkedList<String>();
		boolean first = true;
		for (String param : params) {
			String key = param;
			try {
				key = param.substring(0, param.indexOf('='));
			} catch (Exception ex) {
				System.out.println(param);
			}
			if (keys.contains(key) || BAD_KEYS.contains(key)) {
				continue;
			}
			keys.add(key);
			if (key.equals("sig")) {
				builder.append(first ? "" : "&").append("signature=").append(param.substring(4));
			} else {
				if (param.contains(",quality=")) {
					param = remove(param, ",quality=", "_end_");
				}
				if (param.contains(",type=")) {
					param = remove(param, ",type=", "_end_");
				}
				if (param.contains(",fallback_host")) {
					param = remove(param, ",fallback_host", ".com");
				}
				builder.append(first ? "" : "&").append(param);
			}
			if (first)
				first = false;
		}
		return builder.toString();
	}

	private static String remove(String text, String start, String end) {
		int l = text.indexOf(start);
		return text.replace(text.substring(l, end.equals("_end_") ? text.length() : text.indexOf(end, l)), "");
	}

	public static boolean downloadVideo(String url) {
		System.out.println("Trying: " + url);
		FlvAudioInputStream input = null;
		FileOutputStream output = null;
		try {
			String file = getSaveFilePath(url.substring(url.lastIndexOf('&') + 7));
			if (file != null) {
				output = new FileOutputStream(file);
				HttpURLConnection con = con(url);
				con.connect();
				if (progress != null)
					progress.setTotal(con.getContentLength());
				input = new FlvAudioInputStream(con.getInputStream());
				byte[] buffer = new byte[8192];
				int len;
				while ((len = input.read(buffer, 0, 8192)) > 0) {
					output.write(buffer, 0, len);
					if (progress != null)
						progress.update((int) input.getOffset());
				}
				return true;
			}
		} catch (Exception ex) {
			handleException(ex);
			return false;
		} finally {
			try {
				if (input != null)
					input.close();
				if (output != null)
					output.close();
			} catch (IOException ex) {
				handleException(ex);
			}
		}
		return false;
	}

	private static void handleException(Exception ex) {
		System.out.println(ex);
	}

	public static String getSaveFilePath(String name) {//TODO remove invalid chars from file name
		return STORAGE + "/" + decode(name).replace("?", "") + ".mp3";
	}

	public static String decode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return s;
	}

	public abstract static class Progress {
		protected int total;

		public void setTotal(int total) {
			this.total = total;
		}

		public double getSizeInMB(int len) {
			return len / 1024.0 / 1024.0;
		}

		public double getTotalMB() {
			return getSizeInMB(total);
		}

		public abstract void update(int current);
	}
}