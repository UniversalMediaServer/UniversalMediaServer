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
package net.pms.encoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.external.FinalizeTranscoderArgsListener;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(Player.class);

	public static final int VIDEO_SIMPLEFILE_PLAYER = 0;
	public static final int AUDIO_SIMPLEFILE_PLAYER = 1;
	public static final int VIDEO_WEBSTREAM_PLAYER = 2;
	public static final int AUDIO_WEBSTREAM_PLAYER = 3;
	public static final int MISC_PLAYER = 4;
	public static final String NATIVE = "NATIVE";

	public abstract int purpose();
	public abstract JComponent config();
	public abstract String id();
	public abstract String name();
	public abstract int type();

	// FIXME this is an implementation detail (and not a very good one).
	// it's entirely up to engines how they construct their command lines.
	// need to get rid of this
	public abstract String[] args();

	public abstract String mimeType();
	public abstract String executable();
	protected static final PmsConfiguration _configuration = PMS.getConfiguration();
	protected PmsConfiguration configuration = _configuration;
	private static List<FinalizeTranscoderArgsListener> finalizeTranscoderArgsListeners = new ArrayList<>();

	public static void initializeFinalizeTranscoderArgsListeners() {
		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (listener instanceof FinalizeTranscoderArgsListener) {
				finalizeTranscoderArgsListeners.add((FinalizeTranscoderArgsListener) listener);
			}
		}
	}

	public boolean avisynth() {
		return false;
	}

	public boolean excludeFormat(Format extension) {
		return false;
	}

	public boolean isPlayerCompatible(RendererConfiguration renderer) {
		return true;
	}

	public boolean isInternalSubtitlesSupported() {
		return true;
	}

	public boolean isExternalSubtitlesSupported() {
		return true;
	}

	public boolean isTimeSeekable() {
		return false;
	}

	/**
	 * Each engine capable of video hardware acceleration must override this
	 * method and set
	 * <p>
	 * <code>return true</code>.
	 *
	 * @return false
	 */
	public boolean isGPUAccelerationReady() {
		return false;
	}

	/**
	 * @deprecated Use {@link #launchTranscode(net.pms.dlna.DLNAResource, net.pms.dlna.DLNAMediaInfo, net.pms.io.OutputParams)} instead.
	 */
	@Deprecated
	public final ProcessWrapper launchTranscode(String filename, DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		return launchTranscode(dlna, media, params);
	}

	public abstract ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException;

	@Override
	public String toString() {
		return name();
	}

	// no need to pass Player as a parameter: it's the invocant
	@Deprecated
	protected String[] finalizeTranscoderArgs(
		Player player,
		String filename,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params,
		String[] cmdArgs
	) {
		return finalizeTranscoderArgs(
			filename,
			dlna,
			media,
			params,
			cmdArgs
		);
	}

	protected String[] finalizeTranscoderArgs(
		String filename,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params,
		String[] cmdArgs
	) {
		if (finalizeTranscoderArgsListeners.isEmpty()) {
			return cmdArgs;
		}
		// make it mutable
		List<String> cmdList = new ArrayList<>(Arrays.asList(cmdArgs));

		for (FinalizeTranscoderArgsListener listener : finalizeTranscoderArgsListeners) {
			try {
				cmdList = listener.finalizeTranscoderArgs(
					this,
					filename,
					dlna,
					media,
					params,
					cmdList
				);
			} catch (Throwable t) {
				LOGGER.error("Failed to call finalizeTranscoderArgs on listener of type \"{}\"", listener.getClass().getSimpleName(), t.getMessage());
				LOGGER.trace("", t);
			}
		}

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);
		return cmdArray;
	}

	/**
	 * This method populates the supplied {@link OutputParams} object with the
	 * correct audio track (aid) and subtitles (sid) based on resource
	 * information and configuration settings.
	 *
	 * @param resource the {@link DLNAResource} to use.
	 * @param params The {@link OutputParams} to populate.
	 */
	public static void setAudioAndSubs(DLNAResource resource, OutputParams params) {
		if (resource == null || params == null || resource.getMedia() == null) {
			return;
		}
		if (params.aid == null) {
			params.aid = resource.resolveAudioStream(params.mediaRenderer);
		}

		if (params.sid != null && params.sid.getId() == -1) {
			LOGGER.trace("Don't want subtitles!");
			params.sid = null;
		} else if (params.sid == null) {
			params.sid = resource.resolveSubtitlesStream(params.mediaRenderer, params.aid == null ? null : params.aid.getLang(), true);
		}
	}

	/**
	 * @see #convertToModX(int, int)
	 */
	@Deprecated
	public int convertToMod4(int number) {
		return convertToModX(number, 4);
	}

	/**
	 * Convert number to be divisible by mod.
	 *
	 * @param number the number to convert
	 * @param mod the number to divide by
	 *
	 * @return the number divisible by mod
	 */
	public static int convertToModX(int number, int mod) {
		if (number % mod != 0) {
			number -= (number % mod);
		}

		return number;
	}

	/**
	 * Returns whether or not the player can handle a given resource.
	 * If the resource is <code>null</code> compatibility cannot be
	 * determined and <code>false</code> will be returned.
	 *
	 * @param resource
	 * The {@link DLNAResource} to be matched.
	 * @return True when the resource can be handled, false otherwise.
	 * @since 1.60.0
	 */
	public abstract boolean isCompatible(DLNAResource resource);

	/**
	 * Returns whether or not another player has the same
	 * name and id as this one.
	 *
	 * @param other
	 * The other player.
	 * @return True if names and ids match, false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof Player)) {
			return false;
		}
		Player otherPlayer = (Player) other;
		if (this.name() == null) {
			if (otherPlayer.name() != null) {
				return false;
			}
		} else if (!this.name().equals(otherPlayer.name())) {
			return false;
		}
		if (this.id() == null) {
			if (otherPlayer.id() != null) {
				return false;
			}
		} else if (!this.id().equals(otherPlayer.id())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name() == null ? 0 : name().hashCode());
		result = prime * result + (id() == null ? 0 : id().hashCode());
		return result;
	}
}
