package net.pms.newgui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectRenderers extends JPanel implements ActionListener {
	private static final long serialVersionUID = -2724796596060834064L;
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private final static List<JCheckBox> checkBoxes = new ArrayList<>();
	private JButton selectAll = new JButton(Messages.getString("GeneralTab.7"));
	private JButton deselectAll = new JButton(Messages.getString("GeneralTab.8"));
	private static ArrayList<String> allRenderersNames = RendererConfiguration.getAllRenderersNames();
	private static List<String> ignoredRenderers = configuration.getIgnoredRenderers();
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectRenderers.class);

	public SelectRenderers() {
		super(new BorderLayout());
		JPanel checkPanel = new JPanel(new GridLayout(0, 3));

		selectAll.addActionListener(this);
		checkPanel.add(selectAll);

		deselectAll.addActionListener(this);
		checkPanel.add(deselectAll);

		checkPanel.add(new JLabel(""));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));

		for (String rendererName : allRenderersNames) {
			JCheckBox checkbox = new JCheckBox(rendererName, !ignoredRenderers.contains(rendererName));
			checkBoxes.add(checkbox);
			checkPanel.add(checkbox);
		}

		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(new Locale(configuration.getLanguage())));
		add(checkPanel, BorderLayout.LINE_START);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	}

	/**
	 * Listens to the buttons.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JButton) {
			if (source.equals(selectAll)) {
				deselectAll.setSelected(false);
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(true);
				}
			} else if (source.equals(deselectAll)) {
				selectAll.setSelected(false);
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(false);
				}
			}
		}
	}

	/**
	 * Create the GUI and show it.
	 */
	public void showDialog() {
		// Refresh setting if modified
		ignoredRenderers = configuration.getIgnoredRenderers();
		for (JCheckBox checkBox : checkBoxes) {
			if (ignoredRenderers.contains(checkBox.getText())) {
				checkBox.setSelected(false);
			} else {
				checkBox.setSelected(true);
			}
		}

		int selectRenderers = JOptionPane.showOptionDialog(
			(Component) PMS.get().getFrame(),
			this,
			Messages.getString("GeneralTab.5"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			null
		);
		if (selectRenderers == JOptionPane.OK_OPTION) {
			StringBuilder buildIgnoredRenders = new StringBuilder();
			for (JCheckBox checkBox : checkBoxes) {
				if (!checkBox.isSelected()) {
					buildIgnoredRenders.append(checkBox.getText()).append(",");
				}
			}

			configuration.setIgnoredRenderers(buildIgnoredRenders.toString());
			try {
				configuration.save();
			} catch (ConfigurationException e) {
				LOGGER.error("Could not save configuration", e);
			}
			
		}
	}
}
