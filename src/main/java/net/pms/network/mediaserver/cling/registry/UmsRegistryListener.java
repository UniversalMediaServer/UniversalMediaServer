package net.pms.network.mediaserver.cling.registry;

import net.pms.network.mediaserver.UPNPHelper;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;

public class UmsRegistryListener extends DefaultRegistryListener {

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		super.remoteDeviceAdded(registry, device);
		UPNPHelper.getInstance().remoteDeviceAdded(device);
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice d) {
		super.remoteDeviceRemoved(registry, d);
		UPNPHelper.getInstance().remoteDeviceRemoved(d);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice d) {
		super.remoteDeviceUpdated(registry, d);
		UPNPHelper.getInstance().remoteDeviceUpdated(d);
	}
}
