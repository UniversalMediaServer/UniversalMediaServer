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
package net.pms.swing.gui.tabs.transcoding;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.swing.SwingUtil;
import net.pms.swing.gui.UmsFormBuilder;

public class FFmpegAudio {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * This class is not meant to be instantiated.
	 */
	private FFmpegAudio() {
	}

	public static JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow"
		);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getGuiString("GeneralSettings_SentenceCase")).at(cc.xyw(2, 1, 1));

		JCheckBox noresample = new JCheckBox(Messages.getGuiString("AutomaticAudioResampling"), CONFIGURATION.isAudioResample());
		noresample.setContentAreaFilled(false);
		noresample.addItemListener((ItemEvent e) -> CONFIGURATION.setAudioResample(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(noresample)).at(cc.xy(2, 3));

		return builder.getPanel();
	}

}
