package net.pms.dlna;

import java.io.IOException;
import java.io.OutputStream;

public interface IPushOutput {
	public void push(OutputStream out) throws IOException;
	public boolean isUnderlyingSeekSupported();
}
