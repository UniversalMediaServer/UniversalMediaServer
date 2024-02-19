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
import java.awt.ComponentOrientation;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.swing.SwingUtil;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.UmsFormBuilder;

public class TsMuxeRVideo {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String COL_SPEC = "left:pref, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow";

	/**
	 * This class is not meant to be instantiated.
	 */
	private TsMuxeRVideo() {
	}

	public static JComponent config() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);

		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getGuiString("GeneralSettings_SentenceCase")).at(FormLayoutUtil.flip(cc.xyw(2, 1, 1), colSpec, orientation));

		JCheckBox tsmuxerforcefps = new JCheckBox(Messages.getGuiString("ForceFpsParsedFfmpeg"), CONFIGURATION.isTsmuxerForceFps());
		tsmuxerforcefps.setContentAreaFilled(false);
		tsmuxerforcefps.addItemListener((ItemEvent e) -> CONFIGURATION.setTsmuxerForceFps(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(tsmuxerforcefps)).at(FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JCheckBox muxallaudiotracks = new JCheckBox(Messages.getGuiString("MuxAllAudioTracks"), CONFIGURATION.isMuxAllAudioTracks());
		muxallaudiotracks.setContentAreaFilled(false);
		muxallaudiotracks.addItemListener((ItemEvent e) -> CONFIGURATION.setMuxAllAudioTracks(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(muxallaudiotracks)).at(FormLayoutUtil.flip(cc.xy(2, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

}
