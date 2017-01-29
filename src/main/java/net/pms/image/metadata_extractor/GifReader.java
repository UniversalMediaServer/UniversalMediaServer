/*
 * Copyright 2002-2017 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */
package net.pms.image.metadata_extractor;

import com.drew.lang.ByteArrayReader;
import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.icc.IccReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.pms.image.metadata_extractor.GifControlDirectory.DisposalMethod;

/**
 * This is a modified version of {@link com.drew.metadata.gif.GifReader} which
 * can read more GIF metadata. All extra functionality here has been merged into
 * the project, so when the next version is available, this class can be
 * removed.
 *
 * Reader of GIF encoded data.
 *
 * Resources:
 * <ul>
 * <li>https://wiki.whatwg.org/wiki/GIF</li>
 * <li>https://www.w3.org/Graphics/GIF/spec-gif89a.txt</li>
 * <li>http://web.archive.org/web/20100929230301/http://www.etsimo.uniovi.es/
 * gifanim/gif87a.txt</li>
 * </ul>
 *
 * @author Drew Noakes https://drewnoakes.com
 * @author Kevin Mott https://github.com/kwhopper
 */
public class GifReader
{
    private static final String GIF_87A_VERSION_IDENTIFIER = "87a";
    private static final String GIF_89A_VERSION_IDENTIFIER = "89a";

    @SuppressFBWarnings("DE_MIGHT_IGNORE")
    public void extract(@NotNull final StreamReader reader, final @NotNull Metadata metadata) throws IOException
    {
        reader.setMotorolaByteOrder(false);

        GifHeaderDirectory header;
        header = readGifHeader(reader);
        metadata.addDirectory(header);

        if(header.hasErrors())
            return;

        // Skip over any global colour table
        Integer globalColorTableSize = header.getInteger(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE);
        if (globalColorTableSize != null)
        {
            // Colour table has R/G/B byte triplets
            reader.skip(3 * globalColorTableSize);
        }

        // After the header comes a sequence of blocks
        while (true)
        {
            byte marker;
            try {
                marker = reader.getInt8();
            } catch (IOException ex) {
                return;
            }

            switch (marker)
            {
                case (byte)'!': // 0x21
                {
                    readGifExtensionBlock(reader, metadata);
                    break;
                }
                case (byte)',': // 0x2c
                {
                    readImageBlock(reader);

                    // skip image data blocks
                    skipBlocks(reader);
                    break;
                }
                case (byte)';': // 0x3b
                {
                    // terminator
                    return;
                }
                default:
                {
                    // Anything other than these types is unexpected.
                    // GIF87a spec says to keep reading until a separator is found.
                    // GIF89a spec says file is corrupt.
                    return;
                }
            }
        }
    }

    private static GifHeaderDirectory readGifHeader(@NotNull final StreamReader reader) throws IOException
    {
        // FILE HEADER
        //
        // 3 - signature: "GIF"
        // 3 - version: either "87a" or "89a"
        //
        // LOGICAL SCREEN DESCRIPTOR
        //
        // 2 - pixel width
        // 2 - pixel height
        // 1 - screen and color map information flags (0 is LSB)
        //       0-2  Size of the global color table
        //       3    Color table sort flag (89a only)
        //       4-6  Color resolution
        //       7    Global color table flag
        // 1 - background color index
        // 1 - pixel aspect ratio

        GifHeaderDirectory headerDirectory = new GifHeaderDirectory();

        String signature = reader.getString(3);

        if (!signature.equals("GIF"))
        {
            headerDirectory.addError("Invalid GIF file signature");
            return headerDirectory;
        }

        String version = reader.getString(3);

        if (!version.equals(GIF_87A_VERSION_IDENTIFIER) && !version.equals(GIF_89A_VERSION_IDENTIFIER)) {
            headerDirectory.addError("Unexpected GIF version");
            return headerDirectory;
        }

        headerDirectory.setString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION, version);

        // LOGICAL SCREEN DESCRIPTOR

