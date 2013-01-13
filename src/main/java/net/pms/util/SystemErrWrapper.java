package net.pms.util;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemErrWrapper extends OutputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(SystemErrWrapper.class);
	private int pos = 0;
	private byte line[] = new byte[5000];

	@Override
	public void write(int b) throws IOException {
		if (b == 10) {
			byte text[] = new byte[pos];
			System.arraycopy(line, 0, text, 0, pos);
			LOGGER.info(new String(text));
			pos = 0;
			line = new byte[5000];
		} else if (b != 13) {
			line[pos] = (byte) b;
			pos++;
		}
	}
}
