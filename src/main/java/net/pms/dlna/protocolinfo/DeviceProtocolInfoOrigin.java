package net.pms.dlna.protocolinfo;

import java.io.Serializable;

/**
 * This abstract class is used to identify the origin of a given
 * {@link ProtocolInfo} instance stored in {@link DeviceProtocolInfo}.
 *
 * @param <T> The class responsible for parsing the given origin.
 *
 * @author Nadahar
 */
public abstract class DeviceProtocolInfoOrigin<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * @return The {@link Class} responsible for parsing this
	 *         {@link DeviceProtocolInfoOrigin}.
	 */
	public abstract Class<T> getClazz();

	/**
	 * @return The {@link String} representation of the origin.
	 */
	public abstract String getOrigin();

	/**
	 * @return The {@link ProtocolInfoType} for this instance.
	 */
	public abstract ProtocolInfoType getType();

	@Override
	public String toString() {
		return "DeviceProtocolInfoOrigin: [Origin: " + getOrigin() + ", Type: " + getType() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getOrigin() == null) ? 0 : getOrigin().hashCode());
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
		if (!(object instanceof DeviceProtocolInfoOrigin)) {
			return false;
		}
		DeviceProtocolInfoOrigin<?> other = (DeviceProtocolInfoOrigin<?>) object;
		if (getOrigin() == null) {
			if (other.getOrigin() != null) {
				return false;
			}
		} else if (!getOrigin().equals(other.getOrigin())) {
			return false;
		}
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().equals(other.getType())) {
			return false;
		}
		return true;
	}

	/**
	 * This {@code enum} represents the abstract terms {@link #SINK} and
	 * {@link #SOURCE} which in this context means consume/render/play and serve
	 * respectively.
	 */
	public enum ProtocolInfoType {

		/** The consumer; the receiving end of a media transfer i.e. the rendering capabilities */
		SINK,

		/** The provider; the serving end of a media transfer i.e the serving capabilities */
		SOURCE
	}
}
