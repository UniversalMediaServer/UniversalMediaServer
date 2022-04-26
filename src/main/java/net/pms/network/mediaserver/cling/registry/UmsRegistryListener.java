package net.pms.network.mediaserver.cling.registry;

import net.pms.network.mediaserver.UPNPControl;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

public class UmsRegistryListener extends DefaultRegistryListener {
	UPNPControl control;

	public UmsRegistryListener(UPNPControl control) {
		this.control = control;
	}

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		super.remoteDeviceAdded(registry, device);
		control.remoteDeviceAdded(device);
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice d) {
		super.remoteDeviceRemoved(registry, d);
		control.remoteDeviceRemoved(d);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice d) {
		super.remoteDeviceUpdated(registry, d);
		control.remoteDeviceUpdated(d);
	}
}
