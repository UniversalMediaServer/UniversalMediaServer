package net.pms.dlna.virtual;

import net.pms.Messages;
import net.pms.database.MediaTableFilesStatus;
import net.pms.util.FullyPlayedAction;

/**
 * This is the Media Library folder which contains dynamic folders populated
 * by the SQL (h2) database.
 */
public class MediaLibrary extends VirtualFolder {
	private MediaLibraryFolder allFolder;

	public MediaLibraryFolder getAllFolder() {
		return allFolder;
	}

	private MediaLibraryFolder albumFolder;
	private MediaLibraryFolder artistFolder;
	private MediaLibraryFolder genreFolder;
	private MediaLibraryFolder playlistFolder;
	private MediaLibraryFolder tvShowsFolder;

	public MediaLibraryFolder getAlbumFolder() {
		return albumFolder;
	}

	public MediaLibrary() {
		super(Messages.getString("PMS.MediaLibrary"), "/images/folder-icons/media-library.png");
		init();
	}

	private void init() {
		// Videos folder
		VirtualFolder vfVideo = new VirtualFolder(Messages.getString("PMS.34"), null);

		String sqlJoinStart = "SELECT * FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE ";

		// All videos that are unwatched
		String unwatchedCondition = " AND " + MediaTableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE";
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.4"),
			new String[]{
				"SELECT DISTINCT FILES.MOVIEORSHOWNAME FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.FORMAT_TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + "                                    ORDER BY FILES.MOVIEORSHOWNAME ASC",
				sqlJoinStart +                                                                                                                                                        "FILES.FORMAT_TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + " AND FILES.MOVIEORSHOWNAME = '${0}' ORDER BY FILES.TVSEASON, FILES.TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);

		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.5"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND MEDIA_YEAR != '' AND STEREOSCOPY = ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.7"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND MEDIA_YEAR != '' AND STEREOSCOPY != ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.8"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND (MEDIA_YEAR IS NULL OR MEDIA_YEAR = '')" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedRecentlyAddedVideos = new MediaLibraryFolder(Messages.getString("MediaLibrary.RecentlyAdded"), sqlJoinStart + "FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILES.MODIFIED DESC LIMIT 100", MediaLibraryFolder.FILES_NOSORT);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("PMS.35"), sqlJoinStart + "FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.FORMAT_TYPE = 4" + unwatchedCondition + " ORDER BY FILES.MODIFIED DESC",
				sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}'" + unwatchedCondition + " ORDER BY FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("PMS.36"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("PMS.39"), sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("PMS.40"), sqlJoinStart + "FILES.FORMAT_TYPE = 32" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.ISOS);

		// The following block contains all videos regardless of fully played status
		tvShowsFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.4"),
			new String[]{
				"SELECT DISTINCT MOVIEORSHOWNAME FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE                              ORDER BY MOVIEORSHOWNAME ASC",
				"SELECT          *               FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);
		MediaLibraryFolder moviesFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.5"),
			"FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND MEDIA_YEAR != '' AND STEREOSCOPY = '' ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder movies3DFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.7"),
			"FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND MEDIA_YEAR != '' AND STEREOSCOPY != '' ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder unsortedFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.8"),
			"FORMAT_TYPE = 4 AND NOT ISTVEPISODE AND (MEDIA_YEAR IS NULL OR MEDIA_YEAR = '') ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(
			Messages.getString("PMS.35"),
			"FORMAT_TYPE = 4 ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder recentlyAddedVideos = new MediaLibraryFolder(
			Messages.getString("MediaLibrary.RecentlyAdded"),
			new String[]{"SELECT * FROM FILES WHERE FORMAT_TYPE = 4 ORDER BY FILES.MODIFIED DESC LIMIT 100"},
			new int[]{MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder inProgressVideos = new MediaLibraryFolder(
			Messages.getString("MediaLibrary.InProgress"),
			sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY IS NOT NULL" + unwatchedCondition + " ORDER BY " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mostPlayedVideos = new MediaLibraryFolder(
			Messages.getString("MediaLibrary.MostPlayed"),
			sqlJoinStart + "FILES.FORMAT_TYPE = 4 AND " + MediaTableFilesStatus.TABLE_NAME + ".DATELASTPLAY IS NOT NULL ORDER BY " + MediaTableFilesStatus.TABLE_NAME + ".PLAYCOUNT DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mlfVideo02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES WHERE FORMAT_TYPE = 4 ORDER BY FILES.MODIFIED DESC",
				"FORMAT_TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}' ORDER BY FILES.FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT_WITH_FILTERS, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(
			Messages.getString("PMS.36"),
			"FORMAT_TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)    ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(
			Messages.getString("PMS.39"),
			"FORMAT_TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576) ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(
			Messages.getString("PMS.40"),
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
					Messages.getString("VirtualFolder.1"),
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

		VirtualFolder vfAudio = new VirtualFolder(Messages.getString("PMS.1"), null);
		allFolder = new MediaLibraryFolder(Messages.getString("PMS.11"), "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY F.FILENAME ASC", MediaLibraryFolder.FILES);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(Messages.getString("PMS.9"), "select FILENAME, MODIFIED from FILES F WHERE F.FORMAT_TYPE = 16 ORDER BY F.FILENAME ASC", MediaLibraryFolder.PLAYLISTS);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(Messages.getString("PMS.13"), new String[]{"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY ARTIST ASC", "select FILENAME, MODIFIED  from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(Messages.getString("PMS.16"), new String[]{"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.ALBUM ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.ALBUM = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(Messages.getString("PMS.19"), new String[]{"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.GENRE ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(Messages.getString("PMS.22"), new String[]{
				"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(Messages.getString("PMS.26"), new String[]{
				"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY A.GENRE ASC",
				"SELECT DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${0}' ORDER BY ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${1}' AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND A.GENRE = '${2}' AND COALESCE(A.ALBUMARTIST, A.ARTIST) = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf7);
		MediaLibraryFolder mlfAudioDate = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.FORMAT_TYPE = 1 ORDER BY F.MODIFIED DESC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.FORMAT_TYPE = 1 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlfAudioDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(
			Messages.getString("PMS.28"),
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

		VirtualFolder vfImage = new VirtualFolder(Messages.getString("PMS.31"), null);
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(
			Messages.getString("PMS.32"),
			"FORMAT_TYPE = 2 ORDER BY FILENAME ASC",
			MediaLibraryFolder.FILES
		);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder mlfPhoto02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
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