        headerDirectory.setInt(GifHeaderDirectory.TAG_IMAGE_WIDTH, reader.getUInt16());
        headerDirectory.setInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT, reader.getUInt16());

        short flags = reader.getUInt8();

        // First three bits = (BPP - 1)
        int colorTableSize = 1 << ((flags & 7) + 1);
        int bitsPerPixel = ((flags & 0x70) >> 4) + 1;
        boolean hasGlobalColorTable = (flags & 0xf) != 0;

        headerDirectory.setInt(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE, colorTableSize);

        if (version.equals(GIF_89A_VERSION_IDENTIFIER)) {
            boolean isColorTableSorted = (flags & 8) != 0;
            headerDirectory.setBoolean(GifHeaderDirectory.TAG_IS_COLOR_TABLE_SORTED, isColorTableSorted);
        }

        headerDirectory.setInt(GifHeaderDirectory.TAG_BITS_PER_PIXEL, bitsPerPixel);
        headerDirectory.setBoolean(GifHeaderDirectory.TAG_HAS_GLOBAL_COLOR_TABLE, hasGlobalColorTable);

        headerDirectory.setInt(GifHeaderDirectory.TAG_BACKGROUND_COLOR_INDEX, reader.getUInt8());

        int aspectRatioByte = reader.getUInt8();
        if (aspectRatioByte != 0) {
            float pixelAspectRatio = (float)((aspectRatioByte + 15d) / 64d);
            headerDirectory.setFloat(GifHeaderDirectory.TAG_PIXEL_ASPECT_RATIO, pixelAspectRatio);
        }

        return headerDirectory;
    }

    private static void readGifExtensionBlock(StreamReader reader, Metadata metadata) throws IOException
    {
        byte extensionLabel = reader.getInt8();
        short blockSizeBytes = reader.getUInt8();
        long blockStartPos = reader.getPosition();

        switch (extensionLabel)
        {
            case (byte) 0x01:
                Directory plainTextBlock = readPlainTextBlock(reader, blockSizeBytes);
                if (plainTextBlock != null)
                    metadata.addDirectory(plainTextBlock);
                break;
            case (byte) 0xf9:
                metadata.addDirectory(readControlBlock(reader, blockSizeBytes));
                break;
            case (byte) 0xfe:
                readCommentBlock(reader, blockSizeBytes);
                break;
            case (byte) 0xff:
                readApplicationExtensionBlock(reader, blockSizeBytes, metadata);
                break;
            default:
            	throw new IOException(String.format("Unsupported GIF extension block with type 0x%02X.", extensionLabel));
        }

        long skipCount = blockStartPos + blockSizeBytes - reader.getPosition();
        if (skipCount > 0)
            reader.skip(skipCount);
    }

    @Nullable
    private static Directory readPlainTextBlock(StreamReader reader, int blockSizeBytes) throws IOException
    {
        // It seems this extension is deprecated. If somebody finds an image with this in it, could implement here.
        // Just skip the entire block for now.

        if (blockSizeBytes != 12)
        	throw new IOException(String.format("Invalid GIF plain text block size. Expected 12, got %d.", blockSizeBytes));

        // skip 'blockSizeBytes' bytes
        reader.skip(12);

        // keep reading and skipping until a 0 byte is reached
        skipBlocks(reader);

        return null;
    }

    private static void readCommentBlock(StreamReader reader, int blockSizeBytes) throws IOException
    {
    	// Just skip, not implemented here
        gatherBytes(reader, blockSizeBytes);
    }

    private static void readApplicationExtensionBlock(StreamReader reader, int blockSizeBytes, Metadata metadata) throws IOException
    {
        if (blockSizeBytes != 11)
        {
        	throw new IOException(String.format("Invalid GIF application extension block size. Expected 11, got %d.", blockSizeBytes));
        }

        String extensionType = reader.getString(blockSizeBytes, "UTF-8");

        if (extensionType.equals("XMP DataXMP"))
        {
            // XMP data extension, skip
            gatherBytes(reader);
        }
        else if (extensionType.equals("ICCRGBG1012"))
        {
            // ICC profile extension
            byte[] iccBytes = gatherBytes(reader, reader.getByte());
            if (iccBytes.length != 0)
                new IccReader().extract(new ByteArrayReader(iccBytes), metadata);
        }
        else if (extensionType.equals("NETSCAPE2.0"))
        {
        	// Not implemented here, skip
            reader.skip(5);
            // Netscape's animated GIF extension
            // Iteration count (0 means infinite)
        }
        else
        {
            skipBlocks(reader);
        }
    }

    private static GifControlDirectory readControlBlock(StreamReader reader, int blockSizeBytes) throws IOException
    {
        if (blockSizeBytes < 4)
            blockSizeBytes = 4;

        GifControlDirectory directory = new GifControlDirectory();

        short packedFields = reader.getUInt8();
        directory.setObject(GifControlDirectory.TAG_DISPOSAL_METHOD, DisposalMethod.typeOf((packedFields >> 2) & 7));
        directory.setBoolean(GifControlDirectory.TAG_USER_INPUT_FLAG, (packedFields & 2) >> 1 == 1 ? true : false);
        directory.setBoolean(GifControlDirectory.TAG_TRANSPARENT_COLOR_FLAG, (packedFields & 1) == 1 ? true : false);
        directory.setInt(GifControlDirectory.TAG_DELAY, reader.getUInt16());
        directory.setInt(GifControlDirectory.TAG_TRANSPARENT_COLOR_INDEX, reader.getUInt8());

        // skip 0x0 block terminator
        reader.skip(1);

        return directory;
    }

    private static void readImageBlock(StreamReader reader) throws IOException
    {
    	// GifImageDirectory is not available in this version, so just skip the block
    	reader.skip(8);

        byte flags = (byte) reader.getUInt8();
        boolean hasColorTable = (flags & 0x7) != 0;

        if (hasColorTable)
        {
            // skip color table
            reader.skip(3 * (2 << (flags & 0x7)));
        }

        // skip "LZW Minimum Code Size" byte
        reader.skip(1);

        return;
    }

    private static byte[] gatherBytes(StreamReader reader) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[257];

        while (true)
        {
            byte b = reader.getByte();
            if (b == 0)
                return bytes.toByteArray();

            int bInt = b & 0xFF;

            buffer[0] = b;
            reader.getBytes(buffer, 1, bInt);
            bytes.write(buffer, 0, bInt + 1);
        }
    }

    private static byte[] gatherBytes(StreamReader reader, int firstLength) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int length = firstLength;

        while (length > 0)
        {
            buffer.write(reader.getBytes(length), 0, length);

            length = reader.getByte();
        }

        return buffer.toByteArray();
    }

    private static void skipBlocks(StreamReader reader) throws IOException
    {
        while (true)
        {
            short length = reader.getUInt8();

            if (length == 0)
                return;

            reader.skip(length);
        }
    }
}
