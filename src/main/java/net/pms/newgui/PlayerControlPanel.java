package net.pms.newgui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.Messages;
import net.pms.util.BasicPlayer;
import net.pms.util.UMSUtils;
import org.apache.commons.lang.StringUtils;

public class PlayerControlPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 8972730138916895247L;

	private BasicPlayer.Logical player;
	@SuppressWarnings("unused")
	private AbstractAction add, remove, clear, play, pause, stop, next, prev, forward, rewind, mute, volume, seturi, excl;
	private Button position;
	private JSlider volumeSlider;
	private JTextField uri;
	private JComboBox uris;
	private boolean edited, playing;
	private String lasturi;
	private File pwd;
	private boolean playControl, volumeControl, expanded;
	int sliding;

	private static ImageIcon addIcon, removeIcon, clearIcon, playIcon, pauseIcon, stopIcon, fwdIcon, rewIcon,
		nextIcon, prevIcon, volumeIcon, muteIcon, sliderIcon;

	public PlayerControlPanel(final BasicPlayer player) {
		if (playIcon == null) {
			loadIcons();
		}
		this.player = (BasicPlayer.Logical)player;
		player.connect(this);
		int controls = player.getControls();
		playControl = (controls & BasicPlayer.PLAYCONTROL) != 0;
		volumeControl = (controls & BasicPlayer.VOLUMECONTROL) != 0;
		expanded = true;
		sliding = 0;

		try {
			pwd = new File(player.getState().uri).getParentFile();
		} catch (Exception e) {
			pwd = new File("");
		}

		setPreferredSize(new Dimension(530, 70));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(6, 0, 0, 0);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = c.weighty = 1.0;

		Toolbar ctrl = new Toolbar();
		ctrl.setLayout(new BoxLayout(ctrl, BoxLayout.X_AXIS));
		if (volumeControl) {
			if (playControl) {
				addVolumeControls(ctrl);
			} else {
				ctrl.add(Box.createHorizontalGlue());
				addVolumeControls(ctrl);
				ctrl.add(Box.createHorizontalGlue());
				add(ctrl, c);
			}
		}
		if (playControl) {
			ctrl.add(Box.createHorizontalStrut(volumeControl ? 55 : 140));
			addPlayControls(ctrl);
			ctrl.add(Box.createHorizontalGlue());
			addStatus(ctrl);
			add(ctrl, c);
			c.gridy++;
			Toolbar uri = new Toolbar();
			addUriControls(uri);
			add(uri, c);
		}

		player.alert();

		final ActionListener self = this;
		getEnclosingWindow(this).addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				player.disconnect(self);
			}
		});

	}

	public void addPlayControls(Container parent) {
		parent.add(new Button(36, prev = new AbstractAction("", prevIcon) {
			private static final long serialVersionUID = 7558487023838124078L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.prev();
			}
		}));
		parent.add(new Button(36, rewind = new AbstractAction("", rewIcon) {
			private static final long serialVersionUID = -1520355550308740828L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.rewind();
			}
		}));
		parent.add(new Button(36, play = new AbstractAction("", playIcon) {
			private static final long serialVersionUID = -5492279549624322429L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setEdited(false);
				player.pressPlay(uri.getText(), null);
			}
		}));
		parent.add(new Button(36, stop = new AbstractAction("", stopIcon) {
			private static final long serialVersionUID = 8389133040373106061L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.pressStop();
			}
		}));
		parent.add(new Button(36, forward = new AbstractAction("", fwdIcon) {
			private static final long serialVersionUID = 9017731678937164070L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.forward();
			}
		}));
		parent.add(new Button(36, next = new AbstractAction("", nextIcon) {
			private static final long serialVersionUID = -2100492235066666555L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.next();
			}
		}));
	}

	public void addStatus(final Container parent) {
		parent.add(position = new Button(new AbstractAction("") {
			private static final long serialVersionUID = 2L;

			@Override
			public void actionPerformed(ActionEvent e) {
				toggleView(parent);
			}
		}));
		position.setHorizontalAlignment(SwingConstants.RIGHT);
		position.setToolTipText(Messages.getString("PlayerControlPanel.0"));
	}

	public void addVolumeControls(Container parent) {
		UIDefaults defaults = UIManager.getDefaults();
		Object hti = defaults.put("Slider.horizontalThumbIcon", sliderIcon);
		Object tb = defaults.put("Slider.trackBorder", BorderFactory.createEmptyBorder());

		volumeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
		Dimension d = new Dimension(80, 20);
		volumeSlider.setPreferredSize(d);
		volumeSlider.setSize(d);
		volumeSlider.setMaximumSize(d);
		volumeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Fire only when the slider is in motion, i.e. not during external updates
				if (((JSlider) e.getSource()).getValueIsAdjusting()) {
					player.setVolume(volumeSlider.getValue());
					// For smoothness ignore external volume data until
					// the 3rd update after sliding has finished
					sliding = 3;
				}
			}
		});
		volumeSlider.setFocusable(false);
