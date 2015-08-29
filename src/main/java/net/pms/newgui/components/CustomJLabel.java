package net.pms.newgui.components;

import javax.swing.JLabel;
import javax.swing.JToolTip;

public class CustomJLabel extends JLabel {
	private static final long serialVersionUID = -6726814430751911120L;

	public CustomJLabel() {
	    super();
	}

	public CustomJLabel(String text) {
	    super(text);
	}

	@Override
	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}

	@Override
	public void setText(String text) {
		super.setText(htmlify(text));
	}
	
	public static String htmlify(String text) {
		String t = text.trim().toLowerCase();
		return (t.startsWith("<html>") ? "" : "<html>") + text + (t.endsWith("</html>") ? "" : "</html>");
	}
}
