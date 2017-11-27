package net.pms.dlna;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.util.OpenSubtitle;
import net.pms.util.OpenSubtitle.SubtitleItem;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubSelFile extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubSelFile.class);
	private final DLNAResource originalResource;

	public SubSelFile(DLNAResource resource) {
		super(resource.getDisplayNameBase(resource.configuration), null);
		originalResource = resource;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		try {
			return originalResource.getThumbnailInputStream();
		} catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}

	@Override
	public void checkThumbnail() {
		originalResource.checkThumbnail();
	}

	@Override
	public void discoverChildren() {
		ArrayList<SubtitleItem> subtitleItems;
		RealFile rf = null;
		try {
			if (originalResource instanceof RealFile) {
				rf = (RealFile) originalResource;
				subtitleItems = OpenSubtitle.findSubtitles(rf, getDefaultRenderer()); //TODO: (Nad) Temp test
//				subtitleItems = OpenSubtitle.findSubs(rf.getFile(), getDefaultRenderer());
			} else {
				subtitleItems = OpenSubtitle.querySubs(originalResource.getDisplayNameBase(configuration), getDefaultRenderer());
			}
		} catch (IOException e) {
			LOGGER.error("Could not get live subtitles for \"{}\": {}", getName(), e.getMessage());
			LOGGER.trace("", e);
			return;
		}
		if (subtitleItems == null || subtitleItems.isEmpty()) {
			return;
		}
		Collections.sort(subtitleItems, new SubSort(getDefaultRenderer()));
		reduceSubtitles(subtitleItems, configuration.getLiveSubtitlesLimit());
		for (SubtitleItem subtitleItem : subtitleItems) {
			LOGGER.debug("Added live subtitles child \"{}\" for {}", subtitleItem.getSubFileName(), originalResource);
			DLNAMediaOpenSubtitle subtitle = new DLNAMediaOpenSubtitle(subtitleItem);
			DLNAResource liveResource = originalResource.clone();
			if (liveResource.getMedia() != null) {
				liveResource.getMedia().getSubtitleTracksList().clear();
				liveResource.getMedia().getSubtitleTracksList().add(subtitle);
			}
			liveResource.setMediaSubtitle(subtitle);
			liveResource.resetSubtitlesStatus();
			addChild(liveResource);
		}
	}

	private static int removeLanguage(ArrayList<SubtitleItem> subtitles, LinkedHashMap<String, Integer> languages, String languageCode, int count) {
		ListIterator<SubtitleItem> iterator = subtitles.listIterator(subtitles.size());
		int removed = 0;
		while (removed != count && iterator.hasPrevious()) {
			SubtitleItem subtitle = iterator.previous();
			if (
				(
					languageCode == null &&
					subtitle.getLanguageCode() == null
				) ||
				(
					languageCode != null &&
					languageCode.equals(subtitle.getLanguageCode())
				)
			) {
				Integer langCount = languages.get(languageCode);
				if (langCount != null) {
					if (langCount.intValue() == 1) {
						languages.remove(languageCode);
					} else {
						languages.put(languageCode, langCount.intValue() - 1);
					}
				}
				iterator.remove();
				removed++;
			}
		}
		return removed;
	}

	private static void reduceSubtitles(ArrayList<SubtitleItem> subtitles, int limit) {
		int remove = subtitles.size() - limit;
		if (remove <= 0) {
			return;
		}

		LinkedHashMap<String, Integer> languages = new LinkedHashMap<>();
		for (SubtitleItem subtitle : subtitles) {
			String languageCode = isBlank(subtitle.getLanguageCode()) ? null : subtitle.getLanguageCode();
			if (!languages.containsKey(languageCode)) {
				languages.put(languageCode, 1);
			} else {
				languages.put(languageCode, languages.get(languageCode).intValue() + 1);
			}
		}

		// Remove those without a specified language first
		if (languages.containsKey(null)) {
			remove -= removeLanguage(subtitles, languages, null, remove);
		}
		// Remove the lowest ranked from each language proportionally, rounded down
		if (remove > 0) {
			double removeFactor = (double) remove / subtitles.size();
			HashMap<String, Integer> languagesCopy = new HashMap<>(languages);
			for (Entry<String, Integer> entry : languagesCopy.entrySet()) {
				remove -= removeLanguage(subtitles, languages, entry.getKey(), (int) (entry.getValue() * removeFactor));
			}
			// Remove the lowest ranked from one language at the time until satisfied
			while (remove > 0) {
				String[] languagesList = languages.keySet().toArray(new String[languages.size()]);
				for (int i = languagesList.length - 1; i >= 0; i--) {
					remove -= removeLanguage(subtitles, languages, languagesList[i], 1);
					if (remove == 0) {
						break;
					}
				}
			}
		}
	}

	private static class SubSort implements Comparator<SubtitleItem>, Serializable {
		private static final long serialVersionUID = 1L;
		private List<String> configuredLanguages;

		SubSort(RendererConfiguration renderer) {
			configuredLanguages = Arrays.asList(UMSUtils.getLangList(renderer, true).split(","));
		}

		@Override
		public int compare(SubtitleItem o1, SubtitleItem o2) {
			if (o1 == null) {
				return o2 == null ? 0 : 1;
			}
			if (o2 == null) {
				return -1;
			}

			// Compare languages with configured priority
			if (o1.getLanguageCode() == null) {
				if (o2.getLanguageCode() != null) {
					return 1;
				}
			} else if (o2.getLanguageCode() == null) {
				return -1;
			} else {
				int index1 = configuredLanguages.indexOf(o1.getLanguageCode());
				int index2 = configuredLanguages.indexOf(o2.getLanguageCode());
				if (index1 < 0) {
					if (index2 >= 0) {
						return 1;
					}
				} else if (index2 < 0) {
					return -1;
				} else if (index1 != index2) {
					return index1 - index2;
				}
			}
			// Prefer match by moviehash, then imdbid
			boolean o1MovieHash = "moviehash".equals(o1.getMatchedBy());
			boolean o2MovieHash = "moviehash".equals(o2.getMatchedBy());
			if (o1MovieHash) {
				if (!o2MovieHash) {
					return -1;
				}
			} else if (o2MovieHash) {
				return 1;
			}
			boolean o1ImdbID = "imdbid".equals(o1.getMatchedBy());
			boolean o2ImdbID = "imdbid".equals(o2.getMatchedBy());
			if (o1ImdbID) {
				if (!o2ImdbID) {
					return -1;
				}
			} else if (o2ImdbID) {
				return 1;
			}
			// Compare score if match isn't moviehash or imdbid
			if (!o1MovieHash && !o1ImdbID) {
				if (Double.isNaN(o1.getScore()) || Double.compare(o1.getScore(), 0.0) < 0) {
					if (!Double.isNaN(o2.getScore()) && Double.compare(o2.getScore(), 0.0) >= 0) {
						return 1;
					}
				} else if (Double.isNaN(o2.getScore()) || Double.compare(o2.getScore(), 0.0) < 0) {
					return -1;
				} else {
					int scoreDiff = (int) (o2.getScore() - o1.getScore());
					if (scoreDiff >= 5) {
						// Only use score if the difference is 5 or more
						return scoreDiff;
					}
				}
			}
			if (o1.isSubBad()) {
				if (!o2.isSubBad()) {
					return 1;
				}
			} else if (o2.isSubBad()) {
				return -1;
			}
			if (o1.isSubFromTrusted()) {
				if (!o2.isSubFromTrusted()) {
					return -1;
				}
			} else if (o2.isSubFromTrusted()) {
				return 1;
			}
			// Compare subtitle rating
			if (Double.isNaN(o1.getSubRating()) || Double.compare(o1.getSubRating(), 0.0) < 0) {
				if (!Double.isNaN(o2.getSubRating()) && Double.compare(o2.getSubRating(), 0.0) >= 0) {
					return 1;
				}
			} else if (Double.isNaN(o2.getSubRating()) || Double.compare(o2.getSubRating(), 0.0) < 0) {
				return -1;
			} else {
				return (int) (o2.getScore() - o1.getScore());
			}
			return 0;
		}
	}

	@Override
	protected String getDisplayNameSuffix(RendererConfiguration renderer, PmsConfiguration configuration) {
		return "{" + Messages.getString("Subtitles.LiveSubtitles") + "}";
	}
}
