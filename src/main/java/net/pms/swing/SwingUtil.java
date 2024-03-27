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
package net.pms.swing;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.image.BaseMultiResolutionImage;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.JTextComponent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.swing.components.SvgMultiResolutionImage;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SwingUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(SwingUtil.class);
	private static final Object LOOK_AND_FEEL_INITIALIZED_LOCK = new Object();
	/**
	 * Class name of Windows L&F provided in Sun JDK.
	 */
	private static final String WINDOWS_LNF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

	/**
	 * Class name of PlasticXP L&F.
	 */
	private static final String PLASTICXP_LNF = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";

	/**
	 * Class name of Metal L&F.
	 */
	private static final String METAL_LNF = "javax.swing.plaf.metal.MetalLookAndFeel";

	/**
	 * Class name of GTK L&F.
	 */
	private static final String GTK_LNF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";

	/**
	 * Temp flag until svg is fully implemented.
	 */
	public static final boolean HDPI_AWARE = true;

	private static boolean lookAndFeelInitialized = false;

	/**
	 * This class is not meant to be instantiated.
	 */
	private SwingUtil() {
	}

	/**
	 * Check swing availability.
	 *
	 * It don't use java.awt.GraphicsEnvironment.isHeadless() as some Linux
	 * distributions seem to not handle it properly.
	 *
	 * @return true if is headless, false if swing is available
	 */
	public static boolean isHeadless() {
		try {
			JDialog d = new JDialog();
			d.dispose();
			return false;
		} catch (NoClassDefFoundError | HeadlessException | InternalError e) {
			return true;
		}
	}

	/**
	 * Brings up a dialog with a yes/no choice
	 *
	 * @param message the message to display
	 * @param title the title string for the dialog
	 * @param defaultValue the default value
	 * @return an boolean indicating the option chosen by the user
	 */
	public static boolean askYesNoMessage(Object message, String title, boolean defaultValue) {
		if (!PMS.isHeadless()) {
			Object[] yesNoOptions = {
				Messages.getGuiString("Yes"),
				Messages.getGuiString("No")
			};
			int result = JOptionPane.showOptionDialog(
					null,
					message,
					title,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					yesNoOptions,
					defaultValue ? yesNoOptions[0] : yesNoOptions[1]
			);
			return result == JOptionPane.YES_OPTION;
		}
		return defaultValue;
	}

	/**
	 * Init the default platform-specific implementation of Toolkit.
	 *
	 * @return false if a toolkit could not be found, or if one could not be
	 * accessed or instantiated.
	 */
	public static boolean initDefaultToolkit() {
		try {
			Toolkit.getDefaultToolkit();
			return true;
		} catch (AWTError t) {
			LOGGER.error("Toolkit error: " + t.getClass().getName() + ": " + t.getMessage());
			return false;
		}
	}

	/**
	 * Wraps a {@link JComponent} into a {@link JPanel} using a
	 * {@link BorderLayout}, adding it to WEST.<br>
	 * If using this method for e.g. a {@link JCheckBox} and adding it to a
	 * layout, the {@link JCheckBox} won't span the entire space and thus, it
	 * won't change the checked state if clicking outside of it.
	 *
	 * @param component the component
	 * @return the preferred size component
	 */
	public static JComponent getPreferredSizeComponent(JComponent component) {
		JPanel pWrap = new JPanel(new BorderLayout());
		pWrap.add(component, BorderLayout.WEST);
		return pWrap;
	}

	public static void enableContainer(Container c, boolean enable) {
		for (Component component : c.getComponents()) {
			component.setEnabled(enable);
			if (component instanceof Container container) {
				enableContainer(container, enable);
			}
		}
	}

	public static URL getImageResource(String filename) {
		return SwingUtil.class.getResource("/resources/images/" + filename);
	}

	public static Image getImage(String filename) {
		URL url = getImageResource(filename);
		if (url != null) {
			if ("svg".equalsIgnoreCase(FileUtil.getExtension(filename))) {
				return new SvgMultiResolutionImage(url);
			} else {
				Image img = new ImageIcon(url).getImage();
				ArrayList<Image> images = new ArrayList<>();
				images.add(img);
				String[] resolutions = {"@1.25x", "@1.5x", "@1.75x", "@2x", "@2.25x", "@2.5x", "@2.75x", "@3x", "@3.25x", "@3.5x", "@3.75x", "@4x"};
				for (String resolution : resolutions) {
					url = getImageResource(FileUtil.appendToFileName(filename, resolution));
					if (url != null) {
						images.add(new ImageIcon(url).getImage());
					}
				}
				if (images.size() > 1) {
					return new BaseMultiResolutionImage(images.toArray(Image[]::new));
				} else {
					return img;
				}
			}
		}
		return null;
	}

	public static ImageIcon getImageIcon(String filename) {
		Image image = getImage(filename);
		if (image != null) {
			return new ImageIcon(image);
		}
		return null;
	}

	public static Image getAppIconImage() {
		if (HDPI_AWARE) {
			return new SvgMultiResolutionImage(getImageResource("icon.svg"));
		} else {
			ArrayList<Image> images = new ArrayList<>();
			for (String filename : new String[]{"icon-16.png", "icon-20.png", "icon-24.png", "icon-28.png", "icon-32.png", "icon-36.png",
					"icon-40.png", "icon-44.png", "icon-48.png", "icon-52.png", "icon-56.png", "icon-60.png", "icon-64.png"}) {
				URL url = getImageResource(filename);
				if (url != null) {
					images.add(new ImageIcon(url).getImage());
				}
			}
			return new BaseMultiResolutionImage(images.toArray(Image[]::new));
		}
	}

	/**
	 * Returns the {@link Dimension} for the given {@link JTextComponent}
	 * subclass that will show the whole word wrapped text in the given width.
	 * It won't work for styled text of varied size or style, it's assumed that
	 * the whole text is rendered with the {@link JTextComponent}s font.
	 *
	 * @param textComponent the {@link JTextComponent} to calculate the {@link Dimension} for
	 * @param width the width of the resulting {@link Dimension}
	 * @param text the {@link String} which should be word wrapped
	 * @return The calculated {@link Dimension}
	 */
	public static Dimension getWordWrappedTextDimension(JTextComponent textComponent, int width, String text) {
		if (textComponent == null) {
			throw new IllegalArgumentException("textComponent cannot be null");
		}
		if (width < 1) {
			throw new IllegalArgumentException("width must be 1 or greater");
		}
		if (text == null) {
			text = textComponent.getText();
		}
		if (text.isEmpty()) {
			return new Dimension(width, 0);
		}

		FontMetrics metrics = textComponent.getFontMetrics(textComponent.getFont());
		FontRenderContext rendererContext = metrics.getFontRenderContext();
		int formatWidth = width - textComponent.getInsets().left - textComponent.getInsets().right;

		int lines = 0;
		String[] paragraphs = text.split("\n");
		for (String paragraph : paragraphs) {
			if (paragraph.isEmpty()) {
				lines++;
			} else {
				AttributedString attributedText = new AttributedString(paragraph);
				attributedText.addAttribute(TextAttribute.FONT, textComponent.getFont());
				AttributedCharacterIterator charIterator = attributedText.getIterator();
				LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(charIterator, rendererContext);

				lineMeasurer.setPosition(charIterator.getBeginIndex());
				while (lineMeasurer.getPosition() < charIterator.getEndIndex()) {
					lineMeasurer.nextLayout(formatWidth);
					lines++;
				}
			}
		}

		return new Dimension(width, metrics.getHeight() * lines + textComponent.getInsets().top + textComponent.getInsets().bottom);
	}

	/**
	 * Returns the {@link Dimension} for the given {@link JTextComponent}
	 * subclass that will show the whole word wrapped text in the given width.
	 * The {@link String} from {@link JTextComponent#getText()} will be used
	 * as the word wrapped text. It won't work for styled text of varied size
	 * or style, it's assumed that the whole text is rendered with the
	 * {@link JTextComponent}s font.
	 *
	 * @param textComponent the {@link JTextComponent} to calculate the {@link Dimension} for
	 * @param width the width of the resulting {@link Dimension}
	 * @return The calculated {@link Dimension}
	 */
	public static Dimension getWordWrappedTextDimension(JTextComponent textComponent, int width) {
		return getWordWrappedTextDimension(textComponent, width, null);
	}

	/**
	 * Calculates the average character width for the given {@link Component}.
	 * This can be useful as a scaling factor when designing for font scaling.
	 *
	 * @param component the {@link Component} for which to calculate the
	 *        average character width.
	 * @return The average width in pixels
	 */
	public static float getComponentAverageCharacterWidth(Component component) {
		FontMetrics metrics = component.getFontMetrics(component.getFont());
		int i = 0;
		float avgWidth = 0;
		for (int width : metrics.getWidths()) {
			avgWidth += width;
			i++;
		}
		return i == 0 ? 0 : avgWidth / i;
	}

	public static void initializeLookAndFeel() {
		synchronized (LOOK_AND_FEEL_INITIALIZED_LOCK) {
			if (lookAndFeelInitialized) {
				return;
			}

			if (Platform.isWindows()) {
				try {
					UIManager.setLookAndFeel(WINDOWS_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Windows look and feel: ", e);
				}
			} else if (System.getProperty("nativelook") == null && !Platform.isMac()) {
				try {
					UIManager.setLookAndFeel(PLASTICXP_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Plastic XP look and feel: ", e);
				}
			} else {
				try {
					String systemClassName = UIManager.getSystemLookAndFeelClassName();

					if (!Platform.isMac()) {
						// Workaround for Gnome
						try {
							Class.forName(GTK_LNF);

							if (systemClassName.equals(METAL_LNF)) {
								systemClassName = GTK_LNF;
							}
						} catch (ClassNotFoundException ce) {
							LOGGER.error("Error loading GTK look and feel: ", ce);
						}
					}

					LOGGER.trace("Choosing Java look and feel: " + systemClassName);
					UIManager.setLookAndFeel(systemClassName);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
					try {
						UIManager.setLookAndFeel(PLASTICXP_LNF);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
						LOGGER.error("Error while setting Plastic XP look and feel: ", e);
					}
					LOGGER.error("Error while setting native look and feel: ", e1);
				}
			}

			if (isParticularLaFSet(UIManager.getLookAndFeel(), PLASTICXP_LNF)) {
				PlasticLookAndFeel.setPlasticTheme(PlasticLookAndFeel.createMyDefaultTheme());
				PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
				PlasticLookAndFeel.setHighContrastFocusColorsEnabled(false);
			} else if (isParticularLaFSet(UIManager.getLookAndFeel(), METAL_LNF)) {
				MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
			}

			// Work around caching in MetalRadioButtonUI
			JRadioButton radio = new JRadioButton();
			radio.getUI().uninstallUI(radio);
			JCheckBox checkBox = new JCheckBox();
			checkBox.getUI().uninstallUI(checkBox);

			// Workaround for JDK-8179014: JFileChooser with Windows look and feel crashes on win 10
			// https://bugs.openjdk.java.net/browse/JDK-8179014
			if (isParticularLaFSet(UIManager.getLookAndFeel(), WINDOWS_LNF)) {
				UIManager.put("FileChooser.useSystemExtensionHiding", false);
			}

			lookAndFeelInitialized = true;
		}
	}

	/**
	 * Safely checks whether a particular look and feel class is set.
	 *
	 * @param lnf
	 * @param lookAndFeelClassPath
	 * @return whether the incoming look and feel class is set
	 */
	private static boolean isParticularLaFSet(LookAndFeel lnf, String lookAndFeelClassPath) {
		// as of Java 10, com.sun.java.swing.plaf.windows.WindowsLookAndFeel
		// is no longer available on macOS
		// thus "instanceof WindowsLookAndFeel" directives will result
		// in a NoClassDefFoundError during runtime
		if (lnf == null) {
			return false;
		} else {
			try {
				Class<?> c = Class.forName(lookAndFeelClassPath);
				return c.isInstance(lnf);
			} catch (ClassNotFoundException cnfe) {
				// if it is not possible to load the Windows LnF class, the
				// given lnf instance cannot be an instance of the Windows
				// LnF class
				return false;
			}
		}
	}

}
