package net.pms.newgui.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JToolTip;

public class CustomJCheckBox extends JCheckBox  {
	private static final long serialVersionUID = -8027836064057652678L;

	public CustomJCheckBox() {
		super();
	}

	public CustomJCheckBox(Action a) {
		super(a);
	}

	public CustomJCheckBox(Icon icon) {
		super(icon);
	}

	public CustomJCheckBox(String text) {
	    super(text);
	}

	public CustomJCheckBox(Icon icon, boolean selected) {
		super(icon, selected);
	}

	public CustomJCheckBox(String text, boolean selected) {
	    super(text, selected);
	}

	public CustomJCheckBox(String text, Icon icon) {
		super(text, icon);
	}

	public CustomJCheckBox(String text, Icon icon, boolean selected) {
		super(text, icon, selected);
	}

	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}
}
