package net.pms.newgui.components;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

public class CustomJLabel extends JLabel {
	private static final long serialVersionUID = -6726814430751911120L;

	public CustomJLabel() {
	    super();
	}

	public CustomJLabel(Icon image) {
	    super(image);
	}

	public CustomJLabel(Icon image, int horizontalAlignment) {
	    super(image, horizontalAlignment);
	}

	public CustomJLabel(String text) {
	    super(text);
	}

	public CustomJLabel(String text, Icon icon, int horizontalAlignment) {
	    super(text, icon, horizontalAlignment);
	}

	public CustomJLabel(String text, int horizontalAlignment) {
	    super(text, horizontalAlignment);
	}

	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}

	public Point getToolTipLocation(MouseEvent event) {
	    return new Point(getWidth() / 2, getHeight() / 2);
	}
}
