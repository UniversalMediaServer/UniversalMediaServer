package net.pms.newgui.components;

import java.awt.Component;
import javax.swing.JLabel;

public class OrientedSpanBuilder extends OrientedPanelBuilder {
	private int x;

	public OrientedSpanBuilder(String colSpec) {
		super(colSpec, "fill:pref:grow");
		x = 1;
	}

	public JLabel append(String label) {
		JLabel l =_addLabel(label, cc.xy(x, 1));
		x += 2;
		return l;
	}

	public Component append(Component c) {
		add(c, cc.xy(x, 1));
		x += 2;
		return c;
	}
}
