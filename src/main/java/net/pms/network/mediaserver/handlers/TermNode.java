package net.pms.network.mediaserver.handlers;

import java.util.regex.Pattern;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;

class TermNode extends Node {

	private static final Logger LOGGER = LoggerFactory.getLogger(TermNode.class.getName());
	private static UmsConfiguration umsConfiguration;
	private static final Pattern LUCENE_PATTERN = Pattern.compile("([-+&|!(){}\\[\\]^\"~*?:/\\\\])");

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
		value = LUCENE_PATTERN.matcher(value).replaceAll("\\\\$1");
		value = value.replace("'", "''");
		switch (operator.toLowerCase()) {
			case "contains":
				if (getUmsConfiguration().getLuceneContainsFuzzySearch()) {
					value = prepareLuceneSearch(value, "~2");
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
