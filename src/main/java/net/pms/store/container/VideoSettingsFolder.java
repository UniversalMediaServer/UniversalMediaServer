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
package net.pms.store.container;

import net.pms.PMS;
import net.pms.renderers.Renderer;
import net.pms.store.item.VirtualVideoAction;
import net.pms.store.item.VirtualVideoActionLocalized;
import net.pms.util.CodeDb;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class VideoSettingsFolder extends LocalizedStoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(VideoSettingsFolder.class);

	public VideoSettingsFolder(Renderer renderer) {
		super(renderer, "VideoSettings_FolderName");
	}

	/**
	 * Returns Video Settings folder. Used by manageRoot, so it is usually used
	 * as a folder at the root folder. Child objects are created when this
	 * folder is created.
	 */
	public static VideoSettingsFolder getVideoSettingsFolder(Renderer renderer) {
		VideoSettingsFolder res = null;

		if (renderer.getUmsConfiguration().isShowServerSettingsFolder()) {
			res = new VideoSettingsFolder(renderer);
			LocalizedStoreContainer vfSub = new LocalizedStoreContainer(renderer, "Subtitles");
			res.addChild(vfSub);

			if (renderer.getUmsConfiguration().useCode() && !PMS.get().masterCodeValid() &&
					StringUtils.isNotEmpty(PMS.get().codeDb().lookup(CodeDb.MASTER))) {
				// if the master code is valid we don't add this
				VirtualVideoAction vva = new VirtualVideoAction(renderer, "MasterCode", true, null) {
					@Override
					public boolean enable() {
						CodeEnter ce = (CodeEnter) getParent();
						if (ce.validCode(this)) {
							PMS.get().setMasterCode(ce);
							return true;
						}
						return false;
					}
				};
				CodeEnter ce1 = new CodeEnter(vva);
				ce1.setCode(CodeDb.MASTER);
				res.addChild(ce1);
			}

			res.addChild(new VirtualVideoActionLocalized(renderer, "AvSyncAlternativeMethod", renderer.getUmsConfiguration().isMencoderNoOutOfSync(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderNoOutOfSync(!renderer.getUmsConfiguration().isMencoderNoOutOfSync());
					return renderer.getUmsConfiguration().isMencoderNoOutOfSync();
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "DefaultH264RemuxMencoder", renderer.getUmsConfiguration().isMencoderMuxWhenCompatible(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderMuxWhenCompatible(!renderer.getUmsConfiguration().isMencoderMuxWhenCompatible());

					return renderer.getUmsConfiguration().isMencoderMuxWhenCompatible();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, "  !!-- Fix 23.976/25fps A/V Mismatch --!!", renderer.getUmsConfiguration().isFix25FPSAvMismatch(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderForceFps(!renderer.getUmsConfiguration().isFix25FPSAvMismatch());
					renderer.getUmsConfiguration().setFix25FPSAvMismatch(!renderer.getUmsConfiguration().isFix25FPSAvMismatch());
					return renderer.getUmsConfiguration().isFix25FPSAvMismatch();
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "DeinterlaceFilter", renderer.getUmsConfiguration().isMencoderYadif(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderYadif(!renderer.getUmsConfiguration().isMencoderYadif());

					return renderer.getUmsConfiguration().isMencoderYadif();
				}
			});

			vfSub.addChild(new VirtualVideoActionLocalized(renderer, "DisableSubtitles", renderer.getUmsConfiguration().isDisableSubtitles(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isDisableSubtitles();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setDisableSubtitles(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoActionLocalized(renderer, "AutomaticallyLoadSrtSubtitles", renderer.getUmsConfiguration().isAutoloadExternalSubtitles(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isAutoloadExternalSubtitles();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setAutoloadExternalSubtitles(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoActionLocalized(renderer, "UseEmbeddedStyle", renderer.getUmsConfiguration().isUseEmbeddedSubtitlesStyle(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isUseEmbeddedSubtitlesStyle();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setUseEmbeddedSubtitlesStyle(newValue);
					return newValue;
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "SkipLoopFilterDeblocking", renderer.getUmsConfiguration().getSkipLoopFilterEnabled(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setSkipLoopFilterEnabled(!renderer.getUmsConfiguration().getSkipLoopFilterEnabled());
					return renderer.getUmsConfiguration().getSkipLoopFilterEnabled();
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "KeepDtsTracks", renderer.getUmsConfiguration().isAudioEmbedDtsInPcm(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setAudioEmbedDtsInPcm(!renderer.getUmsConfiguration().isAudioEmbedDtsInPcm());
					return renderer.getUmsConfiguration().isAudioEmbedDtsInPcm();
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "SaveConfiguration", true, null) {
				@Override
				public boolean enable() {
					try {
						renderer.getUmsConfiguration().save();
					} catch (ConfigurationException e) {
						LOGGER.debug("Caught exception", e);
					}
					return true;
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "RestartServer", true, null) {
				@Override
				public boolean enable() {
					PMS.get().resetMediaServer();
					return true;
				}
			});

			res.addChild(new VirtualVideoActionLocalized(renderer, "ShowLiveSubtitlesFolder", renderer.getUmsConfiguration().isShowLiveSubtitlesFolder(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setShowLiveSubtitlesFolder(renderer.getUmsConfiguration().isShowLiveSubtitlesFolder());
					return renderer.getUmsConfiguration().isShowLiveSubtitlesFolder();
				}
			});
		}

		return res;
	}

}
