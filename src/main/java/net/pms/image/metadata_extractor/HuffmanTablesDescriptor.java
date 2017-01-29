package net.pms.image.metadata_extractor;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.TagDescriptor;
import static net.pms.image.metadata_extractor.HuffmanTablesDirectory.*;

/**
 * Provides a human-readable string version of the tag stored in a HuffmanTableDirectory.
 *
 * <ul>
 *   <li>https://en.wikipedia.org/wiki/Huffman_coding</li>
 *   <li>http://stackoverflow.com/a/4954117</li>
 * </ul>
 *
 * @author Nadahar
 */
public class HuffmanTablesDescriptor extends TagDescriptor<HuffmanTablesDirectory>
{
	public HuffmanTablesDescriptor(@NotNull HuffmanTablesDirectory directory)
	{
		super(directory);
	}

	@Override
	@Nullable
	public String getDescription(int tagType)
	{
		switch (tagType) {
			case TAG_NUMBER_OF_TABLES:
				return getNumberOfTablesDescription();
			default:
				return super.getDescription(tagType);
		}
	}

	@Nullable
	public String getNumberOfTablesDescription()
	{
		Integer value = _directory.getInteger(TAG_NUMBER_OF_TABLES);
		if (value==null)
			return null;
		return value + (value == 1 ? " Huffman table" : " Huffman tables");
	}
}
