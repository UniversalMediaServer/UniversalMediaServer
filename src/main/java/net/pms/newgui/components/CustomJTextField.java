package net.pms.newgui.components;

import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.text.Document;

public class CustomJTextField extends JTextField {
	private static final long serialVersionUID = -5697643578934331831L;

	public CustomJTextField() {
	    super();
	}

	public CustomJTextField(int columns) {
	    super(columns);
	}

	public CustomJTextField(String text) {
	    super(text);
	}

	public CustomJTextField(String text, int columns) {
	    super(text, columns);
	}

	public CustomJTextField(Document doc, String text, int columns) {
	    super(doc, text, columns);
	}

	public JToolTip createToolTip() {
	    JToolTip tip = new HyperLinkToolTip();
	    tip.setComponent(this);
	    return tip;
	}
}

