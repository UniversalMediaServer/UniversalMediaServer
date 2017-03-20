package net.pms.newgui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class CustomTabbedPaneUI extends BasicTabbedPaneUI {
	private int anchoFocoH = 4;

	public static ComponentUI createUI(JComponent c) {
		return new CustomTabbedPaneUI();
	}

	@Override
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
		if (isSelected) {
			g.setColor(new Color(0, 0, 0));
		}

		super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	}

	@Override
	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
	}

	@Override
	protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
		return 10 + super.calculateTabWidth(tabPlacement, tabIndex, metrics);
	}

	@Override
	protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
		if (tabPlacement == LEFT || tabPlacement == RIGHT) {
			return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
		} else {
			return anchoFocoH + super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
		}
	}
}
