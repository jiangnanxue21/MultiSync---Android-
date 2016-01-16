package SyncMain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

public class Common {
	public static String HOME_PATH = "";
	public static final String CONFIG_FILENAME = ".syncsetting";
	public static final String INDEX_FILENAME = "index";
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String CONFIG_HOST = "http://file-sync.oicp.net:9000";
	public static final long RESERVE_SPACE = (long) 1048576;
	public static final long SPILIT_SIZE = (long) 10;
	public static final int RETRY_TIMES = 10;
	public static final int BUFFER_SIZE = 4096;
	
	public static String[] SERVICES = {
		"dropbox",
		"kuaipan",
		"baidu",
		"gdrive",
		"skydrive",
		"dbank",
		"box"
	};
	
	public static void Init(String home) {
		HOME_PATH = home;
		Arrays.sort(SERVICES);
	}
	
	public static void getConfig(Map<String, String> param_table) throws IOException {
		File config_file = new File(Common.HOME_PATH + Common.CONFIG_FILENAME);
		BufferedReader fin = new BufferedReader(new FileReader(config_file));
		
		String paramstr;
		while ((paramstr = fin.readLine()) != null) {
			int pos = paramstr.indexOf('=');
			if (pos != -1) {
				String key = paramstr.substring(0, pos);
				String value;
				if (pos >= paramstr.length() - 1) {
					value = "";
				} else {
					value = paramstr.substring(pos + 1);
				}
				param_table.put(key, value);
			}
		}
		fin.close();
	}
	
	public static void putConfig(Map<String, String> param_table) throws IOException {
		File config_file = new File(Common.HOME_PATH + Common.CONFIG_FILENAME);
		File parent = config_file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		BufferedWriter fout = new BufferedWriter(new FileWriter(config_file));
		
		for (Entry<String, String> entry : param_table.entrySet()) {
			fout.write(entry.getKey() + "=" + entry.getValue() + "\n");
		}
		fout.close();
	}
}
