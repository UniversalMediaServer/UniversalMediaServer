package net.pms.store.container.audioaddict;

import java.util.List;
import net.pms.external.audioaddict.AudioAddictChannelDto;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;

public class AudioAddictNetworkFilter extends StoreContainer {

	private Platform network = null;

	public AudioAddictNetworkFilter(Renderer renderer, Platform network, String name) {
		super(renderer, name, null);
		this.network = network;

		List<AudioAddictChannelDto> filterList = AudioAddictService.get().getFilteredChannels(this.network, getName());
		for (AudioAddictChannelDto c : filterList) {
			MediaInfo mi = new MediaInfo();
			mi.setMimeType("audio/mpeg");
			mi.setMediaParser("STATIC");
			String genres = StringUtils.trimToNull(c.genres);
			String descShort = StringUtils.trimToNull(c.descShort);
			if (genres != null || descShort != null) {
				MediaAudioMetadata md = new MediaAudioMetadata();
				md.setArtist(genres);
				md.setGenre(genres);
				md.setAlbum(descShort);
				mi.setAudioMetadata(md);
			}
			StoreResource sr = new AudioAddictRadioStream(renderer, c.name, c.streamUrl, "http:" + c.albumArt, network, c.id);
			sr.setMediaInfo(mi);
			addChild(sr);
		}
	}
}
