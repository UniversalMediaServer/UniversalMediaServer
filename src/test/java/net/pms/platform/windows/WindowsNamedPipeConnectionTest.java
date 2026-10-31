package net.pms.platform.windows;

import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
public class WindowsNamedPipeConnectionTest {
	private static final class DelayedPipe extends WindowsNamedPipe {
		DelayedPipe() { super("ums-early-connect-test-" + UUID.randomUUID(), false, true, null); }
		@Override
		public synchronized void start() { /* Delay only the reader, not native pipe creation. */ }
		void startReader() { super.start(); }
	}

	@Test
	public void clientMayConnectBeforeReaderStarts() throws Exception {
		DelayedPipe pipe = new DelayedPipe();
		var worker = Executors.newSingleThreadExecutor();
		try (var output = new FileOutputStream(pipe.getPipeName()); var input = pipe.getReadable()) {
			// Opening the client first forces ERROR_PIPE_CONNECTED on the server.
			pipe.startReader();
			byte[] expected = new byte[]{0, 1, 127, (byte) 128, (byte) 255};
			output.write(expected);
			output.flush();
			var read = worker.submit(() -> input.readNBytes(expected.length));
			assertArrayEquals(expected, read.get(2, TimeUnit.SECONDS));
		} finally {
			worker.shutdownNow();
			pipe.join(3000);
			assertFalse(pipe.isAlive(), "Reader must stop when its client closes");
		}
	}
}
