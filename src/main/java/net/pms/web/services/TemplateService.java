package net.pms.web.services;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;

import net.pms.util.FileWatcher;

@Singleton
public class TemplateService {
	private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);

	private Map<String, Template> templates = new ConcurrentHashMap<>();
	
	private Compiler compiler = Mustache.compiler().escapeHTML(false);

	/**
	 * Automatic recompiling
	 */
	FileWatcher.Listener recompiler = new FileWatcher.Listener() {
		@Override
		public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir) {
			String path = watch.fspec.startsWith("/resources/templates/") ? watch.fspec.substring(4) : watch.fspec;
			if (templates.containsKey(path)) {
				templates.put(path, compile(getAsStream(path)));
				LOGGER.info("Recompiling template: {}", path);
			}
		}
	};

	public Template getTemplate(String name) {
		Template template = templates.get(name);
		if (template != null) {
			return template;
		}
		template = compile(getAsStream(name));
		templates.put(name, template);
		return template;
	}
	
	private InputStream getAsStream(String name) {
		InputStream in = getClass().getResourceAsStream("/resources/templates/" + name);
		if (in == null) {
			LOGGER.warn("Unable to locate template {}", name);
			throw new NotFoundException("Unable to locate template with name: " + name);
		}
		return in;
	}

	private Template compile(InputStream stream) {
		try {
			return compiler.compile(new InputStreamReader(stream));
		} catch (Exception e) {
			LOGGER.debug("Error compiling mustache template: " + e);
		}
		return null;
	}
}
