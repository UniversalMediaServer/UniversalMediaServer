package net.pms.newgui.components;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.*;
import java.awt.Component;
import javax.swing.*;
import static net.pms.newgui.GuiUtil.htmlify;

public class CustomPanelBuilder extends PanelBuilder {
//	private static final long serialVersionUID = -6726814430751911120L;

	public CustomPanelBuilder(FormLayout layout) {
		super(layout);
	}
	
	public CustomPanelBuilder(FormLayout layout, JPanel panel) {
		super(layout, panel);
	}

	public JLabel _addLabel(String textWithMnemonic) {
		return super.addLabel(htmlify(textWithMnemonic));
	}

	public JLabel _addLabel(String textWithMnemonic, CellConstraints constraints) {
		return super.addLabel(htmlify(textWithMnemonic), constraints);
	}

	public JLabel _addLabel(String textWithMnemonic, String encodedConstraints) {
		return super.addLabel(htmlify(textWithMnemonic), encodedConstraints);
	}

	public JLabel _addLabel(String textWithMnemonic, CellConstraints labelConstraints,
			Component component, CellConstraints componentConstraints) {
		return super.addLabel(htmlify(textWithMnemonic), labelConstraints, component, componentConstraints);
	}
}
