/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.network.mediaserver.jupnp.model.meta;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.network.mediaserver.jupnp.support.connectionmanager.UmsConnectionManagerService;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.Ums2ContentDirectoryService;
import net.pms.network.mediaserver.jupnp.support.xmicrosoft.Ums2MediaReceiverRegistrarService;
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
	final private DeviceDetailsProvider deviceDetailsProvider;

	public UmsLocalDevice() throws ValidationException {
		super(
			new DeviceIdentity(new UDN(PMS.get().udn())),
			new UDADeviceType("MediaServer"),
			null,
			createDeviceIcons(),
			createMediaServerServices(),
			null
		);
		this.deviceDetailsProvider = new UmsDeviceDetailsProvider();
	}

	@Override
	public DeviceDetails getDetails(RemoteClientInfo info) {
		if (deviceDetailsProvider != null) {
			return deviceDetailsProvider.provide(info);
		}
		return this.getDetails();
	}

	/**
	 * Create the local UMS device
	 *
	 * @return the device
	 */
	public static LocalDevice createMediaServerDevice() {
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
			icons.add(new Icon("image/png", 256, 256, 24, "images/icon-256.png", getResourceInputStream("images/icon-256.png")));
			icons.add(new Icon("image/png", 128, 128, 24, "images/icon-128.png", getResourceInputStream("images/icon-128.png")));
			icons.add(new Icon("image/png", 120, 120, 24, "images/icon-120.png", getResourceInputStream("images/icon-120.png")));
			icons.add(new Icon("image/png", 48, 48, 24, "images/icon-48.png", getResourceInputStream("images/icon-48.png")));
			icons.add(new Icon("image/jpeg", 128, 128, 24, "images/icon-128.jpg", getResourceInputStream("images/icon-128.jpg")));
			icons.add(new Icon("image/jpeg", 120, 120, 24, "images/icon-120.jpg", getResourceInputStream("images/icon-120.jpg")));
			icons.add(new Icon("image/jpeg", 48, 48, 24, "images/icon-48.jpg", getResourceInputStream("images/icon-48.jpg")));
		} catch (IOException ex) {
			LOGGER.debug("Error in device icons creation: {}", ex);
		}
		return icons.toArray(new Icon[0]);
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null otherwise.
	 */
	private static InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replaceAll("//", "/");
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
		return services.toArray(new LocalService[] {});
	}

	/**
	 * Creates the upnp ContentDirectoryService.
	 * @return The ContenDirectoryService.
	 */
	private static LocalService<Ums2ContentDirectoryService> createContentDirectoryService() {
		LocalService<Ums2ContentDirectoryService> contentDirectoryService = new AnnotationLocalServiceBinder().read(Ums2ContentDirectoryService.class);
		contentDirectoryService.setManager(new DefaultServiceManager<Ums2ContentDirectoryService>(contentDirectoryService, null) {

			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected Ums2ContentDirectoryService createServiceInstance() throws Exception {
				return new Ums2ContentDirectoryService();
			}
		});
		return contentDirectoryService;
	}

	/**
	 * creates the upnp ConnectionManagerService.
	 *
	 * @return the service
	 */
	private static LocalService<UmsConnectionManagerService> createServerConnectionManagerService() {
		LocalService<UmsConnectionManagerService> connectionManagerService = new AnnotationLocalServiceBinder().read(UmsConnectionManagerService.class);
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
	private static LocalService<Ums2MediaReceiverRegistrarService> createMediaReceiverRegistrarService() {
		LocalService<Ums2MediaReceiverRegistrarService> mediaReceiverRegistrarService = new AnnotationLocalServiceBinder().read(Ums2MediaReceiverRegistrarService.class);
		mediaReceiverRegistrarService.setManager(new DefaultServiceManager<Ums2MediaReceiverRegistrarService>(mediaReceiverRegistrarService, null) {
			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected Ums2MediaReceiverRegistrarService createServiceInstance() throws Exception {
				return new Ums2MediaReceiverRegistrarService();
			}
		});
		return mediaReceiverRegistrarService;
	}

}
