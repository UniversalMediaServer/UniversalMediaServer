package net.pms.network.mediaserver.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.network.mediaserver.handlers.BaseSearchRequestHandler.SearchToken;
import net.pms.network.mediaserver.handlers.message.SearchRequest;

public class SearchRequestTokenizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchRequestTokenizer.class.getName());

	protected static final Pattern TOKENIZER_PATTERN = Pattern.compile(
		"(?<property>((dc)|(upnp)):[A-Za-z@\\[\\]\"=]+)\\s+" + "(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>([^\"]|\"\")*)\"",
		Pattern.CASE_INSENSITIVE);

	private SearchRequest requestMessage = null;
	private List<SearchToken> searchTokens = null;

	public SearchRequestTokenizer(SearchRequest requestMessage) {
		this.requestMessage = requestMessage;
		this.searchTokens = calcSearchTokens();
	}

	private List<SearchToken> calcSearchTokens() {
		List<SearchToken> result = new ArrayList<>();
		Matcher matcher = TOKENIZER_PATTERN.matcher(requestMessage.getSearchCriteria());
		while (matcher.find()) {
			result.add(new SearchToken(matcher.group("property"), matcher.group("op"), makeClean(matcher.group("val"))));
		}
		return result;
	}

	private String makeClean(String val) {
		return val.replace("\"\"", "\"");
	}

	/**
	 * Are there any search tokens with dc:title property? If not we search for all items.
	 * @return
	 */
	public boolean hasDcTitleSearch() {
		boolean hasTitle = searchTokens.stream().anyMatch(t -> t.attr().equalsIgnoreCase("dc:title"));
		LOGGER.trace("SearchRequestTokenizer.hasDcTitleSearch: {}", hasTitle);
		return hasTitle;
	}

	public String getDcTitleValue() {
		return searchTokens.stream()
			.filter(t -> t.attr().equalsIgnoreCase("dc:title"))
			.map(SearchToken::val)
			.findFirst()
			.orElse(null);
	}

	public List<SearchToken> getSearchTokens() {
		return searchTokens;
	}

}