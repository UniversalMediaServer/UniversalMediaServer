package net.pms.network;

import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchPower extends AbstractContentDirectoryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SwitchPower.class);

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag,
							   String filter,
							   long firstResult, long maxResults,
							   SortCriterion[] orderby) throws ContentDirectoryException {
		try {

			// This is just an example... you have to create the DIDL content dynamically!
			LOGGER.debug("############################");
			LOGGER.debug("HERE WE GO");
			LOGGER.debug("############################");

			DIDLContent didl = new DIDLContent();

			String album = ("Black Gives Way To Blue");
			String creator = "Alice In Chains"; // Required
			PersonWithRole artist = new PersonWithRole(creator, "Performer");
			MimeType mimeType = new MimeType("audio", "mpeg");

			didl.addItem(new MusicTrack(
					"101", "3", // 101 is the Item ID, 3 is the parent Container ID
					"All Secrets Known",
					creator, album, artist,
					new Res(mimeType, 123456l, "00:03:25", 8192l, "http://10.0.0.1/files/101.mp3")
			));

			didl.addItem(new MusicTrack(
					"102", "3",
					"Check My Brain",
					creator, album, artist,
					new Res(mimeType, 2222222l, "00:04:11", 8192l, "http://10.0.0.1/files/102.mp3")
			));

			// Create more tracks...

			// Count and total matches is 2
			return new BrowseResult(new DIDLParser().generate(didl), 2, 2);

		} catch (Exception ex) {
			throw new ContentDirectoryException(
					ContentDirectoryErrorCode.CANNOT_PROCESS,
					ex.toString()
			);
		}
	}

	@Override
	public BrowseResult search(String containerId,
							   String searchCriteria, String filter,
							   long firstResult, long maxResults,
							   SortCriterion[] orderBy) throws ContentDirectoryException {
		// You can override this method to implement searching!
		return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
	}
}