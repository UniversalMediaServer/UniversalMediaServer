package net.pms.newgui.components;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;


/**
 * This is a custom {@link HTMLEditorKit} that doesn't use the application
 * shared {@link StyleSheet}. If using the standard {@link HTMLEditorKit}
 * any changes to the {@link StyleSheet} will apply to all
 * {@link HTMLEditorKit}s in the application, practically rendering
 * {@link StyleSheet}s useless.
 *
 * @author Nadahar
 * @since 5.4.0
 */
public class CustomHTMLEditorKit extends HTMLEditorKit{

	private static final long serialVersionUID = -4110333075630471497L;
	private StyleSheet customStyleSheet;

    @Override
    public void setStyleSheet(StyleSheet styleSheet) {
        customStyleSheet = styleSheet;
    }
    @Override
    public StyleSheet getStyleSheet() {
        if (customStyleSheet == null) {
            customStyleSheet = super.getStyleSheet();
        }
        return customStyleSheet;
    }
}
