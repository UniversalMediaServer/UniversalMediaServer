package net.pms.platform.windows;

/**
 * This is an utility class containing Windows {@code KnownFolder} constants.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:ConstantName")
public class KnownFolders {

	/**
	 * Desktop ({@link CSIDL#CSIDL_DESKTOP})
	 * <code>{B4BFCC3A-DB2C-424C-B029-7FE99A87C641}</code>
	 */
	public static final GUID FOLDERID_Desktop = new GUID("{B4BFCC3A-DB2C-424C-B029-7FE99A87C641}");

	/** Downloads <code>{374DE290-123F-4565-9164-39C4925E467B}</code> */
	public static final GUID FOLDERID_Downloads = new GUID("{374DE290-123F-4565-9164-39C4925E467B}");

	/**
	 * Music ({@link CSIDL#CSIDL_MYMUSIC})
	 * <code>{4BD8D571-6D19-48D3-BE97-422220080E43}</code>
	 */
	public static final GUID FOLDERID_Music = new GUID("{4BD8D571-6D19-48D3-BE97-422220080E43}");

	/** Original Images <code>{2C36C0AA-5812-4B87-BFD0-4CD0DFB19B39}</code> */
	public static final GUID FOLDERID_OriginalImages = new GUID("{2C36C0AA-5812-4B87-BFD0-4CD0DFB19B39}");

	/** Slide Shows <code>{69D2CF90-FC33-4FB7-9A0C-EBB0F0FCB43C}</code> */
	public static final GUID FOLDERID_PhotoAlbums = new GUID("{69D2CF90-FC33-4FB7-9A0C-EBB0F0FCB43C}");

	/**
	 * Pictures ({@link CSIDL#CSIDL_MYPICTURES})
	 * <code>{33E28130-4E1E-4676-835A-98395C3BC3BB}</code>
	 */
	public static final GUID FOLDERID_Pictures = new GUID("{33E28130-4E1E-4676-835A-98395C3BC3BB}");

	/** Playlists <code>{DE92C1C7-837F-4F69-A3BB-86E631204A23}</code> */
	public static final GUID FOLDERID_Playlists = new GUID("{DE92C1C7-837F-4F69-A3BB-86E631204A23}");

	/** Public Downloads <code>{3D644C9B-1FB8-4F30-9B45-F670235F79C0}</code> */
	public static final GUID FOLDERID_PublicDownloads = new GUID("{3D644C9B-1FB8-4F30-9B45-F670235F79C0}");

	/**
	 * Public Music ({@link CSIDL#CSIDL_COMMON_MUSIC})
	 * <code>{3214FAB5-9757-4298-BB61-92A9DEAA44FF}</code>
	 */
	public static final GUID FOLDERID_PublicMusic = new GUID("{3214FAB5-9757-4298-BB61-92A9DEAA44FF}");

	/**
	 * Public Pictures ({@link CSIDL#CSIDL_COMMON_PICTURES})
	 * <code>{B6EBFB86-6907-413C-9AF7-4FC2ABF07CC5}</code>
	 */
	public static final GUID FOLDERID_PublicPictures = new GUID("{B6EBFB86-6907-413C-9AF7-4FC2ABF07CC5}");

	/**
	 * Public Videos ({@link CSIDL#CSIDL_COMMON_VIDEO})
	 * <code>{2400183A-6185-49FB-A2D8-4A392A602BA3}</code>
	 */
	public static final GUID FOLDERID_PublicVideos = new GUID("{2400183A-6185-49FB-A2D8-4A392A602BA3}");

	/**
	 * Videos ({@link CSIDL#CSIDL_MYVIDEO})
	 * <code>{18989B1D-99B5-455B-841C-AB7C74E4DDFC}</code>
	 */
	public static final GUID FOLDERID_Videos = new GUID("{18989B1D-99B5-455B-841C-AB7C74E4DDFC}");

	/**
	 * Music (Library) <code>{2112AB0A-C86A-4FFE-A368-0DE96E47012E}</code>
	 *
	 * @since Windows 7
	 */
	public static final GUID FOLDERID_MusicLibrary = new GUID("{2112AB0A-C86A-4FFE-A368-0DE96E47012E}");

	/**
	 * Pictures (Library) <code>{A990AE9F-A03B-4E80-94BC-9912D7504104}</code>
	 *
	 * @since Windows 7
	 */
	public static final GUID FOLDERID_PicturesLibrary = new GUID("{A990AE9F-A03B-4E80-94BC-9912D7504104}");

	/**
	 * Videos (Library) <code>{491E922F-5643-4Af4-A7EB-4E7A138D8174}</code>
	 *
	 * @since Windows 7
	 */
	public static final GUID FOLDERID_VideosLibrary = new GUID("{491E922F-5643-4Af4-A7EB-4E7A138D8174}");

	/**
	 * Recorded TV <code>{1A6FDBA2-F42D-4358-A798-B74D745926C5}</code>
	 *
	 * @since Windows 7
	 */
	public static final GUID FOLDERID_RecordedTVLibrary = new GUID("{1A6FDBA2-F42D-4358-A798-B74D745926C5}");

	/** Saved Pictures <code>{3B193882-D3AD-4EAB-965A-69829D1FB59F}</code> */
	public static final GUID FOLDERID_SavedPictures = new GUID("{3B193882-D3AD-4EAB-965A-69829D1FB59F}");

	/**
	 * Saved Pictures Library
	 * <code>{E25B5812-BE88-4BD9-94B0-29233477B6C3}</code>
	 *
	 * @since Windows 7
	 */
	public static final GUID FOLDERID_SavedPicturesLibrary = new GUID("{E25B5812-BE88-4BD9-94B0-29233477B6C3}"); // W7

	/**
	 * Screenshots <code>{B7BEDE81-DF94-4682-A7D8-57A52620B86F}</code>
	 *
	 * @since Windows 8
	 */
	public static final GUID FOLDERID_Screenshots = new GUID("{B7BEDE81-DF94-4682-A7D8-57A52620B86F}");

	/**
	 * Not to be instantiated.
	 */
	private KnownFolders() {
	}
}
