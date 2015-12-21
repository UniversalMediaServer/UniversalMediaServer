package net.pms.newgui.components;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JToolTip;
import net.pms.newgui.GuiUtil;

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
		super.setText(GuiUtil.htmlify(text));
	}

	@Override
	public void paint(Graphics g) {
		if (hitWidth == -1) {
			// Text has changed, recalculate the hit zone
			// Note: stripping html here to get accurate stringWidth()
			hitWidth = g.getFontMetrics().stringWidth(GuiUtil.deHtmlify(getText()))
				+ getIconTextGap() + (getIcon() != null ? getIcon().getIconWidth() : 16);
		}
		super.paint(g);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		// Block click events beyond the hit zone, adjusting for orientation
		if (e.getClickCount() == 0 || (getComponentOrientation().isLeftToRight() ? e.getX() : getWidth() - e.getX()) < hitWidth) {
			super.processMouseEvent(e);
		}
	}
}
