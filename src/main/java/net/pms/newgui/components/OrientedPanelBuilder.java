package net.pms.newgui.components;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import net.pms.PMS;
import net.pms.util.FormLayoutUtil;
import static net.pms.newgui.GuiUtil.htmlify;

public class OrientedPanelBuilder extends PanelBuilder {
	protected String colSpec;
	protected static ComponentOrientation defaultOrientation = ComponentOrientation.getOrientation(PMS.getLocale());
	protected ComponentOrientation orientation;
	protected CellConstraints cc;

	public OrientedPanelBuilder(String colSpec) {
		this(colSpec, defaultOrientation);
	}

	public OrientedPanelBuilder(String colSpec, String rowSpec) {
		this(colSpec, rowSpec, defaultOrientation);
	}

	public OrientedPanelBuilder(String colSpec, String rowSpec, ComponentOrientation orientation) {
		super(new FormLayout(FormLayoutUtil.getColSpec(colSpec, orientation), rowSpec));
		init(colSpec, orientation);
	}

	public OrientedPanelBuilder(String colSpec, ComponentOrientation orientation) {
		super(new FormLayout(FormLayoutUtil.getColSpec(colSpec, orientation)));
		init(colSpec, orientation);
	}

	private void init(String colSpec, ComponentOrientation orientation) {
		this.colSpec = colSpec;
		this.orientation = orientation;
		setLeftToRight(orientation.isLeftToRight());
		cc = new CellConstraints();
		cc.honorsVisibility = true;
	}

	public CellConstraints getCellConstraints() {
		return cc;
	}

	public String getColSpec() {
		return colSpec;
	}

	public ComponentOrientation getOrientation() {
		return orientation;
	}

	@Override
	public Component add(Component c, CellConstraints cc) {
		return super.add(c, FormLayoutUtil.flip(cc, colSpec, orientation));
	}

	// Jgoodies authors in their infinite wisdom have declared almost all methods final,
	// so the '_' functions below wrap their final PanelBuilder counterparts, adding
	// orientation handling

	public JComponent _addSeparator(String label, CellConstraints cc) {
		return addSeparator(htmlify(label, orientation.isLeftToRight()), cc);
	}

	public JLabel _addLabel(String label, CellConstraints cc) {
		return addLabel(htmlify(label, orientation.isLeftToRight()), cc);
	}

	public JPanel _getPanel() {
		JPanel panel = super.getPanel();
		panel.applyComponentOrientation(orientation);
		return panel;
	}

	public DefaultTableCellRenderer getOrientedCellRenderer() {
		DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
		cellRenderer.setHorizontalAlignment(orientation.isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
		return cellRenderer;
	}
}
