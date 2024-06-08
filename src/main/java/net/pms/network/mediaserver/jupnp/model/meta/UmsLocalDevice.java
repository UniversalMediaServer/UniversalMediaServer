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
package net.pms.network.mediaserver.jupnp.model.meta;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.support.connectionmanager.UmsConnectionManagerService;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.network.mediaserver.jupnp.support.xmicrosoft.UmsMediaReceiverRegistrarService;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsLocalDevice extends LocalDevice {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsLocalDevice.class);
	private final DeviceDetailsProvider deviceDetailsProvider;
	private final UmsContentDirectoryService contentDirectoryService;
	private final UmsConnectionManagerService connectionManagerService;
	private final UmsMediaReceiverRegistrarService mediaReceiverRegistrarService;

	public UmsLocalDevice() throws ValidationException {
		super(
			new DeviceIdentity(new UDN(MediaServer.getUuid())),
			new UDADeviceType("MediaServer"),
			null,
			createDeviceIcons(),
			createMediaServerServices(),
			null
		);
		this.deviceDetailsProvider = new UmsDeviceDetailsProvider();
		this.contentDirectoryService = getServiceImplementation(UmsContentDirectoryService.class);
		this.connectionManagerService = getServiceImplementation(UmsConnectionManagerService.class);
		this.mediaReceiverRegistrarService = getServiceImplementation(UmsMediaReceiverRegistrarService.class);
	}

	@Override
	public DeviceDetails getDetails(RemoteClientInfo info) {
		if (deviceDetailsProvider != null) {
			return deviceDetailsProvider.provide(info);
		}
		return this.getDetails();
	}

	public UmsContentDirectoryService getContentDirectoryService() {
		return this.contentDirectoryService;
	}

	public UmsConnectionManagerService getConnectionManagerService() {
		return this.connectionManagerService;
	}

	public UmsMediaReceiverRegistrarService getMediaReceiverRegistrarService() {
		return this.mediaReceiverRegistrarService;
	}

	private <T> T getServiceImplementation(Class<T> baseClass) {
		for (LocalService service : getServices()) {
			if (service != null && service.getManager().getImplementation().getClass().equals(baseClass)) {
				return (T) service.getManager().getImplementation();
			}
		}
		return null;
	}

	/**
	 * Create the local UMS device
	 *
	 * @return the device
	 */
	public static UmsLocalDevice createMediaServerDevice() {
		try {
			return new UmsLocalDevice();
		} catch (ValidationException e) {
			LOGGER.debug("Error in upnp local device creation: {}", e);
			return null;
		}
	}

	private static Icon[] createDeviceIcons() {
		List<Icon> icons = new ArrayList<>();
		try {
			icons.add(new Icon("image/png", 256, 256, 24, "images/icon-square-256.png", getResourceInputStream("images/icon-square-256.png")));
			icons.add(new Icon("image/png", 128, 128, 24, "images/icon-square-128.png", getResourceInputStream("images/icon-square-128.png")));
			icons.add(new Icon("image/png", 120, 120, 24, "images/icon-square-120.png", getResourceInputStream("images/icon-square-120.png")));
			icons.add(new Icon("image/png", 48, 48, 24, "images/icon-square-48.png", getResourceInputStream("images/icon-square-48.png")));
			icons.add(new Icon("image/jpeg", 128, 128, 24, "images/icon-square-128.jpg", getResourceInputStream("images/icon-square-128.jpg")));
			icons.add(new Icon("image/jpeg", 120, 120, 24, "images/icon-square-120.jpg", getResourceInputStream("images/icon-square-120.jpg")));
			icons.add(new Icon("image/jpeg", 48, 48, 24, "images/icon-square-48.jpg", getResourceInputStream("images/icon-square-48.jpg")));
		} catch (IOException ex) {
			LOGGER.debug("Error in device icons creation: {}", ex);
		}
		return icons.toArray(Icon[]::new);
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null otherwise.
	 */
	private static InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replace("//", "/");
		ClassLoader cll = UmsLocalDevice.class.getClassLoader();
		InputStream is = cll.getResourceAsStream(fileName.substring(1));

		while (is == null && cll.getParent() != null) {
			cll = cll.getParent();
			is = cll.getResourceAsStream(fileName.substring(1));
		}

		return is;
	}

	/**
	 * Create the upnp services provided by UMS
	 *
	 * @return the media server services
	 */
	private static LocalService<?>[] createMediaServerServices() {
		List<LocalService<?>> services = new ArrayList<>();
		services.add(createContentDirectoryService());
		services.add(createServerConnectionManagerService());
		services.add(createMediaReceiverRegistrarService());
		return services.toArray(LocalService[]::new);
	}

	/**
	 * Creates the upnp ContentDirectoryService.
	 * @return The ContenDirectoryService.
	 */
	private static LocalService createContentDirectoryService() {
		LocalService contentDirectoryService = new AnnotationLocalServiceBinder().read(UmsContentDirectoryService.class);
		contentDirectoryService.setManager(new DefaultServiceManager<UmsContentDirectoryService>(contentDirectoryService, null) {

			@Override
			protected void lock() {
				//don't lock cds.
			}

			@Override
			protected void unlock() {
				//don't lock cds.
			}

			@Override
			protected UmsContentDirectoryService createServiceInstance() throws Exception {
				return new UmsContentDirectoryService();
			}
		});
		return contentDirectoryService;
	}

	/**
	 * creates the upnp ConnectionManagerService.
	 *
	 * @return the service
	 */
	private static LocalService createServerConnectionManagerService() {
		LocalService connectionManagerService = new AnnotationLocalServiceBinder().read(UmsConnectionManagerService.class);
		connectionManagerService.setManager(new DefaultServiceManager<UmsConnectionManagerService>(connectionManagerService, UmsConnectionManagerService.class) {

			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected UmsConnectionManagerService createServiceInstance() throws Exception {
				return new UmsConnectionManagerService();
			}
		});

		return connectionManagerService;
	}

	/**
	 * creates the upnp MediaReceiverRegistrarService.
	 *
	 * @return the service
	 */
	private static LocalService createMediaReceiverRegistrarService() {
		LocalService mediaReceiverRegistrarService = new AnnotationLocalServiceBinder().read(UmsMediaReceiverRegistrarService.class);
		mediaReceiverRegistrarService.setManager(new DefaultServiceManager<UmsMediaReceiverRegistrarService>(mediaReceiverRegistrarService, null) {
			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected UmsMediaReceiverRegistrarService createServiceInstance() throws Exception {
				return new UmsMediaReceiverRegistrarService();
			}
		});
		return mediaReceiverRegistrarService;
	}

}
