/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.swing.gui.tabs.about;

import com.jgoodies.forms.FormsSetup;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.platform.PlatformUtils;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.SvgMultiResolutionImage;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.util.PropertiesUtil;

public class AboutTab {

	private Integer rowPosition = 1;

	/**
	 * Gets the current GUI row position and adds 2 to it for next time. Using
	 * it saves having to manually specify numbers which can waste time when we
	 * make changes.
	 */
	private Integer getAndIncrementRowPosition() {
		Integer currentRowPosition = rowPosition;
		rowPosition += 2;
		return currentRowPosition;
	}

	public JComponent build() {
		rowPosition = 1;
		FormLayout layout = new FormLayout(
				"0:grow, pref, 0:grow",
				"pref, 3dlu, pref, 12dlu, pref, 12dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"
		);

		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout).border(Paddings.DIALOG).opaque(true);
		CellConstraints cc = new CellConstraints();

		String projectName = PropertiesUtil.getProjectProperties().get("project.name");

		final LinkMouseListener pms3Link = new LinkMouseListener(projectName + " " + PMS.getVersion(),
				"https://www.universalmediaserver.com/");
		JLabel lPms3Link = FormsSetup.getComponentFactoryDefault().createLabel(pms3Link.getLabel());
		lPms3Link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lPms3Link.addMouseListener(pms3Link);
		builder.add(lPms3Link).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		// Create a build name from the available git properties
		String commitId = PropertiesUtil.getProjectProperties().get("git.commit.id");
		String commitTime = PropertiesUtil.getProjectProperties().get("git.commit.time");
		String commitUrl = "https://github.com/UniversalMediaServer/UniversalMediaServer/commit/" + commitId;
		String buildLabel = Messages.getGuiString("BuildDate") + " " + commitTime;

		final LinkMouseListener commitLink = new LinkMouseListener(buildLabel, commitUrl);
		JLabel lCommitLink = FormsSetup.getComponentFactoryDefault().createLabel(commitLink.getLabel());
		lCommitLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lCommitLink.addMouseListener(commitLink);
		builder.add(lCommitLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		builder.add(buildLogoImage()).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		builder.addLabel(Messages.getGuiString("RelatedLinks")).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final String crowdinName = (String.format(Messages.getGuiString("XCrowdinProject"), PMS.NAME));
		final LinkMouseListener crowdinLink = new LinkMouseListener(crowdinName, PMS.CROWDIN_LINK);
		JLabel lCrowdinLink = FormsSetup.getComponentFactoryDefault().createLabel(crowdinLink.getLabel());
		lCrowdinLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lCrowdinLink.addMouseListener(crowdinLink);
		builder.add(lCrowdinLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener ffmpegLink = new LinkMouseListener("FFmpeg", "https://ffmpeg.org/");
		JLabel lFfmpegLink = FormsSetup.getComponentFactoryDefault().createLabel(ffmpegLink.getLabel());
		lFfmpegLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lFfmpegLink.addMouseListener(ffmpegLink);
		builder.add(lFfmpegLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener mplayerLink = new LinkMouseListener("MPlayer", "http://www.mplayerhq.hu");
		JLabel lMplayerLink = FormsSetup.getComponentFactoryDefault().createLabel(mplayerLink.getLabel());
		lMplayerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lMplayerLink.addMouseListener(mplayerLink);
		builder.add(lMplayerLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener spirtonLink = new LinkMouseListener("MPlayer, MEncoder and InterFrame builds", "https://www.spirton.com");
		JLabel lSpirtonLink = FormsSetup.getComponentFactoryDefault().createLabel(spirtonLink.getLabel());
		lSpirtonLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lSpirtonLink.addMouseListener(spirtonLink);
		builder.add(lSpirtonLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener mediaInfoLink = new LinkMouseListener("MediaInfo", "https://mediaarea.net/en/MediaInfo");
		JLabel lMediaInfoLink = FormsSetup.getComponentFactoryDefault().createLabel(mediaInfoLink.getLabel());
		lMediaInfoLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lMediaInfoLink.addMouseListener(mediaInfoLink);
		builder.add(lMediaInfoLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener avisynthMTLink = new LinkMouseListener("AviSynth MT", "https://forum.doom9.org/showthread.php?t=148782");
		JLabel lAvisynthMTLink = FormsSetup.getComponentFactoryDefault().createLabel(avisynthMTLink.getLabel());
		lAvisynthMTLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lAvisynthMTLink.addMouseListener(avisynthMTLink);
		builder.add(lAvisynthMTLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener dryIconsLink = new LinkMouseListener("DryIcons", "https://dryicons.com/");
		JLabel lDryIconsLink = FormsSetup.getComponentFactoryDefault().createLabel(dryIconsLink.getLabel());
		lDryIconsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lDryIconsLink.addMouseListener(dryIconsLink);
		builder.add(lDryIconsLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener jmgIconsLink = new LinkMouseListener("Jordan Michael Groll's Icons", "https://www.deviantart.com/jrdng");
		JLabel lJmgIconsLink = FormsSetup.getComponentFactoryDefault().createLabel(jmgIconsLink.getLabel());
		lJmgIconsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lJmgIconsLink.addMouseListener(jmgIconsLink);
		builder.add(lJmgIconsLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener svpLink = new LinkMouseListener("SVP", "https://www.svp-team.com/");
		JLabel lSVPLink = FormsSetup.getComponentFactoryDefault().createLabel(svpLink.getLabel());
		lSVPLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lSVPLink.addMouseListener(svpLink);
		builder.add(lSVPLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		final LinkMouseListener openSubtitlesLink = new LinkMouseListener("OpenSubtitles.org", "https://www.opensubtitles.org/");
		JLabel lOpenSubtitlesLink = FormsSetup.getComponentFactoryDefault().createLabel(openSubtitlesLink.getLabel());
		lOpenSubtitlesLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lOpenSubtitlesLink.addMouseListener(openSubtitlesLink);
		builder.add(lOpenSubtitlesLink).at(cc.xy(2, getAndIncrementRowPosition(), "center, fill"));

		JScrollPane scrollPane = new JScrollPane(builder.getPanel());
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private static class LinkMouseListener extends MouseAdapter {

		private final String name;
		private final String link;

		public LinkMouseListener(String n, String l) {
			name = n;
			link = l;
		}

		public String getLabel() {
			final StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			sb.append("<a href=\"");
			sb.append(link);
			sb.append("\">");
			sb.append(name);
			sb.append("</a>");
			sb.append("</html>");
			return sb.toString();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			PlatformUtils.INSTANCE.browseURI(link);
		}

	}

	private static ImageIcon buildLogoImage() {
		if (SwingUtil.HDPI_AWARE) {
			return new SvgMultiResolutionImage(SwingUtil.getImageResource("logo.svg"), 256, 256).toImageIcon();
		} else {
			return SwingUtil.getImageIcon("logo.png");
		}
	}

}
