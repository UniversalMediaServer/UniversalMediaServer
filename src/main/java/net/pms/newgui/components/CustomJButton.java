package net.pms.newgui.components;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolTip;

public class CustomJButton extends JButton {
	private static final long serialVersionUID = -528428545289132331L;

	public CustomJButton() {
		super();
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(Action a) {
		super(a);
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(Icon icon) {
		super(null, icon);
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(String text) {
		super(text);
		this.setRequestFocusEnabled(false);
	}

	public CustomJButton(String text, Icon icon) {
		super(text, icon);
		this.setRequestFocusEnabled(false);
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
