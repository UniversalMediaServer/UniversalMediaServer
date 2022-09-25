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
import javax.swing.JTextField;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.util.FormLayoutUtil;

public class VLCVideo {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p";

	protected static JTextField scale;
	protected static JCheckBox experimentalCodecs;
	protected static JCheckBox audioSyncEnabled;
	protected static JTextField sampleRate;
	protected static JCheckBox sampleRateOverride;

	public static JComponent config() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("GeneralSettings_SentenceCase"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		experimentalCodecs = new JCheckBox(Messages.getString("EnableExperimentalCodecs"), CONFIGURATION.isVlcExperimentalCodecs());
		experimentalCodecs.setContentAreaFilled(false);
		experimentalCodecs.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setVlcExperimentalCodecs(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(experimentalCodecs), FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		audioSyncEnabled = new JCheckBox(Messages.getString("AvSyncAlternativeMethod"), CONFIGURATION.isVlcAudioSyncEnabled());
		audioSyncEnabled.setContentAreaFilled(false);
		audioSyncEnabled.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setVlcAudioSyncEnabled(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(audioSyncEnabled), FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

}
