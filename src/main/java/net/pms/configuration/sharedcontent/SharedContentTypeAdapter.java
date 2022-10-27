/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.configuration.sharedcontent;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentTypeAdapter implements JsonSerializer<SharedContent>, JsonDeserializer<SharedContent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentConfiguration.class);

	@Override
	public JsonElement serialize(SharedContent src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = context.serialize(src).getAsJsonObject();
		result.addProperty("type", src.getClass().getSimpleName());
		return result;
	}

	@Override
	public SharedContent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json.isJsonObject()) {
			JsonObject sharedContent = json.getAsJsonObject();
			if (sharedContent.has("type") && sharedContent.get("type").isJsonPrimitive()) {
				String type = sharedContent.remove("type").getAsString();
				try {
					return switch (type) {
						case Folder.TYPE -> context.deserialize(sharedContent, Folder.class);
						case Folders.TYPE -> context.deserialize(sharedContent, Folders.class);
						case FeedAudio.TYPE -> context.deserialize(sharedContent, FeedAudio.class);
						case FeedImage.TYPE -> context.deserialize(sharedContent, FeedImage.class);
						case FeedVideo.TYPE -> context.deserialize(sharedContent, FeedVideo.class);
						case StreamAudio.TYPE -> context.deserialize(sharedContent, StreamAudio.class);
						case StreamVideo.TYPE -> context.deserialize(sharedContent, StreamVideo.class);
						default -> null;
					};
				} catch (JsonParseException e) {
					LOGGER.warn("Exception parsing: {}", e.getMessage());
					return null;
				}
			}
		}
		return null;
	}

}
