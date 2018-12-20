package net.pms.platform.windows;


/**
 * This {@code enum} represents Windows {@code CSIDL} constants.
 *
 * @author Nadahar
 */
public enum CSIDL {

	/** Shared Music ({@link KnownFolders#FOLDERID_PublicMusic}) */
	CSIDL_COMMON_MUSIC(0x0035),

	/** Shared Pictures ({@link KnownFolders#FOLDERID_PublicPictures}) */
	CSIDL_COMMON_PICTURES(0x0036),

	/** Shared Video ({@link KnownFolders#FOLDERID_PublicVideos}) */
	CSIDL_COMMON_VIDEO(0x0037),

	/** Desktop ({@link KnownFolders#FOLDERID_Desktop}) */
	CSIDL_DESKTOP(0x0000),

	/** My Music ({@link KnownFolders#FOLDERID_Music}) */
	CSIDL_MYMUSIC(0x000d),

	/** My Pictures ({@link KnownFolders#FOLDERID_Pictures}) */
	CSIDL_MYPICTURES(0x0027),

	/** My Videos ({@link KnownFolders#FOLDERID_Videos}) */
	CSIDL_MYVIDEO(0x000e);

	private final int value;

	private CSIDL(int value) {
		this.value = value;
	}

	/**
	 * @return The integer ID value.
	 */
	public int getValue() {
		return value;
	}

}
