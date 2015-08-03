package net.pms.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageFileFormatter {
	private final static Logger LOGGER = LoggerFactory.getLogger(LanguageFileFormatter.class);
	private enum ArgType {NONE, INPUT, OUTPUT};
	private final static String LANGFOLDER = "src/main/resources/i18n/";

	private class LineStruct {
		String line = "";
		String group = "";
		String name = "";
		int num = -1;

		public String toString() {
			return String.format("%s : %d \"%s\"", group, num, line);
		}
	}

	private static class LineComparator implements Comparator<LineStruct> {
		@Override
		public int compare(LineStruct o1, LineStruct o2) {
			int i = o1.group.compareTo(o2.group);
			if (i == 0) {
				i = Integer.valueOf(o1.num).compareTo(Integer.valueOf(o2.num));
				if (i == 0) {
					i = o1.name.compareTo(o2.name);
				}
			}
			return i;
		}

		public static LineComparator getComparator() {
			return new LineComparator();
		}
	}

	public LanguageFileFormatter() {}

	public static void main(String[] args) {
		File inputFile = null;
		File outputFile = null;
		ArgType argType = ArgType.NONE;
		ArrayList<LineStruct> lines = null;
		boolean sort = false;
		boolean indent = false;
		boolean unindent = false;
		boolean deleteSelected = false;

		for (String arg : args) {

			if (arg.startsWith("-")) {
				argType = ArgType.NONE;
				if (arg.equals("-i")) {
					argType = ArgType.INPUT;
				} else if (arg.equals("-o")) {
					argType = ArgType.OUTPUT;
				} else if (arg.equals("-s")) {
					sort = true;
				} else if (arg.equals("-I")) {
					indent = true;
				} else if (arg.equals("-U")) {
					unindent = true;
				} else if (arg.equals("-DEL")) {
					deleteSelected = true;
				} else {
					System.err.println("Invalid argument " + arg);
					System.exit(1);
				}
			} else if (argType != ArgType.NONE) {
				switch (argType) {
					case INPUT:
						if (!arg.toLowerCase().endsWith(".properties")) {
							arg += ".properties";
						}
						if (!arg.equalsIgnoreCase("messages.properties") && !arg.toLowerCase().startsWith("messages_")) {
							arg = "messages_" + arg;
						}
						inputFile = new File(LANGFOLDER + arg);
						break;
					case OUTPUT:
						if (!arg.toLowerCase().endsWith(".properties")) {
							arg += ".properties";
						}
						if (!arg.equalsIgnoreCase("messages.properties") && !arg.toLowerCase().startsWith("messages_")) {
							arg = "messages_" + arg;
						}
						outputFile = new File(LANGFOLDER + arg);
						break;
					default:
						break;
				}
				argType = ArgType.NONE;
			}
		}

		// Verify parameters
		if (indent && !sort) {
			LOGGER.info("Indenting implies sorting, sort enabled");
			sort = true;
		}
		if (indent && unindent) {
			System.err.println("Impossible to indent and unindent at the same time!");
			System.exit(1);
		}

		// Do
		if (inputFile != null) {
			lines = readFile(inputFile);
		}
		if (lines != null) {
			if (sort && lines != null) {
				Collections.sort(lines, LineComparator.getComparator());
			}
			checkDuplicateKeys(lines);

			if (deleteSelected) {
				deleteSelected(lines);
			}

			if (indent) {
				indent(lines);
			}
			if (unindent) {
				unindent(lines);
			}

			if (outputFile != null) {
				writeFile(outputFile,lines);
			} else {
				for (LineStruct line : lines) {
					System.out.println(line.line);
				}
			}
		}
	}

	private static String unicodeToLatin(String s) {
		// Don't touch comment lines
		if (s.matches("\\s*#.*")) {
			return s;
		}

		// Change all existing unicode hex value letters to lower case.
		Pattern pa = Pattern.compile("(?<!\\\\)\\\\u[0-9A-F]{4}");
		Matcher ma = pa.matcher(s);
		while (ma.find()) {
			if (!ma.group(0).toLowerCase().equals(ma.group(0))) {
				s = s.replace(ma.group(0), ma.group(0).toLowerCase());
				ma = pa.matcher(s);
			}
		}

		// Convert unicode characters to codes
		String result = "";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
				result += c;
			} else {
				int cp = Character.codePointAt(s, i);
				i += Character.charCount(cp) - 1;
				result += "\\u" + String.format("%04x", cp);
	        }
		}
		return result;
	}

	private static ArrayList<LineStruct> readFile(File file) {
		if (!file.exists()) {
			System.err.println("Could not open file: " + file.getAbsolutePath());
			return null;
		}
		ArrayList<LineStruct> lineList = new ArrayList<LineStruct>();
		lineList.ensureCapacity(1000);
		LanguageFileFormatter lff = new LanguageFileFormatter();
		try {
			Scanner s = new Scanner(file, "ISO-8859-1");
			try {
				Pattern pa = Pattern.compile("^\\s*(\\w+)\\.(\\w+)");
				while (s.hasNextLine()) {
					LineStruct ls = lff.new LineStruct();
					ls.line = unicodeToLatin(s.nextLine());
					Matcher m = pa.matcher(ls.line);
					if (m.find()) {
						if (m.groupCount() > 0) {
							ls.group = m.group(1);
						}
						if (m.groupCount() > 1) {
							ls.name = m.group(2);
							try {
							ls.num = Integer.valueOf(m.group(2));
							} catch (NumberFormatException e) {
							}
						}
					}
					lineList.add(ls);
				}
			} finally {
				if (s != null) {
					s.close();
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("Error reading file \"{}\": {}", file.getAbsolutePath(), e.getLocalizedMessage());
			return null;
		}
		return lineList;
	}

	private static void writeFile(File file, ArrayList<LineStruct> lines) {
		if (lines == null) {
			return;
		}

		if (file.exists()) {
			LOGGER.error("Output file \"{}\" already exists - aborting write",file.getAbsolutePath());
			return;
		}
		try {
			file.createNewFile();
			try {
				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				try {
					for (LineStruct line : lines) {
						bos.write(line.line.getBytes());
						bos.write(10);
					}
					System.out.println("File \"" + file.getAbsolutePath() + "\" written successfully");
				} finally {
					bos.close();
				}
			} catch (FileNotFoundException e) {
				LOGGER.error("This should not happen: {}", e.getLocalizedMessage());
			}

		} catch (IOException e) {
			LOGGER.error("Could not write to \"{}\": {}", file.getAbsolutePath(), e.getLocalizedMessage());
		}
	}

	private static void indent(ArrayList<LineStruct> lines) {
		if (lines == null) {
			return;
		}

		int i = 0;
		String prevGroup = "";

		while (i < lines.size()) {
			if (lines.get(i).group != prevGroup) {
				prevGroup = lines.get(i).group;
				int j = i;
				int ind = 0;
				while (j < lines.size() && lines.get(j).group.equals(prevGroup)) {
					String line = lines.get(j).line;
					int lineInd = line.indexOf("=");
					if (lineInd > 0) {
						if (line.charAt(lineInd - 1) != ' ') {
							lineInd++;
						}
						ind = Math.max(lineInd,ind);
					} else if (lineInd == 0) {
						LOGGER.error("Can't format line {} because it is invalid: \"{}\"",j + 1,line);
					} else {
						LOGGER.warn("Line {} is missing the equals sign (\"=\"): {}",j + 1,line);
					}
					j++;
				}
				j = i;
				while (j < lines.size() && lines.get(j).group.equals(prevGroup)) {
					String line = lines.get(j).line;
					int lineInd = line.indexOf("=");
					if (lineInd > 0 && lineInd != ind && lines.get(j).line.contains("=")) {
						String[] lineParts = line.split("=", 2);
						if (ind - lineParts[0].trim().length() > -1) {
							lines.get(j).line = StringUtils.rightPad(lineParts[0].trim(), ind) + "= " + lineParts[1].trim();
						} else {
							LOGGER.error("Impossible outcome on line {}: \"{}\"",j + 1, line);
						}
					}
					j++;
				}
				i = j;
			} else {
				i++;
			}
		}
	}

	private static void unindent(ArrayList<LineStruct> lines) {
		if (lines == null) {
			return;
		}

		for (LineStruct line : lines) {
			if (line.line.indexOf("=") > 0) {
				String[] lineParts = line.line.split("=", 2);
				line.line = String.format("%s=%s", lineParts[0].trim(), lineParts[1].trim());
			}
		}
	}

	private static void checkDuplicateKeys(ArrayList<LineStruct> lines) {
		if (lines == null) {
			return;
		}

		for (int i = 0; i < lines.size(); i++) {
			for (int j = i + 1; j < lines.size(); j++) {
				if (lines.get(i).group.equals(lines.get(j).group) && lines.get(i).name.equals(lines.get(j).name)) {
					LOGGER.warn("Warning: Key \"{}\" is duplicated.", lines.get(i).group + "." + lines.get(i).name);
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static void deleteSelected(ArrayList<LineStruct> lines) {
		final String[] DELETEKEYS = {
			"AviSynthFFmpeg.0",
			"AviSynthMEncoder.2",
			"DLNAMediaDatabase.0",
			"DLNAMediaDatabase.3",
			"FFmpegDVRMSRemux.1",
			"FoldTab.1",
			"FoldTab.6",
			"FoldTab.11",
			"FoldTab.53",
			"GeneralTab.4",
			"GeneralTab.7",
			"GeneralTab.8",
			"MEncoderVideo.1",
			"MEncoderVideo.5",
			"PMS.5",
			"PMS.6",
			"PMS.7",
			"PMS.137",
			"StatusTab.7",
			"StatusTab.8",
			"StatusTab.10",
			"TrTab2.2",
			"TrTab2.3",
			"TrTab2.4",
			"TrTab2.12",
			"TrTab2.13",
			"TrTab2.39",
			"TrTab2.40",
			"TrTab2.41",
			"TrTab2.42",
			"TrTab2.43",
			"TrTab2.44",
			"TrTab2.66",
			"TrTab2.69",
			"TsMuxeRVideo.0",
			"TsMuxeRVideo.1",
			"TsMuxeRVideo.3",
			"VlcTrans.1",
			"VlcTrans.2",
			"VlcTrans.4",
			"VlcTrans.6",
			"VlcTrans.7",
			"VlcTrans.8",
			"VlcTrans.9",
			"VlcTrans.10",
			"VlcTrans.11",
			"VlcTrans.17",
			"VlcTrans.18",
			"VlcTrans.19",
			"VlcTrans.20",
		};

		if (lines == null) {
			return;
		}

		for (int i = lines.size() - 1; i > -1; i--) {
			for (String key : DELETEKEYS) {
				if (key.equalsIgnoreCase(lines.get(i).group + "." + lines.get(i).name)) {
					lines.remove(i);
					break;
				}
			}
		}
	}
}
