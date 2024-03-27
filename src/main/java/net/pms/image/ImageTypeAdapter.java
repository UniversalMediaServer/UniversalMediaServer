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
package net.pms.image;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Base64;

public class ImageTypeAdapter implements JsonSerializer<Image> {

	@Override
	public JsonElement serialize(Image src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder("data:image/");
		if (src.getFormat() != null) {
			sb.append(src.getFormat().toFormatConfiguration());
		} else {
			sb.append("jpeg");
		}
		sb.append(";base64,");
		sb.append(Base64.getEncoder().encodeToString(src.getBytes(false)));
		return new JsonPrimitive(sb.toString());
	}

}
