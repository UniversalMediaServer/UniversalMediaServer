package net.pms.newgui.components;

import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

public class CustomJLabel extends JLabel {
	private static final long serialVersionUID = -6726814430751911120L;
    private boolean wrap;

	public CustomJLabel() {
	    super();
	    wrap = true;
	}

	public CustomJLabel(boolean linewrap) {
	    super();
	    wrap = linewrap;
	}

	public CustomJLabel(String text) {
	    super(text);
	    wrap = true;
	}

	public CustomJLabel(String text, boolean linewrap) {
	    super(text);
	    wrap = linewrap;
	}
    /**
     * Gets the line-wrapping policy of the label. If set to true the lines
     * will be wrapped if they are too long to fit within the allocated width.
     * If set to false, the lines will always be unwrapped.
     *
     * @return if lines will be wrapped
     */
    public boolean getLineWrap() {
        return wrap;
    }

    /**
     * Returns true if a viewport should always force the width of this
     * Scrollable to match the width of the viewport.  This is implemented to
     * return true if the line wrapping policy is true, and false if lines are
     * not being wrapped.
     *
     * @return true if a viewport should force the Scrollables width to match
     * its own.
     */
    public boolean getScrollableTracksViewportWidth() {
    	if (wrap) {
    		return true;
    	}
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    /**
     * Sets the line-wrapping policy of the label. If set to true the lines
     * will be wrapped if they are too long to fit within the allocated width.
     * If set to false, the lines will always be unwrapped.
     * A <code>PropertyChange</code> event ("lineWrap") is fired when the
     * policy is changed. By default this property is false.
     *
     * @param wrap indicates if lines should be wrapped
     * @see #getLineWrap
     * @beaninfo
     *   preferred: true
     *       bound: true
     * description: should lines be wrapped
     */
    public void setLineWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
        firePropertyChange("lineWrap", old, wrap);
    }

	@Override
	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}

	public static String htmlify(String text) {
		String t = text.trim().toLowerCase();
		return (t.startsWith("<html>") ? "" : "<html>") + text + (t.endsWith("</html>") ? "" : "</html>");
	}
}
