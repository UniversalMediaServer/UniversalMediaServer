/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.configuration;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.pms.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mfranco
 */
public class MapFileConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapFileConfiguration.class);
	private String name;
	private String thumbnailIcon;
	private List<MapFileConfiguration> children;
	private List<File> files;

	public String getName() {
		return name;
	}

	public String getThumbnailIcon() {
		return thumbnailIcon;
	}

	public List<MapFileConfiguration> getChildren() {
		return children;
	}

	public List<File> getFiles() {
		return files;
	}

	public void setName(String n) {
		name = n;
	}

	public void setThumbnailIcon(String t) {
		thumbnailIcon = t;
	}

	public void setFiles(List<File> f) {
		files = f;
	}

	public MapFileConfiguration() {
		children = new ArrayList<MapFileConfiguration>();
		files = new ArrayList<File>();
	}

	public static List<MapFileConfiguration> parse(String conf) {
		if (conf != null && conf.startsWith("@")) {
			File file = new File(conf.substring(1));
			conf = null;

			if (FileUtil.isFileReadable(file)) {
				try {
					conf = FileUtils.readFileToString(file);
				} catch (IOException ex) {
					return null;
				}
			} else {
				LOGGER.warn("Can't read {}", file.getAbsolutePath());
			}
		}

		if (conf == null || conf.length() == 0) {
			return null;
		}

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(File.class, new FileSerializer());
		Gson gson = gsonBuilder.create();
		Type listType = (new TypeToken<ArrayList<MapFileConfiguration>>() { }).getType();
		List<MapFileConfiguration> out = gson.fromJson(conf, listType);
		return out;
	}
}

class FileSerializer implements JsonSerializer<File>, JsonDeserializer<File> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileSerializer.class);

	public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getAbsolutePath());
	}

	public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		File file = new File(json.getAsJsonPrimitive().getAsString());

		if (!FileUtil.isFileReadable(file)) {
			LOGGER.warn("Can't read {}", file.getAbsolutePath());
			return null;
		} else {
			return file;
		}
	}
}
