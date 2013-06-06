package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import net.pms.util.FileUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;


public class PrettyNameConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(PrettyNameConfig.class);

	private String conf;
	private String expr;
	private String repl;
	private boolean stop;
	
	public PrettyNameConfig() {
		stop = false;
	}
	
	public String getConf() {
		return conf;
	}
	
	public String getExpr() {
		return expr;
	}
	
	public String getRepl() {
		return repl;
	}
	
	public boolean isBad() {
		return StringUtils.isEmpty(expr) &&
			StringUtils.isEmpty(repl);
	}
	
	public boolean stop() {
		return stop;
	}
	
	public String toString() {
		return "conf=" + conf + ",expr=" + expr + ",repl=" + repl + ",stop=" + stop;
	}
	
	public static List<PrettyNameConfig> parse(String conf) {
		if (conf != null) {
			File file = new File(conf);
			conf = null;

			if (FileUtil.isFileReadable(file)) {
				try {
					conf = FileUtils.readFileToString(file);
				} catch (IOException ex) {
					return null;
				}
			} else {
				LOGGER.warn("Can't read file: {}", file.getAbsolutePath());
			}
		}

		if (conf == null || conf.length() == 0) {
			return null;
		}
		

		GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
		Type listType = (new TypeToken<ArrayList<PrettyNameConfig>>() { }).getType();
		List<PrettyNameConfig> out = gson.fromJson(conf, listType);
		return out;
	}
}

