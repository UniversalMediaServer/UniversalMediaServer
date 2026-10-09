package net.pms.network.mediaserver.handlers;

import java.util.regex.Pattern;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;

class TermNode extends Node {

	private static final Logger LOGGER = LoggerFactory.getLogger(TermNode.class.getName());
	private static UmsConfiguration umsConfiguration;
	private static final Pattern LUCENE_PATTERN = Pattern.compile("([-+&|!(){}\\[\\]^\"~*?:/\\\\])");

	/** Below this, a prefix matches too much of the index to be worth adding. */
	private static final int MIN_WILDCARD_LENGTH = 3;

	String field, operator, value;

	TermNode(String f, String o, String v) {
		this.field = f;
		this.operator = o;
		this.value = v;
	}

	/**
	 * Attention: If a clients asks for an exact match or proximity search, it
	 * should put '"' around the search term. Example : dc:title contains "dark
	 * side of the moon"!
	 *
	 * @param list
	 * @return
	 */
	@Override
	public String toLucene() {
		if (value.startsWith("\"") && value.endsWith("\"")) {
			value = "\"" + LUCENE_PATTERN.matcher(value.substring(1, value.length() - 1)).replaceAll("\\\\$1") + "\"";
		} else {
			value = LUCENE_PATTERN.matcher(value).replaceAll("\\\\$1");
		}
		value = value.replace("'", "''");
		switch (operator.toLowerCase()) {
			case "contains":
				if (getUmsConfiguration().getLuceneContainsFuzzySearch()) {
					value = prepareLuceneContains(value);
				} else {
					if (!(value.startsWith("\"") && value.endsWith("\""))) {
						LOGGER.debug("for classic contains logic, title must be between \"\".");
						value = prepareLuceneSearch(value, "*");
					}
				}
				return field + ":" + value;
			case "=":
				if (getUmsConfiguration().getLuceneEqualFuzzySearch()) {
					value = prepareLuceneSearch(value, "~2");
				}
				return field + ":" + value;
			case "!=":
				return "NOT " + field + ":" + value;
			default:
				return field + ":" + value;
		}
	}

	/**
	 * Builds a "contains" the way a type-ahead search box is expected to behave.
	 *
	 * @param title the already escaped search term
	 */
	private String prepareLuceneContains(String title) {
		if (title.startsWith("\"") && title.endsWith("\"")) {
			// A quoted phrase cannot carry a wildcard, so it stays a proximity search.
			return title + "~2";
		}
		StringBuilder query = new StringBuilder();
		for (String word : title.split("\\s+")) {
			if (word.isEmpty()) {
				continue;
			}
			if (query.length() > 0) {
				query.append(" ");
			}
			if (word.length() >= MIN_WILDCARD_LENGTH) {
				query.append("(").append(word).append("* OR ").append(word).append("~2)");
			} else {
				query.append(word).append("~2");
			}
		}
		return query.toString();
	}

	/**
	 * Lucene doesn't support proximity and fuzzy search at the same time. So we
	 * check if the search term is an exact match (between " "). If so, we use
	 * proximity search, else we split the search term into words and add the
	 * fuzzy or wildcard operator to each word.
	 *
	 * I think this is what the user expects most.
	 *
	 * @param title
	 * @param searchType  -> ~2 for fuzzy search, * for wildcard search
	 * @return
	 */
	private String prepareLuceneSearch(String title, String searchType) {
		if (title.startsWith("\"") && title.endsWith("\"")) {
			LOGGER.debug("search request is for an Proximity Search ...");
			if ("*".equals(searchType)) {
				return title;
			}
			return title + searchType;
		}
		String[] words = title.split("\\s+");
		StringBuilder fuzzyQuery = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				if (fuzzyQuery.length() > 0) {
					fuzzyQuery.append(" ");
				}
				fuzzyQuery.append(word).append(searchType);
			}
		}
		return fuzzyQuery.toString();
	}

	protected static UmsConfiguration getUmsConfiguration() {
		return umsConfiguration;
	}

	static {
		try {
			umsConfiguration = new UmsConfiguration();
		} catch (ConfigurationException | InterruptedException e) {
			LOGGER.error("Error while initializing SearchRequestHandler : ", e);
			throw new RuntimeException(e);
		}
	}
}
