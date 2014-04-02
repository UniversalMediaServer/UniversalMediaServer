package net.pms.newgui;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
//import java.util.Hashtable;
import org.apache.commons.lang.StringUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.util.BasicPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PlayerControlPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 8972730138916895247L;

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererPanel.class);

	private BasicPlayer player;
	private AbstractAction play, pause, stop, next, prev, forward, rewind, mute, volume, seturi;
	private JLabel position;
	private JSlider volumeSlider;
	private JTextField uri;
	private JComboBox uris;
	private boolean edited, playing;
	private String lasturi;
	private File pwd;
	private boolean playControl, volumeControl;

	private static ImageIcon playIcon, pauseIcon, stopIcon, fwdIcon, rewIcon,
		nextIcon, prevIcon, volumeIcon, muteIcon, sliderIcon;

	public PlayerControlPanel(final BasicPlayer player) {
		if (playIcon == null) {
			loadIcons();
		}
		this.player = player;
		player.connect(this);
		int controls = player.getControls();
		playControl = (controls & BasicPlayer.PLAYCONTROL) != 0;
		volumeControl = (controls & BasicPlayer.VOLUMECONTROL) != 0;

		try {
			pwd = new File(player.getState().uri).getParentFile();
		} catch (Exception e) {
			pwd = new File("");
		}

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(5, 5, 5, 5);

		c.gridx = 0; c.gridy = 0;
		if (volumeControl) {
			add(volumePanel(), c);
			c.gridx++;
		}
		if (playControl) {
			add(playbackPanel(), c);
			c.gridx++;
			add(statusPanel(), c);
			c.gridx = 0; c.gridy++;
			c.gridwidth = volumeControl ? 3 : 2;
			add(uriPanel(), c);
		}

		player.refresh();

		final ActionListener self = this;
		getEnclosingWindow(this).addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				player.disconnect(self);
			}
		});

	}

	public JComponent playbackPanel() {
		JToolBar playback = new JToolBar(SwingConstants.HORIZONTAL);
		playback.setLayout(new GridLayout());
		playback.setFloatable(false);
		playback.setRollover(true);
		playback.setPreferredSize(new Dimension(220,30));
		playback.setOpaque(false);
		playback.setBorderPainted(false);

		playback.add(new JButton(prev = new AbstractAction("", prevIcon) {
			private static final long serialVersionUID = 7558487023838124078L;

			public void actionPerformed(ActionEvent e) {
				player.prev();
			}
		}));
		playback.add(new JButton(rewind = new AbstractAction("", rewIcon) {
			private static final long serialVersionUID = -1520355550308740828L;

			public void actionPerformed(ActionEvent e) {
				player.rewind();
			}
		}));
		playback.add(new JButton(play = new AbstractAction("", playIcon) {
			private static final long serialVersionUID = -5492279549624322429L;

			public void actionPerformed(ActionEvent e) {
				if (! StringUtils.isBlank(uri.getText())) {
					store(true);
					player.pressPlay(uri.getText(), null);
				}
			}
		}));
		playback.add(new JButton(stop = new AbstractAction("", stopIcon) {
			private static final long serialVersionUID = 8389133040373106061L;

			public void actionPerformed(ActionEvent e) {
				player.stop();
			}
		}));
		playback.add(new JButton(forward = new AbstractAction("", fwdIcon) {
			private static final long serialVersionUID = 9017731678937164070L;

			public void actionPerformed(ActionEvent e) {
				player.forward();
			}
		}));
		playback.add(new JButton(next = new AbstractAction("", nextIcon) {
			private static final long serialVersionUID = -2100492235066666555L;

			public void actionPerformed(ActionEvent e) {
				player.next();
			}
		}));

		return playback;
	}

	public JComponent statusPanel() {
		JPanel status = new JPanel();
		status.setPreferredSize(new Dimension(150,20));
		status.setLayout(new BoxLayout(status, BoxLayout.X_AXIS));
		status.add(Box.createHorizontalGlue());
		status.add(position = new JLabel());
		return status;
	}

	public JComponent volumePanel() {
		JPanel volume = new JPanel(new FlowLayout(FlowLayout.LEFT));
		volume.setPreferredSize(new Dimension(150,30));

		UIDefaults defaults = UIManager.getDefaults();
		Object hti = defaults.put("Slider.horizontalThumbIcon", sliderIcon);
		Object tb = defaults.put("Slider.trackBorder", BorderFactory.createEmptyBorder());

		volumeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
		volumeSlider.setPreferredSize(new Dimension(80,20));
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				player.setVolume(volumeSlider.getValue());
			}
		});
