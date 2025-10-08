package net.pms.renderers.devices;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;

public class ControlPoint extends Renderer {

	static private ControlPoint cPoint = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(ControlPoint.class.getName());

	private ControlPoint() throws ConfigurationException, InterruptedException {
		super(null, null);
	}

	static public ControlPoint getRenderer() {
		return cPoint;
	}

	@Override
	public String getUUID() {
		return "22a40c51-f735-4b62-a376-ab9450fcf1e2";
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean isControllable() {
		return true;
	}

	@Override
	public boolean isControllable(int type) {
		return true;
	}

	@Override
	public String getRendererName() {
		return "ControlPoint";
	}

	/**
	 * Control points do not play content.
	 */
	@Override
	public boolean isCompatible(StoreItem resource, Format format) {
		return true;
	}

	static {
		try {
			cPoint = new ControlPoint();
		} catch (ConfigurationException | InterruptedException e) {
			LOGGER.error("ControlPoint", e);
		}
	}
}
