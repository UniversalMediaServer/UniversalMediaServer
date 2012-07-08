package net.pms.dlna;

import net.pms.formats.SubtitleType;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DLNAMediaSubtitleTest {

	@Test
	public void testDefaultSubtitleType() {
		DLNAMediaSubtitle dlnaMediaSubtitle = new DLNAMediaSubtitle();
		assertThat(dlnaMediaSubtitle.getType()).isEqualTo(SubtitleType.UNKNOWN);
	}
}
