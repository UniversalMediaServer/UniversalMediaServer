package net.pms.database.lucene;

import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_ALL;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Analyzer for filename field. It splits the filename into parts and generates
 * word parts and number parts, catenates all parts and splits on case change.
 */
public class UmsAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		StandardTokenizer src = new StandardTokenizer();
		TokenStream result = new LowerCaseFilter(src);
		result = new WordDelimiterGraphFilter(result, GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE, null);
		return new TokenStreamComponents(src, result);
	}
}
