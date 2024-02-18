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
package net.pms.swing.gui.tabs.status;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.gui.IRendererGuiListener;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.swing.components.FixedPanel;
import net.pms.swing.components.MarqueeLabel;
import net.pms.swing.components.SimpleProgressUI;
import net.pms.swing.components.SmoothProgressBar;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

public class RendererPanel implements ActionListener, IRendererGuiListener {

	private static final int MINIMUM_FILENAME_DISPLAY_SIZE = 200;
	private static final Color BUF_COLOR = new Color(255, 128, 0, 128);

	private final RendererImage rendererImage;
	private final JLabel label;
	private final MarqueeLabel playingLabel;
	private final FixedPanel playing;
	private final JLabel time;
	private final int bufferSize;
	private final SmoothProgressBar rendererProgressBar;

	private RendererFrame rendererFrame;
	private String name = " ";
	private JPanel panel = null;

	public RendererPanel(Renderer renderer) {
		rendererImage = new RendererImage(renderer);
		String rendererName = renderer.getRendererName();
		if ("UnknownRenderer".equals(rendererName)) {
			rendererName = Messages.getGuiString(rendererName);
		}
		label = new JLabel(rendererName);
		playingLabel = new MarqueeLabel(" ");
		playingLabel.setForeground(Color.gray);
		int h = (int) playingLabel.getSize().getHeight();
		playing = new FixedPanel(200, h);
		playing.add(playingLabel);
		time = new JLabel(" ");
		time.setForeground(Color.gray);
		bufferSize = renderer.getUmsConfiguration().getMaxMemoryBufferSize();
		rendererProgressBar = new SmoothProgressBar(0, 100, new SimpleProgressUI(Color.gray, Color.gray));
		rendererProgressBar.setStringPainted(true);
		rendererProgressBar.setBorderPainted(false);
		if (renderer.getAddress() != null) {
			rendererProgressBar.setString(renderer.getAddress().getHostAddress());
		}
		rendererProgressBar.setForeground(BUF_COLOR);
		rendererImage.enableRollover();
		rendererImage.setAction(new AbstractAction() {
			private static final long serialVersionUID = -6316055325551243347L;

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					if (rendererFrame == null) {
						JFrame top = (JFrame) SwingUtilities.getWindowAncestor(getPanel());
						// We're using JFrame instead of JDialog here so as to
						// have a minimize button. Since the player panel
						// is intrinsically a freestanding module this approach
						// seems valid to me but some frown on it: see
						// http://stackoverflow.com/questions/9554636/the-use-of-multiple-jframes-good-bad-practice
						rendererFrame = new RendererFrame(top, renderer);
					} else {
						rendererFrame.setExtendedState(Frame.NORMAL);
						rendererFrame.setVisible(true);
						rendererFrame.toFront();
					}
				});
			}
		});
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		PlayerState state = ((BasicPlayer) e.getSource()).getState();
		time.setText((state.isStopped() || StringUtil.isZeroTime(state.getPosition())) ? " " :
				UMSUtils.playedDurationStr(state.getPosition(), state.getDuration()));
		rendererProgressBar.setValue((int) (100 * state.getBuffer() / bufferSize));
		String n = (state.isStopped() || StringUtils.isBlank(state.getName())) ? " " : state.getName();
		if (!name.equals(n)) {
			name = n;
			playingLabel.setText(name);
		}
		// Maximize the playing label width if not already done
		if (playing.getSize().width == 0) {
			int w = panel.getWidth() - panel.getInsets().left - panel.getInsets().right;
			if (w < MINIMUM_FILENAME_DISPLAY_SIZE) {
				w = MINIMUM_FILENAME_DISPLAY_SIZE;
			}
			playing.setSize(w, (int) playingLabel.getSize().getHeight());
			playingLabel.setMaxWidth(w);
		}
	}

	@Override
	public void refreshPlayerState(final PlayerState state) {
		time.setText((state.isStopped() || StringUtil.isZeroTime(state.getPosition())) ? " " :
				UMSUtils.playedDurationStr(state.getPosition(), state.getDuration()));
		rendererProgressBar.setValue((int) (100 * state.getBuffer() / bufferSize));
		String n = (state.isStopped() || StringUtils.isBlank(state.getName())) ? " " : state.getName();
		if (!name.equals(n)) {
			name = n;
			playingLabel.setText(name);
		}
	}

	public void addTo(Container parent) {
		parent.add(getPanel());
		parent.validate();
		// Maximize the playing label width
		int w = panel.getWidth() - panel.getInsets().left - panel.getInsets().right;
		playing.setSize(w, (int) playingLabel.getSize().getHeight());
		playingLabel.setMaxWidth(w);
	}

	@Override
	public void delete() {
		SwingUtilities.invokeLater(() -> {
			try {
				// Delete the popup if open
				if (rendererFrame != null) {
					rendererFrame.dispose();
					rendererFrame = null;
				}
				Container parent = panel.getParent();
				parent.remove(panel);
				parent.revalidate();
				parent.repaint();
			} catch (Exception e) {
				//nothing to do
			}
		});
	}

	public JPanel getPanel() {
		if (panel == null) {
			UmsFormBuilder b = UmsFormBuilder.create().layout(new FormLayout(
					"center:pref",
					"max(140px;pref), 3dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"
			));
			b.opaque(true);
			CellConstraints cc = new CellConstraints();
			b.add(rendererImage).at(cc.xy(1, 1));
			b.add(label).at(cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
			b.add(rendererProgressBar).at(cc.xy(1, 5));
			b.add(playing).at(cc.xy(1, 7, CellConstraints.CENTER, CellConstraints.DEFAULT));
			b.add(time).at(cc.xy(1, 9));
			panel = b.getPanel();
		}
		return panel;
	}

	@Override
	public void updateRenderer(final Renderer renderer) {
		SwingUtilities.invokeLater(() -> {
			rendererImage.set(renderer);
			String rendererName = renderer.getRendererName();
			if ("UnknownRenderer".equals(rendererName)) {
				rendererName = Messages.getGuiString(rendererName);
			}
			label.setText(rendererName);
			// Update the popup panel if it's been opened
			if (rendererFrame != null) {
				rendererFrame.update();
			}
		});
	}

	@Override
	public void setActive(final boolean active) {
		rendererImage.setGrey(!active);
	}

	@Override
	public void setAllowed(boolean allowed) {
		// not implemented on Java GUI
	}

	@Override
	public void setUserId(int userId) {
		// not implemented on Java GUI
	}

}