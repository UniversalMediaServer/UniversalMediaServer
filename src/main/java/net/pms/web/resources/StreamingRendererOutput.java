package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.WebRender;

public class StreamingRendererOutput implements StreamingOutput {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingRendererOutput.class);

	private final InputStream in;

	private final WebRender renderer;

	public StreamingRendererOutput(InputStream in, WebRender renderer) {
		this.in = in;
		this.renderer = renderer;
	}

	@Override
	public void write(OutputStream os) throws IOException, WebApplicationException {
		try {
			byte[] buffer = new byte[32 * 1024];
			int bytes;
			int sendBytes = 0;

			try {
				while ((bytes = in.read(buffer)) != -1) {
					sendBytes += bytes;
					os.write(buffer, 0, bytes);
					os.flush();
				}
			} catch (IOException e) {
				LOGGER.trace("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + e.getMessage());
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			try {
				os.close();
			} catch (IOException e) {
			}
		} finally {
			if (renderer != null) {
				renderer.stop();
			}
		}
	}
}
