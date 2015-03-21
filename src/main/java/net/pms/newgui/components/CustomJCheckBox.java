package net.pms.newgui.components;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JToolTip;

public class CustomJCheckBox extends JCheckBox  {
	private static final long serialVersionUID = -8027836064057652678L;

	public CustomJCheckBox(String text) {
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
