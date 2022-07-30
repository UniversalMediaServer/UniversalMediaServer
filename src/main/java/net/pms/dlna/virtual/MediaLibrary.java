package net.pms.dlna.virtual;

import net.pms.Messages;
import net.pms.database.MediaTableAudiotracks;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableRegexpRules;
import net.pms.database.MediaTableVideoMetadatas;
import net.pms.util.FullyPlayedAction;

/**
 * This is the Media Library folder which contains dynamic folders populated
 * by the SQL (h2) database.
 */
public class MediaLibrary extends VirtualFolder {
	/**
	 * According to the Academy of Motion Picture Arts and Sciences, the American
	 * Film Institute, and the British Film Institute, a feature film runs for
	 * more than 40 minutes.
	 *
	 * @see https://www.oscars.org/sites/oscars/files/93aa_rules.pdf
	 * @see https://www.bfi.org.uk/bfi-national-archive/research-bfi-archive/bfi-filmography/bfi-filmography-faq
	 */
	private static final Double FORTY_MINUTES_IN_SECONDS = 2400.0;
	private static final String SELECT_FILES_STATUS_WHERE = "SELECT * " + MediaLibraryFolder.FROM_FILES_STATUS + "WHERE ";
	private static final String SELECT_FILES_STATUS_VIDEO_WHERE = "SELECT * " + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE ";

	private boolean enabled;

	private MediaLibraryFolder allFolder;
	private MediaLibraryFolder albumFolder;
	private MediaLibraryFolder artistFolder;
	private MediaLibraryFolder genreFolder;
	private MediaLibraryFolder playlistFolder;
	private MediaLibraryFolder tvShowsFolder;
	private VirtualFolder vfAudio = null;

	public boolean isEnabled() {
		return enabled;
	}

	public MediaLibraryFolder getAllFolder() {
		return allFolder;
	}

	public VirtualFolder getAudioFolder() {
		return vfAudio;
	}

	public MediaLibraryFolder getAlbumFolder() {
		return albumFolder;
	}

	public MediaLibrary() {
		super(Messages.getString("MediaLibrary"), "/images/folder-icons/media-library.png");
		reset();
	}

