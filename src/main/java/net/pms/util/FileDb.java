package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDb {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDb.class);
	private static final String NULLOBJ_STR = "@@@NULLOBJ@@@";
	private Map<String, Object> db;
	private int minCnt;
	private String separator;
	private String encodedSeparator;
	private File file;
	private DbHandler handler;
	private boolean autoSync;
	private boolean overwrite;
	private boolean useNullObj;
	private Object nullObj;
	private boolean hasNulls;

	public FileDb(DbHandler h) {
		this(PMS.getConfiguration().getDataFile(h.name()), h);
	}

	public FileDb(String f, DbHandler h) {
		if (StringUtils.isEmpty(f)) {
			f = "UMS.db";
		}
		file = new File(f);
		handler = h;
		minCnt = 2;
		separator = ",";
		encodedSeparator = "&comma;";
		autoSync = true;
		overwrite = false;
		useNullObj = false;
		db = new HashMap<>();
		nullObj = new Object();
		hasNulls = false;
	}

	public void setSep(String separator, String encodedSeparator) {
		if (separator == null || encodedSeparator == null) {
			throw new IllegalArgumentException("Neither argument can be null");
		}
		this.separator = separator;
		this.encodedSeparator = encodedSeparator;
	}

	public void setMinCnt(int c) {
		minCnt = c;
	}

	public void setAutoSync(boolean b) {
		autoSync = b;
	}

	public void setOverwrite(boolean b) {
		overwrite = b;
	}

	public void setUseNullObj(boolean b) { useNullObj = b; }

	public Object nullObj() { return nullObj; }

	public boolean isNull(Object obj) { return ((obj == null) || (obj == nullObj)); }

	public boolean hasNulls() { return hasNulls; }

	public Set<String> keys() {
		return db.keySet();
	}

	public Iterator iterator() {
		return db.entrySet().iterator();
	}

	private String recode(String str) {
		return Pattern.compile(encodedSeparator, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).
				matcher(str).replaceAll(Matcher.quoteReplacement(separator));
	}

	public void init() {
		if (!file.exists()) {
			return;
		}
		hasNulls = false;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (StringUtils.isEmpty(line) || line.startsWith("#")) {
					continue;
				}
				if (useNullObj) {
					String re = ".*" + separator + NULLOBJ_STR + "$";
					if (line.matches(re)) {
						// we got a line which is key, NULL
						// translate to nullobj
						hasNulls = true;
						String[] key = Pattern.compile(separator, Pattern.LITERAL).split(line);
						db.put(recode(key[0]), nullObj);
						continue;
					}
				}
				String[] entry = Pattern.compile(separator, Pattern.LITERAL).split(line);
				if (entry.length < minCnt) {
					continue;
				}
				// Substitute the encoded separator with the separator
				for (int i = 0; i < entry.length; i++) {
					if (entry[i] != null) {
						entry[i] = recode(entry[i]);
					}
				}
				db.put(entry[0], handler.create(entry));
			}
		} catch (IOException e) {
			LOGGER.warn("Could not read file database file \"{}\": {}", file.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public void addNoSync(String key, Object obj) {
		if (!overwrite) {
			if (get(key) != null) {
				return;
			}
		}
		db.put(key, obj);
		hasNulls |= isNull(obj);
	}

	public void removeNoSync(String key) {
		db.remove(key);
	}

	public void add(String key, Object obj) {
		addNoSync(key, obj);
		if (autoSync) {
			sync();
		}
	}

	public void remove(String key) {
		db.remove(key);
		if (autoSync) {
			sync();
		}
	}

	public Object get(String key) {
		return db.get(key);
	}

	public void sync() {
		try (FileOutputStream out = new FileOutputStream(file)) {
			// Write a dummy line to make sure the file exists
			Date now = new Date();
			StringBuilder data = new StringBuilder();
			data.append("#########################\n#### Db file generated ").append(now.toString()).append("\n")
				.append("#### Edit with care\n#########################\n");
			out.write(data.toString().getBytes(StandardCharsets.UTF_8));
			hasNulls = false;
			for (Entry<String, Object> entry : db.entrySet()) {
				Object obj = entry.getValue();
				data = new StringBuilder(Pattern.compile(separator, Pattern.LITERAL).
										 matcher(entry.getKey()).
										 replaceAll(Matcher.quoteReplacement(encodedSeparator)));
				if (isNull(obj)) {
					hasNulls = true;
					if (useNullObj) {
						data.append(separator);
						data.append(NULLOBJ_STR);
					} else {
						for (int i = 1; i < minCnt; i++) {
							data.append(separator);
						}
					}
					data.append("\n");
				} else {
					String[] data1 = handler.format(obj);

					// Substitute the separator with the encoded separator
					for (int i = 0; i < data1.length; i++) {
						data1[i] = Pattern.compile(separator, Pattern.LITERAL).matcher(data1[i]).replaceAll(Matcher.quoteReplacement(encodedSeparator));
					}
					data.append(separator).append(StringUtils.join(data1, separator)).append("\n");
				}
				out.write(data.toString().getBytes(StandardCharsets.UTF_8));
			}
			out.flush();
		} catch (IOException e) {
			LOGGER.warn("Could not write file database file \"{}\": {}", file.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static String safeGetArg(String[] args, int i) {
		return (i >= args.length ? "" : args[i]);
	}
}
