package net.pms.dlna.virtual;

import net.pms.Messages;
import net.pms.database.MediaTableFilesStatus;
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

		String sqlJoinStart = "SELECT * FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE ";

		// All videos that are unwatched
		String movieCondition = " AND NOT ISTVEPISODE AND MEDIA_YEAR != '' AND DURATION > " + FORTY_MINUTES_IN_SECONDS;
		String unwatchedCondition = " AND " + MediaTableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE";
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT FILES.MOVIEORSHOWNAME FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.FORMAT_TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + "                                    ORDER BY FILES.MOVIEORSHOWNAME ASC",
				sqlJoinStart +                                                                                                                                                        "FILES.FORMAT_TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + " AND FILES.MOVIEORSHOWNAME = '${0}' ORDER BY FILES.TVSEASON, FILES.TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);

		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(Messages.getString("Movies"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 " + movieCondition + " AND STEREOSCOPY = ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(Messages.getString("3dMovies"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 " + movieCondition + " AND STEREOSCOPY != ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(Messages.getString("Unsorted"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND (MEDIA_YEAR IS NULL OR MEDIA_YEAR = '')" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedRecentlyAddedVideos = new MediaLibraryFolder(Messages.getString("RecentlyAdded"), sqlJoinStart + "FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILES.MODIFIED DESC LIMIT 100", MediaLibraryFolder.FILES_NOSORT);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("AllVideos"), sqlJoinStart + "FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILES.MODIFIED DESC",
				sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}'" + unwatchedCondition + " ORDER BY FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("HdVideos"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("SdVideos"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("DvdImages"), sqlJoinStart + "FILES.FORMAT_TYPE = 32" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.ISOS);

		// The following block contains all videos regardless of fully played status
		tvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT MOVIEORSHOWNAME FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE                              ORDER BY MOVIEORSHOWNAME ASC",
				"SELECT          *               FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);
		MediaLibraryFolder moviesFolder = new MediaLibraryFolder(
			Messages.getString("Movies"),
			"FORMAT_TYPE = 4 " + movieCondition + " AND STEREOSCOPY = '' ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder movies3DFolder = new MediaLibraryFolder(
			Messages.getString("3dMovies"),
			"FORMAT_TYPE = 4 " + movieCondition + " AND STEREOSCOPY != '' ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder unsortedFolder = new MediaLibraryFolder(
			Messages.getString("Unsorted"),
			"FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND (MEDIA_YEAR IS NULL OR MEDIA_YEAR = '') ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(
			Messages.getString("AllVideos"),
			"FORMAT_TYPE = 4 ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder recentlyAddedVideos = new MediaLibraryFolder(
			Messages.getString("RecentlyAdded"),
			new String[]{"SELECT * FROM FILES WHERE FORMAT_TYPE = 4 ORDER BY FILES.MODIFIED DESC LIMIT 100"},
			new int[]{MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder inProgressVideos = new MediaLibraryFolder(
			Messages.getString("InProgress"),
			sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY IS NOT NULL" + unwatchedCondition + " ORDER BY " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mostPlayedVideos = new MediaLibraryFolder(
			Messages.getString("MostPlayed"),
			sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY IS NOT NULL ORDER BY " + MediaTableFilesStatus.TABLE_NAME + ".PLAYCOUNT DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES WHERE FORMAT_TYPE = 4 ORDER BY FILES.MODIFIED DESC",
				"FORMAT_TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}' ORDER BY FILES.FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT_WITH_FILTERS, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(
			Messages.getString("HdVideos"),
			"FORMAT_TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)    ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(
			Messages.getString("SdVideos"),
			"FORMAT_TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576) ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(
			Messages.getString("DvdImages"),
			"FORMAT_TYPE = 32                                     ORDER BY FILENAME ASC",
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
					sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY IS NOT NULL ORDER BY " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY DESC LIMIT 100",
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
		allFolder = new MediaLibraryFolder(Messages.getString("AllAudioTracks"), "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY F.FILENAME ASC", MediaLibraryFolder.FILES);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(Messages.getString("AllAudioPlaylists"), "select FILENAME, MODIFIED from FILES F WHERE F.FORMAT_TYPE = 16 ORDER BY F.FILENAME ASC", MediaLibraryFolder.PLAYLISTS);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(Messages.getString("ByArtist"), new String[]{"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY ARTIST ASC", "select FILENAME, MODIFIED  from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(Messages.getString("ByAlbum"), new String[]{"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.ALBUM ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.ALBUM = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(Messages.getString("ByGenre"), new String[]{"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.GENRE ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(Messages.getString("ByArtistAlbum"), new String[]{
				"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(Messages.getString("ByGenreArtistAlbum"), new String[]{
				"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.GENRE ASC",
				"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${0}' ORDER BY ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${1}' AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${2}' AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf7);
		MediaLibraryFolder mlfAudioDate = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY F.MODIFIED DESC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlfAudioDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(
			Messages.getString("ByLetterArtistAlbum"),
			new String[]{
				"SELECT ID FROM REGEXP_RULES ORDER BY REGEXP_ORDER ASC",
				"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND ARTIST REGEXP (SELECT REGEXP_RULE FROM REGEXP_RULES WHERE ID = '${0}') ORDER BY ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}' ORDER BY A.ALBUM ASC",
				"SELECT FILENAME, MODIFIED FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${1}' AND A.ALBUM = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf8);
		addChild(vfAudio);

		VirtualFolder vfImage = new VirtualFolder(Messages.getString("Photo"), null);
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(
			Messages.getString("AllPhotos"),
			"FORMAT_TYPE = 2 ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES
		);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder mlfPhoto02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES WHERE FORMAT_TYPE = 2 ORDER BY MODIFIED DESC",
				"FORMAT_TYPE = 2 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY FILENAME ASC"
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