	public final void reset() {
		enabled = configuration.getUseCache();
		if (!enabled) {
			return;
		}
		// Videos folder
		VirtualFolder vfVideo = new VirtualFolder(Messages.getString("Video"), null);

		//String sqlJoinStart = "SELECT * FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE ";

		// All videos that are unwatched
		String movieCondition = " AND NOT " + MediaTableVideoMetadatas.ISTVEPISODE + " AND " + MediaTableVideoMetadatas.MEDIA_YEAR + " != '' AND DURATION > " + FORTY_MINUTES_IN_SECONDS;
		String unwatchedCondition = " AND " + MediaTableFilesStatus.ISFULLYPLAYED + " IS NOT TRUE";
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + unwatchedCondition + "                                    ORDER BY " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " ASC",
				SELECT_FILES_STATUS_VIDEO_WHERE +                                                                      MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + unwatchedCondition + " AND " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}' ORDER BY " + MediaTableVideoMetadatas.TVSEASON + ", " + MediaTableVideoMetadatas.TVEPISODENUMBER
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);

		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(Messages.getString("Movies"), SELECT_FILES_STATUS_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 " + movieCondition + " AND " + MediaTableFiles.STEREOSCOPY + " = ''" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(Messages.getString("3dMovies"), SELECT_FILES_STATUS_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 " + movieCondition + " AND " + MediaTableFiles.STEREOSCOPY + " != ''" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(Messages.getString("Unsorted"), SELECT_FILES_STATUS_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND NOT " + MediaTableVideoMetadatas.ISTVEPISODE + " AND (" + MediaTableVideoMetadatas.MEDIA_YEAR + " IS NULL OR " + MediaTableVideoMetadatas.MEDIA_YEAR + " = '')" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedRecentlyAddedVideos = new MediaLibraryFolder(Messages.getString("RecentlyAdded"), SELECT_FILES_STATUS_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4" + unwatchedCondition + " ORDER BY " + MediaTableFiles.MODIFIED + " DESC LIMIT 100", MediaLibraryFolder.FILES_NOSORT);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("AllVideos"), SELECT_FILES_STATUS_WHERE  + MediaTableFiles.FORMAT_TYPE + " = 4" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') " + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4" + unwatchedCondition + " ORDER BY " + MediaTableFiles.MODIFIED + " DESC",
				SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') = '${0}'" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("HdVideos"), SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND (" + MediaTableFiles.WIDTH + " > 864 OR " + MediaTableFiles.HEIGHT + " > 576)" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("SdVideos"), SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND (" + MediaTableFiles.WIDTH + " <= 864 AND " + MediaTableFiles.HEIGHT + " <= 576)" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("DvdImages"), SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 32" + unwatchedCondition + " ORDER BY " + MediaTableFiles.FILENAME + " ASC", MediaLibraryFolder.ISOS);

		// The following block contains all videos regardless of fully played status
		tvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + "                              ORDER BY " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " ASC",
				"SELECT          *               " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + " AND " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);
		MediaLibraryFolder moviesFolder = new MediaLibraryFolder(
			Messages.getString("Movies"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 " + movieCondition + " AND " + MediaTableFiles.STEREOSCOPY + " = '' ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder movies3DFolder = new MediaLibraryFolder(
			Messages.getString("3dMovies"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 " + movieCondition + " AND " + MediaTableFiles.STEREOSCOPY + " != '' ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder unsortedFolder = new MediaLibraryFolder(
			Messages.getString("Unsorted"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND NOT " + MediaTableVideoMetadatas.ISTVEPISODE + " AND (" + MediaTableVideoMetadatas.MEDIA_YEAR + " IS NULL OR " + MediaTableVideoMetadatas.MEDIA_YEAR + " = '') ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(
			Messages.getString("AllVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder recentlyAddedVideos = new MediaLibraryFolder(
			Messages.getString("RecentlyAdded"),
			new String[]{"SELECT * " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 ORDER BY " + MediaTableFiles.MODIFIED + " DESC LIMIT 100"},
			new int[]{MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder inProgressVideos = new MediaLibraryFolder(
			Messages.getString("InProgress"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableFilesStatus.DATELASTPLAY + " IS NOT NULL" + unwatchedCondition + " ORDER BY " + MediaTableFilesStatus.DATELASTPLAY + " DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mostPlayedVideos = new MediaLibraryFolder(
			Messages.getString("MostPlayed"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableFilesStatus.DATELASTPLAY + " IS NOT NULL ORDER BY " + MediaTableFilesStatus.PLAYCOUNT + " DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 ORDER BY " + MediaTableFiles.MODIFIED + " DESC",
				MediaTableFiles.FORMAT_TYPE + " = 4 AND FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT_WITH_FILTERS, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(
			Messages.getString("HdVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND (" + MediaTableFiles.WIDTH + " > 864 OR " + MediaTableFiles.HEIGHT + " > 576)    ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(
			Messages.getString("SdVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND (" + MediaTableFiles.WIDTH + " <= 864 AND " + MediaTableFiles.HEIGHT + " <= 576) ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(
			Messages.getString("DvdImages"),
			SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 32                                     ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.ISOS_WITH_FILTERS
		);

		// If fully played videos are to be hidden
		if (configuration.getFullyPlayedAction() == FullyPlayedAction.HIDE_MEDIA) {
			vfVideo.addChild(unwatchedTvShowsFolder);
			vfVideo.addChild(unwatchedMoviesFolder);
			vfVideo.addChild(unwatchedMovies3DFolder);
			vfVideo.addChild(unwatchedUnsortedFolder);
			vfVideo.addChild(unwatchedRecentlyAddedVideos);
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(unwatchedAllVideosFolder);
			vfVideo.addChild(unwatchedMlfVideo02);
			vfVideo.addChild(unwatchedMlfVideo03);
			vfVideo.addChild(unwatchedMlfVideo04);
			vfVideo.addChild(unwatchedMlfVideo05);
		// If fully played videos are NOT to be hidden
		} else {
			vfVideo.addChild(tvShowsFolder);
			vfVideo.addChild(moviesFolder);
			vfVideo.addChild(movies3DFolder);
			vfVideo.addChild(unsortedFolder);
			vfVideo.addChild(recentlyAddedVideos);
			if (configuration.isShowRecentlyPlayedFolder()) {
				MediaLibraryFolder recentlyPlayedVideos = new MediaLibraryFolder(
					Messages.getString("RecentlyPlayed"),
					SELECT_FILES_STATUS_VIDEO_WHERE + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableFilesStatus.DATELASTPLAY + " IS NOT NULL ORDER BY " + MediaTableFilesStatus.DATELASTPLAY + " DESC LIMIT 100",
					MediaLibraryFolder.FILES_NOSORT
				);
				vfVideo.addChild(recentlyPlayedVideos);
			}
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(mostPlayedVideos);
			vfVideo.addChild(allVideosFolder);
			vfVideo.addChild(mlfVideo02);
			vfVideo.addChild(mlfVideo03);
			vfVideo.addChild(mlfVideo04);
			vfVideo.addChild(mlfVideo05);
		}
		addChild(vfVideo);

		vfAudio = new VirtualFolder(Messages.getString("Audio"), null);
		allFolder = new MediaLibraryFolder(
			Messages.getString("AllAudioTracks"),
			"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES
		);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(
			Messages.getString("AllAudioPlaylists"),
			"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 16 ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.PLAYLISTS
		);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(
			Messages.getString("ByArtist"),
			new String[]{
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY ARTIST ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + "  FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(
			Messages.getString("ByAlbum"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY " + MediaTableAudiotracks.ALBUM + " ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND " + MediaTableAudiotracks.ALBUM + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(
			Messages.getString("ByGenre"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.GENRE + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY " + MediaTableAudiotracks.GENRE + " ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND " + MediaTableAudiotracks.GENRE + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(
			Messages.getString("ByArtistAlbum"),
			new String[]{
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.ALBUM + " ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.ALBUM + " = '${0}' ORDER BY " + MediaTableAudiotracks.TRACK + " ASC, " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(
			Messages.getString("ByGenreArtistAlbum"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.GENRE + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY " + MediaTableAudiotracks.GENRE + " ASC",
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND " + MediaTableAudiotracks.GENRE + " = '${0}' ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND " + MediaTableAudiotracks.GENRE + " = '${1}' AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.ALBUM + " ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND " + MediaTableAudiotracks.GENRE + " = '${2}' AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.ALBUM + " = '${0}' ORDER BY " + MediaTableAudiotracks.TRACK + " ASC, " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf7);
		MediaLibraryFolder mlfAudioDate = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 ORDER BY " + MediaTableFiles.MODIFIED + " DESC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableAudiotracks.TRACK + " ASC, " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlfAudioDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(
			Messages.getString("ByLetterArtistAlbum"),
			new String[]{
				"SELECT " + MediaTableRegexpRules.ID + " FROM " + MediaTableRegexpRules.TABLE_NAME + " ORDER BY " + MediaTableRegexpRules.REGEXP_ORDER + " ASC",
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND ARTIST REGEXP (SELECT " + MediaTableRegexpRules.REGEXP_RULE + " FROM " + MediaTableRegexpRules.TABLE_NAME + " WHERE " + MediaTableRegexpRules.ID + " = '${0}') ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.ALBUM + " ASC",
				"SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.ID + " = " + MediaTableAudiotracks.FILEID + " AND " + MediaTableFiles.FORMAT_TYPE + " = 1 AND COALESCE(" + MediaTableAudiotracks.ALBUMARTIST + ", " + MediaTableAudiotracks.ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.ALBUM + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf8);
		addChild(vfAudio);

		VirtualFolder vfImage = new VirtualFolder(Messages.getString("Photo"), null);
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(
			Messages.getString("AllPhotos"),
			MediaTableFiles.FORMAT_TYPE + " = 2 ORDER BY " + MediaTableFiles.FILENAME + " ASC",
			MediaLibraryFolder.FILES
		);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder mlfPhoto02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.FORMAT_TYPE + " = 2 ORDER BY " + MediaTableFiles.MODIFIED + " DESC",
				MediaTableFiles.FORMAT_TYPE + " = 2 AND FORMATDATETIME(" + MediaTableFiles.MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableFiles.FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfImage.addChild(mlfPhoto02);
		addChild(vfImage);
	}

	public MediaLibraryFolder getArtistFolder() {
		return artistFolder;
	}

	public MediaLibraryFolder getGenreFolder() {
		return genreFolder;
	}

	public MediaLibraryFolder getPlaylistFolder() {
		return playlistFolder;
	}
}
