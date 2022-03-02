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

import java.net.URI;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.PropertiesUtil;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.DLNACaps;
import org.jupnp.model.types.DLNADoc;

public class UmsDeviceDetailsProvider implements DeviceDetailsProvider {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final String MANUFACTURER_NAME = "Universal Media Server";
	private static final String MANUFACTURER_URL = "https://www.universalmediaserver.com/";
	private static final ManufacturerDetails MANUFACTURER_DETAILS = new ManufacturerDetails(MANUFACTURER_NAME, MANUFACTURER_URL);
	private static final String MODEL_NUMBER = PropertiesUtil.getProjectProperties().get("project.version");
	private static final String PRESENTATION_URI = "/console/index.html";
	private	static final DLNADoc[] DLNA_DOCS = new DLNADoc[] {new DLNADoc("DMS", DLNADoc.Version.V1_5), new DLNADoc("M-DMS", DLNADoc.Version.V1_5)};
	private	static final DLNACaps DLNA_CAPS = new DLNACaps(new String[] {});
	private static final DLNACaps SEC_CAP = new DLNACaps(new String[] {"smi", "DCM10", "getMediaInfo.sec", "getCaptionInfo.sec"});

	@Override
	public DeviceDetails provide(RemoteClientInfo info) {
		//find the renderer
		UmsRemoteClientInfo umsinfo = new UmsRemoteClientInfo(info);
		String friendlyName = CONFIGURATION.getServerDisplayName();
		if (umsinfo.isXbox360Request()) {
			friendlyName += " : Windows Media Connect";
		}
		String modelName = umsinfo.isXbox360Request() ? "Windows Media Connect" : PMS.NAME;
		String modelDescription = CONFIGURATION.getServerName() + " - UPnP/AV 1.0 Compliant Media Server";
		String modelNumber = umsinfo.isXbox360Request() ? "1" : MODEL_NUMBER;
		ModelDetails modelDetails = new ModelDetails(modelName, modelDescription, modelNumber, MANUFACTURER_URL);
		URI presentationURI = null;
		if (umsinfo.getLocalAddress() != null) {
			String webInterfaceUrl = "http" + (CONFIGURATION.getWebHttps() ? "s" : "") + "://" + umsinfo.getLocalAddress().getHostAddress() + ":" + CONFIGURATION.getWebInterfaceServerPort() + PRESENTATION_URI;
			presentationURI = URI.create(webInterfaceUrl);
		}
		DeviceDetails umsDetails = new DeviceDetails(
				null,
				friendlyName,
				MANUFACTURER_DETAILS,
				modelDetails,
				null,
				null,
				presentationURI,
				DLNA_DOCS,
				DLNA_CAPS,
				SEC_CAP
		);
		return umsDetails;
	}

}
