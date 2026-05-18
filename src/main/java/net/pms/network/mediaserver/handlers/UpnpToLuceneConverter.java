package net.pms.network.mediaserver.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpnpToLuceneConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpnpToLuceneConverter.class.getName());

	private List<String> tokens = new ArrayList<>();
	private int pos = 0;
	private final Map<String, String> fieldMap;

	public UpnpToLuceneConverter(String input, Map<String, String> mapping) {
		this.fieldMap = mapping;
		// Tokenizer: quoted strings with UPnP escaped quotes (""), OPS (!=, <=, etc.), parentheses, and words
		String regex = "\"(?:[^\"]|\"\")*\"|[!=<>]{1,2}|\\(|\\)|[a-zA-Z0-9_:@\\.\\-\\[\\]\\\"\\=]+|\\S+";

		Matcher m = Pattern.compile(regex).matcher(input);
		while (m.find()) {
			tokens.add(m.group());
		}
	}

	public String convert() {
		if (tokens.isEmpty()) {
			LOGGER.warn("no tokens found in input");
			return "";
		}
		Node root = parseExpression();
		String result = root.toLucene().trim();
		if (result.isEmpty()) {
			LOGGER.warn("parsed expression is empty");
		}
		return result.isEmpty() ? "" : result;
	}

	private Node parseExpression() {
		try {
			Node node = parseTerm();
			while (pos < tokens.size() && (tokens.get(pos).equalsIgnoreCase("AND") || tokens.get(pos).equalsIgnoreCase("OR"))) {
				String op = tokens.get(pos++).toUpperCase();

				if (pos >= tokens.size()) {
					LOGGER.warn("orphan operator {}", op);
					break;
				}

				node = new BoolNode(op, node, parseTerm());
			}
			return node;
		} catch (Exception e) {
			LOGGER.error("Error parsing expression at token '{}'}", pos < tokens.size() ? tokens.get(pos) : "EOF", e);
			return new EmptyNode();
		}
	}

	private Node parseTerm() {
		if (pos >= tokens.size()) {
			return new EmptyNode();
		}

		// NOT handling
		if (tokens.get(pos).equalsIgnoreCase("NOT")) {
			pos++;
			return new NotNode(parseTerm());
		}

		if (tokens.get(pos).equals("(")) {
			pos++;
			Node node = parseExpression();

			if (pos < tokens.size() && tokens.get(pos).equals(")")) {
				pos++;
			} else {
				LOGGER.warn("missing ) at pos {}", pos);
			}
			return node;
		}

		// field operator
		if (pos + 2 < tokens.size()) {
			String rawField = tokens.get(pos++);
			String op = tokens.get(pos++);
			String rawVal = tokens.get(pos++);
			String val = rawVal;
			if (rawVal.length() >= 2 && rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
				val = rawVal.substring(1, rawVal.length() - 1).replace("\"\"", "\"");
			}

			// Mapping-Logik
			if (fieldMap.containsKey(rawField)) {
				String mapped = fieldMap.get(rawField);
				if (mapped == null) {
					return new EmptyNode();
				}
				return new TermNode(mapped, op, val);
			}
			return new TermNode(rawField, op, val);
		} else {
			LOGGER.warn("incomplete token {}", tokens.get(pos));
			pos = tokens.size();
			return new EmptyNode();
		}
	}
}