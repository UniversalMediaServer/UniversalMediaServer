/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.util.jna;

import com.sun.jna.FromNativeContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.TypeConverter;


/**
 * Performs conversion between {@link JnaIntEnum} and native enums.
 *
 * @author Nadahar
 */
public class JnaIntEnumConverter implements TypeConverter {

	@Override
	public Object fromNative(Object input, FromNativeContext context) {
		if (!JnaIntEnum.class.isAssignableFrom(context.getTargetType())) {
			throw new IllegalStateException("JnaIntEnumConverter can only convert objects implementing JnaIntEnum");
		}
		@SuppressWarnings("rawtypes")
		Class targetClass = context.getTargetType();
		Object[] enumValues = targetClass.getEnumConstants();
		return ((JnaIntEnum<?>) enumValues[0]).typeForValue((int) input);
	}

	@Override
	public Class<Integer> nativeType() {
		return Integer.class;
	}

	@Override
	public Integer toNative(Object input, ToNativeContext context) {
		return ((JnaIntEnum<?>) input).getValue();
	}
}
