package net.pms.dlna.virtual;

import net.pms.Messages;
import net.pms.database.TableFilesStatus;
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
		super(Messages.getString("PMS.MediaLibrary"), null);
		init();
	}

	private void init() {
		// Videos folder
		VirtualFolder vfVideo = new VirtualFolder(Messages.getString("PMS.34"), null);

		String sqlJoinStart = "SELECT * FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE ";

		// All videos that are unwatched
		String unwatchedCondition = " AND " + TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE";
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.4"),
			new String[]{
				"SELECT DISTINCT FILES.MOVIEORSHOWNAME FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + "                                                                ORDER BY FILES.MOVIEORSHOWNAME ASC",
				"SELECT DISTINCT FILES.TVSEASON        FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + " AND FILES.MOVIEORSHOWNAME = '${0}'                             ORDER BY FILES.TVSEASON ASC",
				sqlJoinStart +                                                                                                                                                        "FILES.TYPE = 4 AND FILES.ISTVEPISODE" + unwatchedCondition + " AND FILES.MOVIEORSHOWNAME = '${1}' AND FILES.TVSEASON = '${0}' ORDER BY FILES.TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.SEASONS, MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.5"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY = ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.7"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY != ''" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(Messages.getString("VirtualFolder.8"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND (YEAR IS NULL OR YEAR = '')" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("PMS.35"), sqlJoinStart + "FILES.TYPE = 4" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4" + unwatchedCondition + " ORDER BY FILES.MODIFIED DESC",
				sqlJoinStart + "FILES.TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}'" + unwatchedCondition + " ORDER BY FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("PMS.36"), sqlJoinStart + "FILES.TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("PMS.39"), sqlJoinStart + "FILES.TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576)" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("PMS.40"), sqlJoinStart + "FILES.TYPE = 32" + unwatchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.ISOS);

		// All videos that are watched
		String watchedCondition = " AND " + TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS TRUE";
		MediaLibraryFolder watchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.4"),
			new String[]{
				"SELECT DISTINCT FILES.MOVIEORSHOWNAME FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4 AND FILES.ISTVEPISODE" + watchedCondition + "                                                                ORDER BY FILES.MOVIEORSHOWNAME ASC",
				"SELECT DISTINCT FILES.TVSEASON        FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4 AND FILES.ISTVEPISODE" + watchedCondition + " AND FILES.MOVIEORSHOWNAME = '${0}'                             ORDER BY FILES.TVSEASON ASC",
				sqlJoinStart +                                                                                                                                                        "FILES.TYPE = 4 AND FILES.ISTVEPISODE" + watchedCondition + " AND FILES.MOVIEORSHOWNAME = '${1}' AND FILES.TVSEASON = '${0}' ORDER BY FILES.TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.SEASONS, MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder watchedMoviesFolder    = new MediaLibraryFolder(Messages.getString("VirtualFolder.5"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY = ''" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedMovies3DFolder  = new MediaLibraryFolder(Messages.getString("VirtualFolder.7"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY != ''" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedUnsortedFolder  = new MediaLibraryFolder(Messages.getString("VirtualFolder.8"), sqlJoinStart + "FILES.TYPE = 4 AND NOT ISTVEPISODE AND (YEAR IS NULL OR YEAR = '')" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("PMS.35"),          sqlJoinStart + "FILES.TYPE = 4" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') FROM FILES LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME WHERE FILES.TYPE = 4" + watchedCondition + " ORDER BY FILES.MODIFIED DESC",
				sqlJoinStart + "FILES.TYPE = 4 AND FORMATDATETIME(FILES.MODIFIED, 'yyyy MM d') = '${0}'" + watchedCondition + " ORDER BY FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder watchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("PMS.36"), sqlJoinStart + "FILES.TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("PMS.39"), sqlJoinStart + "FILES.TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576)" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder watchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("PMS.40"), sqlJoinStart + "FILES.TYPE = 32" + watchedCondition + " ORDER BY FILENAME ASC", MediaLibraryFolder.ISOS);

		// The following block contains all videos regardless of fully played status
		tvShowsFolder = new MediaLibraryFolder(
			Messages.getString("VirtualFolder.4"),
			new String[]{
				"SELECT DISTINCT MOVIEORSHOWNAME FROM FILES WHERE TYPE = 4 AND ISTVEPISODE                                                    ORDER BY MOVIEORSHOWNAME ASC",
				"SELECT DISTINCT TVSEASON        FROM FILES WHERE TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}'                       ORDER BY TVSEASON ASC",
				"SELECT          *               FROM FILES WHERE TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${1}' AND TVSEASON = '${0}' ORDER BY TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.SEASONS, MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder moviesFolder    = new MediaLibraryFolder(Messages.getString("VirtualFolder.5"), "TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY = '' ORDER BY FILENAME ASC",  MediaLibraryFolder.FILES);
		MediaLibraryFolder movies3DFolder  = new MediaLibraryFolder(Messages.getString("VirtualFolder.7"), "TYPE = 4 AND NOT ISTVEPISODE AND YEAR != '' AND STEREOSCOPY != '' ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unsortedFolder  = new MediaLibraryFolder(Messages.getString("VirtualFolder.8"), "TYPE = 4 AND NOT ISTVEPISODE AND (YEAR IS NULL OR YEAR = '') ORDER BY FILENAME ASC",      MediaLibraryFolder.FILES);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(Messages.getString("PMS.35"),          "TYPE = 4 ORDER BY FILENAME ASC",                                                          MediaLibraryFolder.FILES);
		MediaLibraryFolder mlfVideo02 = new MediaLibraryFolder(
			Messages.getString("PMS.12"),
			new String[]{
				"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES WHERE TYPE = 4 ORDER BY MODIFIED DESC",
				"TYPE = 4 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY FILENAME ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(Messages.getString("PMS.36"), "TYPE = 4 AND (WIDTH > 864 OR HEIGHT > 576)    ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(Messages.getString("PMS.39"), "TYPE = 4 AND (WIDTH <= 864 AND HEIGHT <= 576) ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(Messages.getString("PMS.40"), "TYPE = 32                                     ORDER BY FILENAME ASC", MediaLibraryFolder.ISOS);

		// If fully played videos are to be hidden
		if (configuration.getFullyPlayedAction() == FullyPlayedAction.HIDE_VIDEO) {
			vfVideo.addChild(unwatchedTvShowsFolder);
			vfVideo.addChild(unwatchedMoviesFolder);
			vfVideo.addChild(unwatchedMovies3DFolder);
			vfVideo.addChild(unwatchedUnsortedFolder);
			vfVideo.addChild(unwatchedAllVideosFolder);
			vfVideo.addChild(unwatchedMlfVideo02);
			vfVideo.addChild(unwatchedMlfVideo03);
			vfVideo.addChild(unwatchedMlfVideo04);
			vfVideo.addChild(unwatchedMlfVideo05);
		// If fully played videos are NOT to be hidden
		} else {
			VirtualFolder videosUnwatchedFolder = new VirtualFolder(Messages.getString("VirtualFolder.9"), null);
			videosUnwatchedFolder.addChild(unwatchedTvShowsFolder);
			videosUnwatchedFolder.addChild(unwatchedMoviesFolder);
			videosUnwatchedFolder.addChild(unwatchedMovies3DFolder);
			videosUnwatchedFolder.addChild(unwatchedUnsortedFolder);
			videosUnwatchedFolder.addChild(unwatchedAllVideosFolder);
			videosUnwatchedFolder.addChild(unwatchedMlfVideo02);
			videosUnwatchedFolder.addChild(unwatchedMlfVideo03);
			videosUnwatchedFolder.addChild(unwatchedMlfVideo04);
			videosUnwatchedFolder.addChild(unwatchedMlfVideo05);

			VirtualFolder videosWatchedFolder = new VirtualFolder(Messages.getString("VirtualFolder.Watched"), null);
			videosWatchedFolder.addChild(watchedTvShowsFolder);
			videosWatchedFolder.addChild(watchedMoviesFolder);
			videosWatchedFolder.addChild(watchedMovies3DFolder);
			videosWatchedFolder.addChild(watchedUnsortedFolder);
			videosWatchedFolder.addChild(watchedAllVideosFolder);
			videosWatchedFolder.addChild(watchedMlfVideo02);
			videosWatchedFolder.addChild(watchedMlfVideo03);
			videosWatchedFolder.addChild(watchedMlfVideo04);
			videosWatchedFolder.addChild(watchedMlfVideo05);

			vfVideo.addChild(videosUnwatchedFolder);
			vfVideo.addChild(videosWatchedFolder);
			vfVideo.addChild(tvShowsFolder);
			vfVideo.addChild(moviesFolder);
			vfVideo.addChild(movies3DFolder);
			vfVideo.addChild(unsortedFolder);
			vfVideo.addChild(allVideosFolder);
			vfVideo.addChild(mlfVideo02);
			vfVideo.addChild(mlfVideo03);
			vfVideo.addChild(mlfVideo04);
			vfVideo.addChild(mlfVideo05);
		}
		addChild(vfVideo);

		VirtualFolder vfAudio = new VirtualFolder(Messages.getString("PMS.1"), null);
		allFolder = new MediaLibraryFolder(Messages.getString("PMS.11"), "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 ORDER BY F.FILENAME ASC", MediaLibraryFolder.FILES);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(Messages.getString("PMS.9"), "select FILENAME, MODIFIED from FILES F WHERE F.TYPE = 16 ORDER BY F.FILENAME ASC", MediaLibraryFolder.PLAYLISTS);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(Messages.getString("PMS.13"), new String[]{"SELECT DISTINCT A.ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY A.ARTIST ASC", "select FILENAME, MODIFIED  from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(Messages.getString("PMS.16"), new String[]{"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY A.ALBUM ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND A.ALBUM = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(Messages.getString("PMS.19"), new String[]{"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY A.GENRE ASC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND A.GENRE = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(Messages.getString("PMS.22"), new String[]{
				"SELECT DISTINCT A.ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY A.ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(Messages.getString("PMS.26"), new String[]{
				"SELECT DISTINCT A.GENRE FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY A.GENRE ASC",
				"SELECT DISTINCT A.ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.GENRE = '${0}' ORDER BY A.ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.GENRE = '${1}' AND A.ARTIST = '${0}' ORDER BY A.ALBUM ASC",
				"select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND A.GENRE = '${2}' AND A.ARTIST = '${1}' AND A.ALBUM = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf7);
		MediaLibraryFolder mlfAudioDate = new MediaLibraryFolder(Messages.getString("PMS.12"), new String[]{"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 ORDER BY F.MODIFIED DESC", "select FILENAME, MODIFIED from FILES F, AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY A.TRACK ASC, F.FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlfAudioDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(Messages.getString("PMS.28"), new String[]{
				"SELECT ID FROM REGEXP_RULES ORDER BY ORDR ASC",
				"SELECT DISTINCT A.ARTIST FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST REGEXP (SELECT RULE FROM REGEXP_RULES WHERE ID = '${0}') ORDER BY A.ARTIST ASC",
				"SELECT DISTINCT A.ALBUM FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST = '${0}' ORDER BY A.ALBUM ASC",
				"SELECT FILENAME, MODIFIED FROM FILES F, AUDIOTRACKS A WHERE F.ID = A.FILEID AND F.TYPE = 1 AND A.ARTIST = '${1}' AND A.ALBUM = '${0}'"}, new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES});
		vfAudio.addChild(mlf8);
		addChild(vfAudio);

		VirtualFolder vfImage = new VirtualFolder(Messages.getString("PMS.31"), null);
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(Messages.getString("PMS.32"), "TYPE = 2 ORDER BY FILENAME ASC", MediaLibraryFolder.FILES);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder mlfPhoto02 = new MediaLibraryFolder(Messages.getString("PMS.12"), new String[]{"SELECT FORMATDATETIME(MODIFIED, 'yyyy MM d') FROM FILES WHERE TYPE = 2 ORDER BY MODIFIED DESC", "TYPE = 2 AND FORMATDATETIME(MODIFIED, 'yyyy MM d') = '${0}' ORDER BY FILENAME ASC"}, new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES});
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
