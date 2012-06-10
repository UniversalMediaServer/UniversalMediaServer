package net.pms.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class InternalJavaProcessImpl implements ProcessWrapper {
	private InputStream input;

	public InternalJavaProcessImpl(InputStream input) {
		this.input = input;
	}

	@Override
	public InputStream getInputStream(long seek) throws IOException {
		return input;
	}

	@Override
	public List<String> getResults() {
		return null;
	}

	@Override
	public boolean isDestroyed() {
		return true;
	}

	@Override
	public void runInNewThread() {
	}

	@Override
	public boolean isReadyToStop() {
		return false;
	}

	@Override
	public void setReadyToStop(boolean nullable) {
	}

	@Override
	public void stopProcess() {
	}
}
