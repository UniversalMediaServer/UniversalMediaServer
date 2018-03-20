package net.pms.dlna;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
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
		ArrayList<SubtitleItem> subtitleItems = OpenSubtitle.findSubtitles(originalResource, getDefaultRenderer());
		if (subtitleItems == null || subtitleItems.isEmpty()) {
			return;
		}
		Collections.sort(subtitleItems, new SubSort(getDefaultRenderer()));
		reduceSubtitles(subtitleItems, configuration.getLiveSubtitlesLimit());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Discovery of OpenSubtitles subtitles for \"{}\" resulted in the following after sorting and reduction:\n{}",
				getName(),
				OpenSubtitle.toLogString(subtitleItems, 2)
			);
		}
		for (SubtitleItem subtitleItem : subtitleItems) {
			LOGGER.debug("Adding live subtitles child \"{}\" for {}", subtitleItem.getSubFileName(), originalResource);
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

		if (remove > 0) {
			// Build a sorted map where each language gets 30 extra points per step in the priority
			ArrayList<LanguageRankedItem> languageRankedSubtitles = new ArrayList<>();
			int languagePreference = 0;
			String languageCode = null;
			for (SubtitleItem subtitle : subtitles) {
				if (languageCode == null) {
					languageCode = subtitle.getLanguageCode();
					languagePreference = languages.size() * 30;
				} else if (!languageCode.equals(subtitle.getLanguageCode())) {
					languageCode = subtitle.getLanguageCode();
					languagePreference -= 30;
				}
				languageRankedSubtitles.add(new LanguageRankedItem(
					subtitle.getScore() + languagePreference,
					subtitle
				));
			}
			Collections.sort(languageRankedSubtitles);

			// Remove the entries with the lowest score
			for (LanguageRankedItem languageRankedItem : languageRankedSubtitles) {
				if (remove <= 0) {
					break;
				}
				if (subtitles.remove(languageRankedItem.subtitleItem)) {
					remove--;
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
			// Compare score
			if (Double.isNaN(o1.getScore()) || Double.compare(o1.getScore(), 0.0) < 0) {
				if (!Double.isNaN(o2.getScore()) && Double.compare(o2.getScore(), 0.0) >= 0) {
					return 1;
				}
			} else if (Double.isNaN(o2.getScore()) || Double.compare(o2.getScore(), 0.0) < 0) {
				return -1;
			} else {
				int scoreDiff = (int) (o2.getScore() - o1.getScore());
				if (Math.abs(scoreDiff) >= 1) {
					// Only use score if the difference is 1 or more
					return scoreDiff;
				}
			}
			// If scores are equal, use subtitle metadata
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
			double o1Value = Double.isNaN(o1.getSubRating()) ? 0.0 : o1.getSubRating();
			double o2Value = Double.isNaN(o2.getSubRating()) ? 0.0 : o2.getSubRating();
			int result = Double.compare(o1Value, o2Value);
			if (result != 0) {
				return result;
			}
			// Compare OpenSubtitles rating
			o1Value = Double.isNaN(o1.getOpenSubtitlesScore()) ? 0.0 : o1.getOpenSubtitlesScore();
			o2Value = Double.isNaN(o2.getOpenSubtitlesScore()) ? 0.0 : o2.getOpenSubtitlesScore();
			result = Double.compare(o1Value, o2Value);
			if (result != 0) {
				return result;
			}
			// This will probably never happen, but if everything else is equal use the fractional score value
			return (int) Math.signum(o2.getScore() - o1.getScore());
		}
	}

	@Override
	protected String getDisplayNameSuffix(RendererConfiguration renderer, PmsConfiguration configuration) {
		return "{" + Messages.getString("Subtitles.LiveSubtitles") + "}";
	}

	private static class LanguageRankedItem implements Comparable<LanguageRankedItem> {
		private final Double score;
		private final SubtitleItem subtitleItem;

		public LanguageRankedItem(double score, SubtitleItem subtitleItem) {
			this.score = Double.valueOf(score);
			this.subtitleItem = subtitleItem;
		}

		@Override
		public String toString() {
			return "[score=" + score + ", subtitleItem=" + subtitleItem + "]";
		}

		@Override
		public int compareTo(LanguageRankedItem o) {
			if (score == null || score.isNaN()) {
				if (o.score != null && !o.score.isNaN()) {
					return 1;
				}
			} else if (o.score == null || o.score.isNaN()) {
				return -1;
			} else {
				int result = score.compareTo(o.score);
				if (result != 0) {
					return result;
				}
			}
			if (subtitleItem == null) {
				if (o.subtitleItem != null) {
					return 1;
				}
			} else if (o.subtitleItem == null) {
				return -1;
			}
			return o.hashCode() - hashCode();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((score == null) ? 0 : score.hashCode());
			result = prime * result + ((subtitleItem == null) ? 0 : subtitleItem.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof LanguageRankedItem)) {
				return false;
			}
			LanguageRankedItem other = (LanguageRankedItem) obj;
			if (score == null) {
				if (other.score != null) {
					return false;
				}
			} else if (!score.equals(other.score)) {
				return false;
			}
			if (subtitleItem == null) {
				if (other.subtitleItem != null) {
					return false;
				}
			} else if (!subtitleItem.equals(other.subtitleItem)) {
				return false;
			}
			return true;
		}
	}
}
