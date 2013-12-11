package net.pms.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import net.pms.PMS;
import org.apache.commons.io.FileUtils;

public class SubtitlesUtil {
	
	public static File convertSubripToWebVTT(File tempSubs) throws IOException {
		File outputSubs = FileUtil.getFileNameWithNewExtension(tempSubs, tempSubs, "vtt");
		StringBuilder outputString = new StringBuilder();
		File temp = new File(PMS.getConfiguration().getTempFolder(), tempSubs.getName() + ".tmp");
		FileUtils.copyFile(tempSubs, temp);
		BufferedWriter output;
		try (BufferedReader input = new BufferedReader(new FileReader(temp))) {
			output = new BufferedWriter(new FileWriter(outputSubs));
			String line;
			outputString.append("WEBVTT FILE").append("\n").append("\n");
			output.write(outputString.toString());
			while ((line = input.readLine()) != null) {
				if (line.contains("-->")) {
					outputString.append(line.replace(",", ".")).append("\n");
					output.write(outputString.toString());
					continue;
				}

				if (line.contains("&")) {
					outputString.append(StringUtil.encodeXML(line)).append("\n");
					output.write(outputString.toString());
					continue;
				}

				// outputString.append(StringUtil.encodeXML(line)).append("\n");
				outputString.append(line).append("\n");
				output.write(outputString.toString());
			}
		}
		output.flush();
		output.close();
		temp.deleteOnExit();
		return outputSubs;
	}

}
