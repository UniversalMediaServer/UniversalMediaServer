/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.renderers;

import java.util.List;
import net.pms.encoders.Engine;
import net.pms.io.OutputParams;
import net.pms.store.StoreResource;

public interface OutputOverride {
	/**
	 * Override a engine's default output formatting.
	 * To be invoked by the engine after input and filter options are complete.
	 *
	 * @param cmdList the command so far
	 * @param resource the media item
	 * @param engine the engine
	 * @param params the output parameters
	 *
	 * @return whether the options have been finalized
	 */
	public boolean getOutputOptions(List<String> cmdList, StoreResource resource, Engine engine, OutputParams params);

	public boolean addSubtitles();
}