/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 *
 * @author mfranco
 */
public class MapFileConfiguration {
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
			File f = new File(conf.substring(1));
			conf = null;
			if (f.canRead()) {
				try {
					conf = FileUtils.readFileToString(f);
				} catch (IOException ex) {
					return null;
				}
			}
		}

		if (conf == null || conf.length() == 0) {
			return null;
		}

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(File.class, new FileSerializer());
		Gson gson = gsonBuilder.create();
		Type listType = new TypeToken<ArrayList<MapFileConfiguration>>() {
		}.getType();
		List<MapFileConfiguration> out = gson.fromJson(conf, listType);
		return out;
	}
}

class FileSerializer implements JsonSerializer<File>, JsonDeserializer<File> {
	public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getAbsolutePath());
	}

	public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
		throws JsonParseException {
		File f = new File(json.getAsJsonPrimitive().getAsString());
		if (!f.canRead()) {
			return null;
		}
		return f;
	}
}
