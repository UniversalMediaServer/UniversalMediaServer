/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.newgui.engines;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.util.FormLayoutUtil;

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

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("GeneralSettings_SentenceCase"), FormLayoutUtil.flip(cc.xyw(2, 1, 1), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		JCheckBox tsmuxerforcefps = new JCheckBox(Messages.getString("ForceFpsParsedFfmpeg"), CONFIGURATION.isTsmuxerForceFps());
		tsmuxerforcefps.setContentAreaFilled(false);
		tsmuxerforcefps.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setTsmuxerForceFps(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(tsmuxerforcefps), FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JCheckBox muxallaudiotracks = new JCheckBox(Messages.getString("MuxAllAudioTracks"), CONFIGURATION.isMuxAllAudioTracks());
		muxallaudiotracks.setContentAreaFilled(false);
		muxallaudiotracks.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMuxAllAudioTracks(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(muxallaudiotracks), FormLayoutUtil.flip(cc.xy(2, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

}
