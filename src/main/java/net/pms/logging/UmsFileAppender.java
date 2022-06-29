/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import ch.qos.logback.core.rolling.RollingFileAppender;
import java.io.File;
import org.apache.commons.io.FileUtils;

/**
 * <code>UmsFileAppender</code> extends {@link RollingFileAppender} to rename
 * the old log file on startup when used with {@link UmsRollingPolicy}.
 * Use the custom {@link UmsRollingPolicy}, otherwise it will have the same effect
 * that the former {@link RollingFileAppender}.
 */
public class UmsFileAppender<E> extends RollingFileAppender<E> {

	@Override
	public void start() {
		savePreviousFile();
		super.start();
	}

	private void savePreviousFile() {
		if (!(getRollingPolicy() instanceof UmsRollingPolicy)) {
			return;
		}
		append = false;
		File currentFile = new File(getFile());
		//this check will avoid deleting old logs if no new one was produced.
		if (currentFile.exists()) {
			//here, we know that a previous log was produced.
			//save the last old log in the zip
			String baseFileName = currentFile.getAbsolutePath() + ".zip";
			String savedFileName = currentFile.getAbsolutePath() + ".prev.zip";
			addInfo("The previous log will be compressed to [" + savedFileName + "].");
			getRollingPolicy().rollover();
			//now rename the zip to prev.zip
			try {
				File logFile = new File(savedFileName);
				if (logFile.exists()) {
					FileUtils.deleteQuietly(logFile);
				}
				logFile = new File(baseFileName);
				if (logFile.exists()) {
					File newFile = new File(savedFileName);
					if (!logFile.renameTo(newFile)) {
						addWarn("Could not rename \"" + baseFileName + "\" to \"" + savedFileName + "\"");
					}
				}
			} catch (Exception e) {
				addWarn("Could not rename \"" + baseFileName + "\" to \"" + savedFileName + "\"", e);
			}
		}
	}
}
