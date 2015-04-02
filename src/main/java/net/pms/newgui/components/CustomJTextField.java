package net.pms.newgui.components;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import javax.swing.JToolTip;

public class CustomJTextField extends JTextField {
	private static final long serialVersionUID = -5697643578934331831L;

	public CustomJTextField(String text) {
	    super(text);
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