//		volumeSlider.setPaintLabels(true);
//		volumeSlider.setLabelTable(new Hashtable<Integer, JLabel>() {{
//			put(0, new JLabel("<html><b>-</b></html>"));
//			put(100, new JLabel("<html><b>+</b></html>"));
//		}});
//		volumeSlider.setAlignmentX(0.25f);
		parent.add(volumeSlider);
		defaults.put("Slider.horizontalThumbIcon", hti);
		defaults.put("Slider.trackBorder", tb);

		Button muteButton = new Button(mute = new AbstractAction("", volumeIcon) {
			private static final long serialVersionUID = 4263195311825852854L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.mute();
			}
		});
		parent.add(muteButton);
	}

	public void addUriControls(Container parent) {
		uris = new JComboBox(player.getPlaylist());
		uris.setMaximumRowCount(20);
		uris.setEditable(true);
		// limit width to available space
		uris.setPrototypeDisplayValue("");
		uri = (JTextField) uris.getEditor().getEditorComponent();
		uri.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent evt) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						uri.select(0, 0);
					}
				});
			}
		});
		uri.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				setEdited(true);
				if (!playing) {
					play.setEnabled(!StringUtils.isBlank(uri.getText()));
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
		});

		parent.add(uris);
		Button a = new Button(add = new AbstractAction("", addIcon) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setEdited(false);
				player.add(-1, uri.getText(), null, null, true);
			}
		});
		a.setToolTipText(Messages.getString("PlayerControlPanel.1"));
		parent.add(a);
		Button r = new Button(remove = new AbstractAction("", removeIcon) {
			private static final long serialVersionUID = 8732700198165912103L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.remove(uri.getText());
			}
		});
		r.setToolTipText(Messages.getString("PlayerControlPanel.2"));
		parent.add(r);
		Button c = new Button(clear = new AbstractAction("", clearIcon) {
			private static final long serialVersionUID = -2484978035031713948L;

			@Override
			public void actionPerformed(ActionEvent e) {
				player.clear();
			}
		});
		c.setToolTipText(Messages.getString("PlayerControlPanel.3"));
		parent.add(c);
		parent.add(new Button(new AbstractAction("", MetalIconFactory.getTreeFolderIcon()) {
			private static final long serialVersionUID = -2826057503405341316L;

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(pwd);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					uri.setText(fc.getSelectedFile().getPath());
					setEdited(true);
				}
				pwd = fc.getCurrentDirectory();
			}
		}));
	}

	public static Window getEnclosingWindow(Component c) {
		return c == null ? JOptionPane.getRootFrame() :
			c instanceof Window ? (Window) c : getEnclosingWindow(c.getParent());
	}

	public BasicPlayer getPlayer() {
		return player;
	}

	public void setEdited(boolean b) {
		edited = b;
		updatePlaylist();
	}

	public void updatePlaylist() {
		boolean empty = uris.getModel().getSize() == 0;
		add.setEnabled((edited || empty) && StringUtils.isNotBlank(uri.getText()));
		remove.setEnabled(! empty);
		clear.setEnabled(! empty);
		boolean more = uris.getModel().getSize() > 1;
		next.setEnabled(more);
		prev.setEnabled(more);
		edited = false;
	}

	public void refresh(BasicPlayer.State state) {
		if (playControl) {
			playing = state.playback != BasicPlayer.STOPPED;
			// update playback status
			play.putValue(Action.SMALL_ICON, state.playback == BasicPlayer.PLAYING ? pauseIcon : playIcon);
			stop.setEnabled(playing);
			forward.setEnabled(playing);
			rewind.setEnabled(playing);
			// update position
			position.setText(UMSUtils.playedDurationStr(state.position, state.duration));
			// update uris only if meaningfully new
			boolean isNew = !StringUtils.isBlank(state.uri) && !state.uri.equals(lasturi);
			lasturi = state.uri;
			if (isNew) {
				if (edited) {
					player.add(-1, uri.getText(), null, null, false);
					setEdited(false);
				}
				uri.setText(state.name);
			}
			play.setEnabled(playing || !StringUtils.isBlank(uri.getText()));
			updatePlaylist();
		}
		if (volumeControl) {
			// update rendering status
			mute.putValue(Action.SMALL_ICON, state.mute ? muteIcon : volumeIcon);
			volumeSlider.setEnabled(!state.mute);
			// ignore volume during slider motion
			if (--sliding < 0) {
				volumeSlider.setValue(state.volume);
			}
		}
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				refresh(((BasicPlayer) e.getSource()).getState());
			}
		});
	}

	private static void loadIcons() {
		addIcon    = loadIcon("/resources/images/player/add16.png");
		removeIcon = loadIcon("/resources/images/player/remove16.png");
		clearIcon  = loadIcon("/resources/images/player/clear16.png");
		playIcon   = loadIcon("/resources/images/player/play16.png");
		pauseIcon  = loadIcon("/resources/images/player/pause16.png");
		stopIcon   = loadIcon("/resources/images/player/stop16.png");
		fwdIcon    = loadIcon("/resources/images/player/fwd16.png");
		rewIcon    = loadIcon("/resources/images/player/rew16.png");
		nextIcon   = loadIcon("/resources/images/player/next16.png");
		prevIcon   = loadIcon("/resources/images/player/prev16.png");
		volumeIcon = loadIcon("/resources/images/player/vol16.png");
		muteIcon   = loadIcon("/resources/images/player/mute16.png");
		sliderIcon = loadIcon("/resources/images/player/bar16.png");
	}

	private static ImageIcon loadIcon(String path) {
		URL url = PlayerControlPanel.class.getResource(path);
		if (url != null) {
			return new ImageIcon(url);
		}
		throw new RuntimeException("icon not found: " + path);
	}

	public void toggleView(Component child) {
		Component anchor = child.getParent();
		anchor.setPreferredSize(anchor.getSize());
		// Toggle sibling visibility
		expanded = !expanded;
		for (Component c : anchor.getParent().getComponents()) {
			if (c != anchor) {
				c.setVisible(expanded);
			}
		}
		// Redraw without moving the anchor (if possible)
		int y = (int) anchor.getLocation().getY();
		JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
		top.setVisible(false);
		top.pack();
		Point p = top.getLocation();
		top.setLocation((int) p.getX(), y - (int) anchor.getLocation().getY() + (int) p.getY());
		top.setVisible(true);
	}

	static class Button extends JButton {
		private static final long serialVersionUID = 8649059925768844933L;
		public Button(Action a) {
			this(0, a);
		}
		public Button(int width, Action a) {
			super(a);
			if (width > 0) {
				setPreferredSize(new Dimension(width, 30));
			}
			setFocusable(false);
		}
	}

	static class Toolbar extends JToolBar {
		private static final long serialVersionUID = -657958964967514184L;

		public Toolbar() {
			super(SwingConstants.HORIZONTAL);
			setFloatable(false);
			setRollover(true);
			setOpaque(false);
			setBorderPainted(false);
		}
	}
}
