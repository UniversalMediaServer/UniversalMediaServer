package net.pms.newgui.components;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolTip;

public class CustomJButton extends JButton {
	private static final long serialVersionUID = -528428545289132331L;

	public CustomJButton() {
		super();
	}

	public CustomJButton(String string) {
		super(string);
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(Icon icon) {
		super(null, icon);
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(String string, Icon icon) {
		super(string, icon);
		this.setRequestFocusEnabled(false);
	}

	@Override
	public JToolTip createToolTip() {
		JToolTip tip = new HyperLinkToolTip();
		tip.setComponent(this);
		return tip;
	}
}
