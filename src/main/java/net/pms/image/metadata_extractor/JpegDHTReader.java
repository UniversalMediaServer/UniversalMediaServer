package net.pms.image.metadata_extractor;

import com.drew.imaging.jpeg.JpegSegmentMetadataReader;
import com.drew.imaging.jpeg.JpegSegmentType;
import com.drew.lang.SequentialByteArrayReader;
import com.drew.lang.SequentialReader;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Metadata;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import net.pms.image.metadata_extractor.HuffmanTablesDirectory.HuffmanTable;
import net.pms.image.metadata_extractor.HuffmanTablesDirectory.HuffmanTable.HuffmanTableClass;

/**
 * Reader for JPEG Huffman tables, found in the DHT JPEG segment.
 *
 * @author Nadahar
 */
public class JpegDHTReader implements JpegSegmentMetadataReader
{
	@NotNull
	public Iterable<JpegSegmentType> getSegmentTypes()
	{
		return Collections.singletonList(JpegSegmentType.DHT);
	}

	public void readJpegSegments(@NotNull Iterable<byte[]> segments, @NotNull Metadata metadata, @NotNull JpegSegmentType segmentType)
	{
		for (byte[] segmentBytes : segments) {
			extract(new SequentialByteArrayReader(segmentBytes), metadata);
		}
	}

	/**
	 * Performs the DHT tables extraction, adding found tables to the specified
	 * instance of {@link Metadata}.
	 */
	public void extract(@NotNull final SequentialReader reader, @NotNull final Metadata metadata)
	{
		HuffmanTablesDirectory directory = metadata.getFirstDirectoryOfType(HuffmanTablesDirectory.class);
		if (directory == null) {
			directory = new HuffmanTablesDirectory();
			metadata.addDirectory(directory);
		}

		try {
			do {
				byte header;
				try {
					header = (byte) reader.getUInt8();
				} catch (EOFException e) {
					// This segment contains no more tables
					break;
				}
				HuffmanTableClass tableClass = HuffmanTableClass.typeOf((header & 0xF0) >> 4);
				int tableDestinationId = header & 0xF;

				byte[] lBytes = getBytes(reader, 16);
				int vCount = 0;
				for (byte b : lBytes) {
					vCount += (0xFF & b);
				}
				byte[] vBytes = getBytes(reader, vCount);
				directory.getTables().add(new HuffmanTable(tableClass, tableDestinationId, lBytes, vBytes));
			} while (true);
		} catch (IOException me) {
			directory.addError(me.getMessage());
		}

		directory.setInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES, directory.getTables().size());
	}

	private byte[] getBytes(@NotNull final SequentialReader reader, int count) throws IOException {
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			byte b = (byte) reader.getUInt8();
			if ((0xFF & b) == 0xFF) {
				byte stuffing = (byte) reader.getUInt8();
				if (stuffing != 0x00) {
					throw new IOException("Marker " + JpegSegmentType.fromByte(stuffing) + " found inside DHT segment");
				}
			}
			bytes[i] = b;
		}
		return bytes;
	}
}
