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
package net.pms.network.mediaserver.cling.model.meta;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.cling.support.contentdirectory.UMSContentDirectoryService;
import net.pms.network.mediaserver.cling.support.xmicrosoft.UMSMediaReceiverRegistrarService;
import net.pms.util.PropertiesUtil;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;
import org.jupnp.support.xmicrosoft.AbstractMediaReceiverRegistrarService;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsLocalDevice {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsLocalDevice.class);
	/**
	 * Create the local UMS device
	 *
	 * @return the device
	 */
	public static LocalDevice createMediaServerDevice() {
		LocalDevice device;
		URL baseURL = null;
		try {
			baseURL = new URL(MediaServer.getURL() + "/");
		} catch (MalformedURLException ex) {
		}
		String friendlyName = CONFIGURATION.getServerDisplayName();
		String manufacturer = "Universal Media Server";
		String manufacturerURI = "https://www.universalmediaserver.com/";
		ManufacturerDetails manufacturerDetails = new ManufacturerDetails(manufacturer, manufacturerURI);
		String modelName = PMS.NAME;
		String modelDescription = CONFIGURATION.getServerName() + " - UPnP/AV 1.0 Compliant Media Server";
		String modelNumber = PropertiesUtil.getProjectProperties().get("project.version");
		String modelURI = "https://www.universalmediaserver.com/";
		ModelDetails modelDetails = new ModelDetails(modelName, modelDescription, modelNumber, modelURI);
		URI presentationURI = null;
		if (baseURL != null) {
			String webInterfaceUrl = "http" + (CONFIGURATION.getWebHttps() ? "s" : "") + "://" + baseURL.getHost() + ":" + CONFIGURATION.getWebInterfaceServerPort() + "/console/index.html";
			presentationURI = URI.create(webInterfaceUrl);
		}
		DeviceDetails umsDetails = new DeviceDetails(
				baseURL,
				friendlyName,
				manufacturerDetails,
				modelDetails,
				null,
				null,
				presentationURI
		);
		DeviceIdentity identity = new DeviceIdentity(new UDN(PMS.get().udn()));
		DeviceType type = new UDADeviceType("MediaServer");
		try {
			device = new LocalDevice(
					identity,
					type,
					umsDetails,
					createDeviceIcons(),
					createMediaServerServices()
			);
			return device;
		} catch (ValidationException e) {
			LOGGER.debug("Error in upnp local device creation: {}", e);
			device = null;
		}

		return device;
	}

	private static Icon[] createDeviceIcons() {
		List<Icon> icons = new ArrayList<>();
		try {
			icons.add(new Icon("image/png", 256, 256, 24, new URI("images/icon-256.png")));
			icons.add(new Icon("image/png", 128, 128, 24, new URI("images/icon-128.png")));
			icons.add(new Icon("image/png", 120, 120, 24, new URI("images/icon-120.png")));
			icons.add(new Icon("image/png", 48, 48, 24, new URI("images/icon-48.png")));
			icons.add(new Icon("image/jpeg", 128, 128, 24, new URI("images/icon-128.jpg")));
			icons.add(new Icon("image/jpeg", 120, 120, 24, new URI("images/icon-120.jpg")));
			icons.add(new Icon("image/jpeg", 48, 48, 24, new URI("images/icon-48.jpg")));
		} catch (URISyntaxException ex) {
			LOGGER.debug("Error in device icons creation: {}", ex);
		}
		return icons.toArray(new Icon[0]);
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
	private static LocalService<UMSContentDirectoryService> createContentDirectoryService() {
		LocalService<UMSContentDirectoryService> contentDirectoryService = new AnnotationLocalServiceBinder().read(UMSContentDirectoryService.class);
		contentDirectoryService.setManager(new DefaultServiceManager<UMSContentDirectoryService>(contentDirectoryService, null) {

			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected UMSContentDirectoryService createServiceInstance() throws Exception {
				return new UMSContentDirectoryService();
			}
		});
		return contentDirectoryService;
	}

	/**
	 * creates the upnp ConnectionManagerService.
	 *
	 * @return the service
	 */
	private static LocalService<ConnectionManagerService> createServerConnectionManagerService() {
		LocalService<ConnectionManagerService> connectionManagerService = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
		final ProtocolInfos sourceProtocols = getSourceProtocolInfos();

		connectionManagerService.setManager(new DefaultServiceManager<ConnectionManagerService>(connectionManagerService, ConnectionManagerService.class) {

			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected ConnectionManagerService createServiceInstance() throws Exception {
				return new ConnectionManagerService(sourceProtocols, null);
			}
		});

		return connectionManagerService;
	}

	/**
	 * creates the upnp MediaReceiverRegistrarService.
	 *
	 * @return the service
	 */
	private static LocalService<UMSMediaReceiverRegistrarService> createMediaReceiverRegistrarService() {
		LocalService<UMSMediaReceiverRegistrarService> mediaReceiverRegistrarService = new AnnotationLocalServiceBinder().read(AbstractMediaReceiverRegistrarService.class);
		mediaReceiverRegistrarService.setManager(new DefaultServiceManager<UMSMediaReceiverRegistrarService>(mediaReceiverRegistrarService, null) {
			@Override
			protected int getLockTimeoutMillis() {
				return 1000;
			}

			@Override
			protected UMSMediaReceiverRegistrarService createServiceInstance() throws Exception {
				return new UMSMediaReceiverRegistrarService();
			}
		});
		return mediaReceiverRegistrarService;
	}

	private static ProtocolInfos getSourceProtocolInfos() {
		return new ProtocolInfos(
				//this one overlap all ???
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, MimeType.WILDCARD, ProtocolInfo.WILDCARD),
				//this one overlap all images ???
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/" + MimeType.WILDCARD, ProtocolInfo.WILDCARD),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio", ProtocolInfo.WILDCARD),
				//this one overlap all audio ???
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/" + MimeType.WILDCARD, ProtocolInfo.WILDCARD),
				//this one overlap all video ???
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/" + MimeType.WILDCARD, ProtocolInfo.WILDCARD),
				//IMAGE
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_TN"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_SM"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_MED"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_LRG"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_RES_H_V"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_TN"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/gif", "DLNA.ORG_PN=GIF_LRG"),
				//AUDIO
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/mpeg", "DLNA.ORG_PN=MP3"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/L16", "DLNA.ORG_PN=LPCM"),
				//VIDEO
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=AVC_TS_HD_24_AC3_ISO;SONY.COM_PN=AVC_TS_HD_24_AC3_ISO"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=MPEG_TS_SD_EU_ISO"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=MPEG_TS_HD_50_L2_ISO;SONY.COM_PN=HD2_50_ISO"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=MPEG_TS_HD_60_L2_ISO;SONY.COM_PN=HD2_60_ISO"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_24_AC3;SONY.COM_PN=AVC_TS_HD_24_AC3"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_24_AC3_T;SONY.COM_PN=AVC_TS_HD_24_AC3_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_PS_PAL"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_PS_NTSC"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_50_L2_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_50_AC3_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_60_L2_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_60_AC3_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_EU"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_SD_EU_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_HD_50_L2_T;SONY.COM_PN=HD2_50_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=MPEG_TS_HD_60_L2_T;SONY.COM_PN=HD2_60_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_50_AC3;SONY.COM_PN=AVC_TS_HD_50_AC3"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_60_AC3;SONY.COM_PN=AVC_TS_HD_60_AC3"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_50_AC3_T;SONY.COM_PN=AVC_TS_HD_50_AC3_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/vnd.dlna.mpeg-tts", "DLNA.ORG_PN=AVC_TS_HD_60_AC3_T;SONY.COM_PN=AVC_TS_HD_60_AC3_T"),
				new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/x-mp2t-mphl-188", ProtocolInfo.WILDCARD)
		);
	}
}
