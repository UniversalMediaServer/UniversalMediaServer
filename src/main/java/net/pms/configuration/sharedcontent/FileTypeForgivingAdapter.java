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
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.File;
import java.lang.reflect.Type;

/**
 * Based on FileTypeAdapter but more forgiving: does not check for file existence or permissions.
 */
public class FileTypeForgivingAdapter implements JsonSerializer<File>, JsonDeserializer<File> {
	@Override
	public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getAbsolutePath());
	}

	@Override
	public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		File file = new File(json.getAsJsonPrimitive().getAsString());

		return file;
	}
}
