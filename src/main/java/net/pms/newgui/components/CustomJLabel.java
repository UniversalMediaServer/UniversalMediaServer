package net.pms.newgui.components;

import javax.swing.JLabel;
import javax.swing.JToolTip;

public class CustomJLabel extends JLabel {
	private static final long serialVersionUID = -6726814430751911120L;

	public CustomJLabel(String text) {
	    super(text);
	}

	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}
}
