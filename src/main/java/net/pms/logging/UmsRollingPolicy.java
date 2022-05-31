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
package net.pms.logging;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import java.io.File;

/**
 * <code>UmsRollingPolicy</code> extends {@link RollingPolicyBase} to compress
 * the rolled log file with the custom {@link UmsCompressor}.
 */
public class UmsRollingPolicy extends RollingPolicyBase {
	final UmsCompressor compressor = new UmsCompressor();

	@Override
	public void rollover() throws RolloverFailure {
		compressor.compress(getActiveFileName(), getActiveFileName() + ".zip", new File(getActiveFileName()).getName());
	}

	/**
	 * Return the value of the parent's RawFile property.
	 *
	 * @return ActiveFileName
	 */
	@Override
	public String getActiveFileName() {
		return getParentsRawFileProperty();
	}

	@Override
	public void setContext(Context context) {
		super.setContext(context);
		if (this.context != null) {
			compressor.setContext(this.context);
		}
	}
}
