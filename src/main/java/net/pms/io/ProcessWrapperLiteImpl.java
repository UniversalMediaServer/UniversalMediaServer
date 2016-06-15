/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import net.pms.util.ProcessUtil;

public class ProcessWrapperLiteImpl implements ProcessWrapper {
	private Process p;

	public ProcessWrapperLiteImpl(Process p) {
		this.p = p;
	}

	@Override
	public InputStream getInputStream(long seek) throws IOException {
		return null;
	}

	@Override
	public ArrayList<String> getResults() {
		return null;
	}

	@Override
	public boolean isDestroyed() {
		return false;
	}

	@Override
	public void runInNewThread() {
	}

	@Override
	public void runInSameThread() {
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
		ProcessUtil.destroy(p);
	}
}