//		volumeSlider.setPaintLabels(true);
//		volumeSlider.setLabelTable(new Hashtable<Integer, JLabel>() {{
//			put(0, new JLabel("<html><b>-</b></html>"));
//			put(100, new JLabel("<html><b>+</b></html>"));
//		}});
//		volumeSlider.setAlignmentX(0.25f);
		volume.add(volumeSlider);
		defaults.put("Slider.horizontalThumbIcon", hti);
		defaults.put("Slider.trackBorder", tb);

		JToggleButton muteButton = new JToggleButton(mute = new AbstractAction("", volumeIcon) {
			private static final long serialVersionUID = 4263195311825852854L;

			public void actionPerformed(ActionEvent e) {
				player.mute(! player.getState().mute);
			}
		});
		muteButton.setOpaque(false);
		muteButton.setContentAreaFilled(false);
		muteButton.setBorderPainted(false);
		muteButton.setFocusPainted(false);
		muteButton.setRolloverEnabled(true);
		volume.add(muteButton);

		return volume;
	}

	public JComponent uriPanel() {
		JToolBar u = new JToolBar(SwingConstants.HORIZONTAL);
		u.setFloatable(false);
		u.setRollover(true);
		u.setOpaque(false);
		u.setBorderPainted(false);

		JLabel uriLabel = new JLabel("URI: ");
		u.add(uriLabel);
		uris = new JComboBox();
		uris.setMaximumRowCount(20);
		uris.setEditable(true);
		// limit width to available space
		uris.setPrototypeDisplayValue("");
		uri = (JTextField)uris.getEditor().getEditorComponent();
		uri.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				edited = true;
				if (! playing) {
					play.setEnabled(! StringUtils.isBlank(uri.getText()));
				}
			}
			public void insertUpdate(DocumentEvent e) {changedUpdate(e);}
			public void removeUpdate(DocumentEvent e) {changedUpdate(e);}
		});

		u.add(uris);
		u.add(new JButton(new AbstractAction("", MetalIconFactory.getTreeFolderIcon()) {
			private static final long serialVersionUID = -2826057503405341316L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(pwd);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					uri.setText(fc.getSelectedFile().getPath());
				}
				pwd = fc.getCurrentDirectory();
			}
		}));

		return u;
	}

	public void store(boolean select) {
		String u = uri.getText();
		if (edited && ! StringUtils.isBlank(u)) {
			int index = ((DefaultComboBoxModel)uris.getModel()).getIndexOf(u);
			if (index == -1) {
				uris.insertItemAt(uri.getText(), 0);
				index = 0;
			}
			if (select) {
				uris.setSelectedIndex(index);
			}
		}
		edited = false;
	}

	public static Window getEnclosingWindow(Component c) {
		return c == null ? JOptionPane.getRootFrame() :
			c instanceof Window ? (Window) c : getEnclosingWindow(c.getParent());
	}

	public BasicPlayer getPlayer() {
		return player;
	}

	public void refresh(BasicPlayer.State state) {
		if (playControl) {
			playing = state.playback != BasicPlayer.STOPPED;
			// update playback status
			play.putValue(Action.SMALL_ICON,
				state.playback == BasicPlayer.PLAYING ? pauseIcon : playIcon);
			stop.setEnabled(playing);
			forward.setEnabled(playing);
			rewind.setEnabled(playing);
			next.setEnabled(playing);
			prev.setEnabled(playing);
			// update position
			String pos = state.position != null ? state.position : "00:00:00";
			if (state.duration != null && ! state.duration.equals(pos)) {
				pos += ((pos == "" ? "" : " / ") + state.duration);
			}
			position.setText(pos);
			// update uris only if meaningfully new
			boolean isNew = ! StringUtils.isBlank(state.uri)
				&& ! state.uri.equals(lasturi)
				&& ! state.uri.equals(uri.getText());
			lasturi = state.uri;
			if (isNew) {
				store(false);
				uri.setText(state.uri);
				store(true);
			}
			play.setEnabled(playing || ! StringUtils.isBlank(uri.getText()));
		}
		if (volumeControl) {
			// update rendering status
			mute.putValue(Action.SMALL_ICON, state.mute ? muteIcon : volumeIcon);
//			volumeSlider.setVisible(! state.mute);
			volumeSlider.setEnabled(! state.mute);
			volumeSlider.setValue((int)state.volume);
		}
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				refresh(((BasicPlayer) e.getSource()).getState());
			}
		});
	}

	private static void loadIcons() {
		playIcon = loadIcon("/resources/images/player/play16.png");
		pauseIcon = loadIcon("/resources/images/player/pause16.png");
		stopIcon = loadIcon("/resources/images/player/stop16.png");
		fwdIcon = loadIcon("/resources/images/player/fwd16.png");
		rewIcon = loadIcon("/resources/images/player/rew16.png");
		nextIcon = loadIcon("/resources/images/player/next16.png");
		prevIcon = loadIcon("/resources/images/player/prev16.png");
		volumeIcon = loadIcon("/resources/images/player/vol16.png");
		muteIcon = loadIcon("/resources/images/player/mute16.png");
		sliderIcon = loadIcon("/resources/images/player/bar16.png");
	}

	private static ImageIcon loadIcon(String path) {
		URL url = PlayerControlPanel.class.getResource(path);
		if (url != null) {
			return new ImageIcon(url);
		}
		throw new RuntimeException("icon not found: " + path);
	}
}

