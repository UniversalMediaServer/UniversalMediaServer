package net.pms.newgui.components;

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
}
