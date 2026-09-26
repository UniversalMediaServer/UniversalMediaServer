package net.pms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Random;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class FileUtilUrlTest {
	private static final Pattern ORIGINAL = Pattern.compile("\\S+://.*");

	@Test
	public void preservesUrlAndLocalPathSemantics() {
		for (String input : new String[] {null, "", "E:\\Filmy\\video.mkv", "/media/video.mp4",
			"https://example.test/a", "rtmp://host/app playpath=sample", "file:///E:/video.mkv",
			"://host", "://a://b", "a://", "a b://host", "a://b://c", "a://b\nc", "a://b\r", "a://b\u2028c"}) {
			assertEquals(input != null && ORIGINAL.matcher(input).matches(), FileUtil.isUrl(input), input);
		}
	}

	@Test
	public void agreesWithOriginalOnRandomInputs() {
		Random random = new Random(47193);
		String alphabet = "abcXYZ012:/\\ \t\r\n\u0085\u2028";
		for (int i = 0; i < 10000; i++) {
			StringBuilder input = new StringBuilder();
			for (int n = random.nextInt(80); n > 0; n--) {
				input.append(alphabet.charAt(random.nextInt(alphabet.length())));
			}
			if (random.nextBoolean()) {
				input.insert(random.nextInt(input.length() + 1), "://");
			}
			String value = input.toString();
			assertEquals(ORIGINAL.matcher(value).matches(), FileUtil.isUrl(value), value);
		}
	}
}
