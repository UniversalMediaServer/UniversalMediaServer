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
