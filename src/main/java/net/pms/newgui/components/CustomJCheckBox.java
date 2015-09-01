package net.pms.newgui.components;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JToolTip;
import static net.pms.newgui.GuiUtil.htmlify;

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

	String prevText = null;
	int hitWidth = -1;

	@Override
	public void setText(String text) {
		if (text == null || !text.equals(prevText)) {
			// Mark as changed
			hitWidth = -1;
		}
		prevText = text;
		super.setText(htmlify(text));
	}

	@Override
	public void paint(Graphics g) {
		if (hitWidth == -1) {
			// Text has changed, recalculate the hit zone
			// FIXME: this is just a "close enough" fudge
			hitWidth = g.getFontMetrics().stringWidth(getText()) - 75;
		}
		super.paint(g);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		// Block click events beyond the hit zone
		if (e.getClickCount() == 0 || e.getX() < hitWidth) {
			super.processMouseEvent(e);
		}
	}
}
