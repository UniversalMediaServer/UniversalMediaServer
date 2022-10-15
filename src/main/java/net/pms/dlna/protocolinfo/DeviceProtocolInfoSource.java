/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna.protocolinfo;

import java.io.Serializable;

/**
 * This abstract class is used to identify the source of a given {@link ProtocolInfo}
 * instance stored in {@link DeviceProtocolInfo}.
 *
 * @param <T> The class responsible for parsing the given source.
 *
 * @author Nadahar
 */
public abstract class DeviceProtocolInfoSource<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * @return The {@link Class} responsible for parsing this
	 *         {@link DeviceProtocolInfoSource}.
	 */
	public abstract Class<T> getClazz();

	/**
	 * @return The {@link String} representation of the source.
	 */
	public abstract String getType();

	@Override
	public String toString() {
		return getType();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof DeviceProtocolInfoSource)) {
			return false;
		}
		DeviceProtocolInfoSource<?> other = (DeviceProtocolInfoSource<?>) object;
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().equals(other.getType())) {
			return false;
		}
		return true;
	}
}
