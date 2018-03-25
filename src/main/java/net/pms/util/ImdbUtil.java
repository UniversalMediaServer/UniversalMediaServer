package net.pms.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ImdbUtil {

	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;
	private static final String HASH_REG = "_os([^_]+)_";
	private static final String IMDB_REG = "_imdb([^_]+)_";

	public static String cleanName(String str) {
		return str.replaceAll(IMDB_REG, "").replaceAll(HASH_REG, "");
	}

	public static String extractOSHashFromFileName(File file) {
		return extract(file, HASH_REG);
	}

	public static String extractImdbIdFromFileName(File file) {
		String ret = extract(file, IMDB_REG);
		// Opensubtitles requires IMDb ID to be a number only
		if (!StringUtils.isEmpty(ret) && ret.startsWith("tt") && ret.length() > 2) {
			ret = ret.substring(2);
		}
		return ret;
	}

	private static String extract(File file, String regex) {
		String fileName = file.getAbsolutePath();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(fileName);
		String result = "";
		while (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	public static String ensureTT(String s) {
		return (s.startsWith("tt") ? s : "tt" + s);
	}

	public static String computeHashInputStream(File file) throws IOException {
		long size = file.length();
		FileInputStream fis = new FileInputStream(file);
		return computeHashInputStream(fis, size);
	}

	public static String computeHashInputStream(InputStream inputStream, long length) throws IOException {
		int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, length);

		// Buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
		byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, length)];
		long head;
		long tail;
		try (DataInputStream in = new DataInputStream(inputStream)) {
			// First chunk
			in.readFully(chunkBytes, 0, chunkSizeForFile);

			long position = chunkSizeForFile;
			long tailChunkPosition = length - chunkSizeForFile;

			// Seek to position of the tail chunk, or not at all if length is smaller than two chunks
			while (position < tailChunkPosition && (position += in.skip(tailChunkPosition - position)) >= 0) {
				;
			}

			// Second chunk, or the rest of the data if length is smaller than two chunks
			in.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);

			head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
			tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));
		}
		return String.format("%016x", length + head + tail);
	}

	public static String computeHashRandomAccessFile(File file) throws IOException {
		long size = file.length();
		int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, size);

		// Buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
		byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, size)];
		long head;
		long tail;
		try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
			// First chunk
			raFile.readFully(chunkBytes, 0, chunkSizeForFile);

			raFile.seek(size - chunkSizeForFile);

			// Second chunk, or the rest of the data if length is smaller than two chunks
			raFile.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);

			head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
			tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));
		}
		return String.format("%016x", size + head + tail);
	}

	/**
	 * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
	 * checksum of the first and last 64k (even if they overlap because the file is smaller than
	 * 128k).
	 *
	 * @see http://trac.opensubtitles.org/projects/opensubtitles/wiki/HashSourceCodes#Java
	 * @param file the file to calculate the hash of
	 * @return an OpenSubtitles/MPC-style hash of the file
	 * @throws IOException
	 */
	public static String computeHashFileChannel(File file) throws IOException {
		long size = file.length();
		long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);


		try (
			FileInputStream fileInputStream = new FileInputStream(file);
			FileChannel fileChannel = fileInputStream.getChannel()
		) {
			long head = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
			long tail = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));

			return String.format("%016x", size + head + tail);
		}
	}

	public static String computeHashFileChannel(Path file) throws IOException {
		long size = Files.size(file);
		long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

		try (FileChannel fileChannel = FileChannel.open(file)) {
			long head = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
			long tail = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));

			return String.format("%016x", size + head + tail);
		}
	}

	private static long computeHashForChunk(ByteBuffer buffer) {
		LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
		long hash = 0;

		while (longBuffer.hasRemaining()) {
			hash += longBuffer.get();
		}

		return hash;
	}

}
