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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package net.pms.encoders;

import java.io.File;
import java.util.ArrayList;

import net.pms.configuration.PmsConfiguration;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.io.BasicSystemUtils;
import net.pms.io.MacSystemUtils;
import net.pms.io.SolarisUtils;
import net.pms.io.SystemUtils;
import net.pms.io.WinUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

/**
 * This class handles players. Creating an instance will initialize the list of
 * known players.
 *
 * @since 1.51.0
 */
public final class PlayerFactory {
	/**
	 * Logger used for all logging.
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FormatFactory.class);

	/**
	 * List of registered and approved {@link Player} objects.
	 */
	private static ArrayList<Player> players = new ArrayList<Player>();

	/**
	 * List of registered {@link Player} objects.
	 */
	private static ArrayList<Player> allPlayers = new ArrayList<Player>();

	/**
	 * Interface to Windows specific functions, like Windows Registry. The
	 * registry is set by the constructor.
	 */
	private static SystemUtils utils;

	/**
	 * This class is not meant to be instantiated.
	 */
	private PlayerFactory() {
	}

	/**
	 * Constructor that registers all players based on the given configuration,
	 * frame and registry.
	 * 
	 * @param configuration The PMS configuration.
	 */
	public static void initialize(final PmsConfiguration configuration) {
		utils = createSystemUtils();
		registerPlayers(configuration);
	}

	/**
	 * Creates system utilities. These are needed to determine isAvis() in
	 * {@link #registerPlayer(Player)}.
	 *
	 * @return The system utilities.
	 */
	private static SystemUtils createSystemUtils() {
		if (Platform.isWindows()) {
			return new WinUtils();
		} else {
			if (Platform.isMac()) {
				return new MacSystemUtils();
			} else {
				if (Platform.isSolaris()) {
					return new SolarisUtils();
				} else {
					return new BasicSystemUtils();
				}
			}
		}
	}

	/**
	 * Register a known set of audio or video transcoders.
	 * 
	 * @param configuration
	 *            PMS configuration settings.
	 */
	private static void registerPlayers(final PmsConfiguration configuration) {

		if (Platform.isWindows()) {
			registerPlayer(new FFMpegVideo());
		}

		registerPlayer(new FFMpegAudio(configuration));
		registerPlayer(new MEncoderVideo(configuration));

		if (Platform.isWindows()) {
			registerPlayer(new MEncoderAviSynth(configuration));
		}

		registerPlayer(new MPlayerAudio(configuration));
		registerPlayer(new MEncoderWebVideo(configuration));
		registerPlayer(new MPlayerWebVideoDump(configuration));
		registerPlayer(new MPlayerWebAudio(configuration));
		registerPlayer(new TSMuxerVideo(configuration));
		registerPlayer(new TsMuxerAudio(configuration));
		registerPlayer(new VideoLanAudioStreaming(configuration));
		registerPlayer(new VideoLanVideoStreaming(configuration));

		if (Platform.isWindows()) {
			registerPlayer(new FFMpegDVRMSRemux());
		}

		registerPlayer(new RAWThumbnailer());
	}

	/**
	 * Adds a single {@link Player} to the list of Players. Before the player is
	 * added to the list, it is verified to be okay.
	 * 
	 * @param player Player to be added to the list.
	 */
	public static synchronized void registerPlayer(final Player player) {
		boolean ok = false;
		allPlayers.add(player);

		if (Player.NATIVE.equals(player.executable())) {
			ok = true;
		} else {
			if (Platform.isWindows()) {
				if (player.executable() == null) {
					LOGGER.info("Executable of transcoder profile " + player
							+ " not defined");
					return;
				}

				File executable = new File(player.executable());
				File executable2 = new File(player.executable() + ".exe");

				if (executable.exists() || executable2.exists()) {
					ok = true;
				} else {
					LOGGER.info("Executable of transcoder profile " + player
							+ " not found");
					return;
				}

				if (player.avisynth()) {
					ok = false;

					if (utils.isAvis()) {
						ok = true;
					} else {
						LOGGER.info("Transcoder profile " + player
								+ " will not be used because AviSynth was not found");
					}
				}
			} else if (!player.avisynth()) {
				ok = true;
			}
		}

		if (ok) {
			LOGGER.info("Registering transcoding engine: " + player);
			players.add(player);
		}
	}

	/**
	 * Returns the list of all players. This includes the ones not verified as
	 * being okay.
	 * 
	 * @return The list of players.
	 */
	public static ArrayList<Player> getAllPlayers() {
		return allPlayers;
	}

	/**
	 * Returns the list of players that have been verified as okay.
	 * 
	 * @return The list of players.
	 */
	public static ArrayList<Player> getPlayers() {
		return players;
	}

	/**
	 * Returns the player that matches the given class and format.
	 * 
	 * @param profileClass
	 *            The class to match.
	 * @param ext
	 *            The format to match.
	 * @return The player if a match could be found, <code>null</code>
	 *         otherwise.
	 */
	public static Player getPlayer(final Class<? extends Player> profileClass,
			final Format ext) {

		for (Player player : players) {
			if (player.getClass().equals(profileClass)
					&& player.type() == ext.getType()
					&& !player.excludeFormat(ext)) {
				return player;
			}
		}

		return null;
	}

	/**
	 * Returns the players matching the given classes and type.
	 * 
	 * @param profileClasses
	 *            The classes to match.
	 * @param type
	 *            The type to match.
	 * @return The list of players that match. If no players match, an empty
	 *         list is returned.
	 */
	public static ArrayList<Player> getPlayers(
			final ArrayList<Class<? extends Player>> profileClasses,
			final int type) {

		ArrayList<Player> compatiblePlayers = new ArrayList<Player>();

		for (Player player : players) {
			if (profileClasses.contains(player.getClass())
					&& player.type() == type) {
				compatiblePlayers.add(player);
			}
		}

		return compatiblePlayers;
	}
}
