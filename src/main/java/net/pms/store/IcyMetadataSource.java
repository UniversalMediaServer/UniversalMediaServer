package net.pms.store;

import java.io.InputStream;

/**
 * A StoreItem whose stream can carry SHOUTcast/Icecast (ICY) in-band metadata, so a
 * renderer that requests it (request header "Icy-MetaData: 1" can display the currently
 * playing track.
 */
public interface IcyMetadataSource {

	/**
	 * Default byte interval between ICY metadata blocks.
	 */
	int DEFAULT_ICY_METAINT = 16000;

	default int getIcyMetaInt() {
		return DEFAULT_ICY_METAINT;
	}

	/**
	 * Builds a stream that interleaves ICY metadata every "metaInt" bytes.
	 *
	 * @param metaInt the byte interval between metadata blocks (must match the advertised header).
	 */
	InputStream getIcyInputStream(int metaInt);
}
