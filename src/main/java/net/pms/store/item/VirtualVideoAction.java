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
package net.pms.store.item;

import java.io.IOException;
import java.io.InputStream;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.parsers.Parser;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import org.apache.commons.lang.StringUtils;

/**
 * Implements a container that when browsed, an action will be performed.
 * The class assumes that the action to be performed is to toggle a boolean value.
 * Because of this, the thumbnail is either a green tick mark or a red cross. Equivalent
 * videos are shown after the value is toggled.<p>
 * However this is just cosmetic. Any action can be performed.
 */
public abstract class VirtualVideoAction extends StoreItem {

	private final String thumbnailIconOK;
	private final String thumbnailIconKO;
	private final String videoOk;
	private final String videoKo;
	private boolean enabled;
	private long timer1;
	protected String name;

	/**
	 * Constructor for this class. Recommended instantiation includes overriding
	 * the {@link #enable()} function (example shown in the link).
	 * @param name Name that is shown via the UPNP ContentBrowser service.
	 *             This field cannot be changed after the instantiation.
	 * @param enabled If true, a green tick mark is shown as thumbnail.
	 *                If false, a red cross is shown. This initial value
	 *                is usually changed via the {@link #enable()} function.
	 * @param enabledIconOverride path to an icon to use for the enabled thumbnail
	 */
	protected VirtualVideoAction(Renderer renderer, String name, boolean enabled, String enabledIconOverride) {
		super(renderer);
		this.name = name;
		if (StringUtils.isNotBlank(enabledIconOverride)) {
			thumbnailIconOK = enabledIconOverride;
		} else {
			thumbnailIconOK = "images/store/action-ok.png";
		}
		thumbnailIconKO = "images/store/action-cancel.png";
		this.videoOk = "videos/action_success-512.mpg";
		this.videoKo = "videos/button_cancel-512.mpg";
		timer1 = -1;
		this.enabled = enabled;

		// Create correct fakeMediaInfo for the embedded .mpg videos
		// This is needed by Format.isCompatible()
		MediaInfo fakeMediaInfo = new MediaInfo();
		fakeMediaInfo.setContainer("mpegps");
		fakeMediaInfo.setMimeType("video/mpeg");
		MediaVideo video = new MediaVideo();
		video.setCodec("mpeg2");
		fakeMediaInfo.addVideoTrack(video);
		fakeMediaInfo.setMediaParser(Parser.MANUAL_PARSER);
		setMediaInfo(fakeMediaInfo);
	}

	/**
	 * Returns <code>false</code> because this virtual video action should not
	 * appear in the transcode folder.
	 *
	 * @return Always returns <code>false</code>
	 */
	@Override
	public boolean isTranscodeFolderAvailable() {
		return false;
	}

	/**
	 * This function is called as an action from the UPnP client when
	 * the user tries to play this item. This function calls instead the enable()
	 * function in order to execute an action.
	 * As the client expects to play an item, a really short video (less than
	 * 1s) is shown with the results of the action.
	 *
	 * @see #enable()
	 * @see net.pms.store.storeResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (timer1 == -1) {
			timer1 = System.currentTimeMillis();
		} else if (System.currentTimeMillis() - timer1 < 2000) {
			timer1 = -1;
		}

		if (timer1 != -1) {
			enabled = enable();
		}

		return HTTPResource.getResourceInputStream(enabled ? videoOk : videoKo);
	}

	/**
	 * Prototype. This function is called by {@link #getInputStream()} and is the core of this class.
	 * The main purpose of this function is toggle a boolean variable somewhere.
	 * The value of that boolean variable is shown then as either a green tick mark or a red cross.
	 * However, this is just a cosmetic thing. Any Java code can be executed in this function, not only toggling a boolean variable.
	 * Recommended way to instantiate this class is as follows:
	 * <pre> VirtualFolder vf;
	 * [...]
	 * vf.addChild(new VirtualVideoAction(Messages.getString("AvSyncAlternativeMethod"), configuration.isMencoderNoOutOfSync()) {
	 *   public boolean enable() {
	 *   configuration.setMencoderNoOutOfSync(!configuration.isMencoderNoOutOfSync());
	 *   return configuration.isMencoderNoOutOfSync();
	 *   }
	 * }); </pre>
	 * @return If true, a green tick mark is shown as thumbnail. If false, a red cross is shown.
	 */
	public abstract boolean enable();

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns an invalid length as this item is not
	 * TODO: (botijo) VirtualFolder returns 0 instead of -1.
	 * @return -1, an invalid length for an item.
	 * @see net.pms.store.storeResource#length()
	 */
	@Override
	public long length() {
		//DLNAMediaInfo.TRANS_SIZE;
		return -1;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	/**
	 * Returns either a green tick mark or a red cross that represents the
	 * actual value of this item
	 * @throws IOException
	 *
	 * @see net.pms.store.storeResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.getResourceInputStream(enabled ? thumbnailIconOK : thumbnailIconKO));
	}

	/**
	 * @return True, as this kind of item is always valid.
	 * @see net.pms.store.storeResource#isValid()
	 */
	@Override
	public boolean isValid() {
		setFormat(FormatFactory.getAssociatedFormat(this.videoOk));
		return true;
	}

	@Override
	protected boolean isResumeable() {
		return false;
	}
}
