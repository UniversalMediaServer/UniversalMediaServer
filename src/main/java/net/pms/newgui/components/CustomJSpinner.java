/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.newgui.components;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.annotation.Nonnull;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import net.pms.PMS;

/**
 * A subclass of JSpinner that implements support for {@link SpinnerIntModel}
 * and mouse wheel scrolling when {@link SpinnerIntModel} is used.
 *
 * @author Nadahar
 */
@SuppressWarnings({ "serial", "rawtypes" })

public class CustomJSpinner extends javax.swing.JSpinner {

	/**
	 * Creates a new <code>CustomJSpinner</code> with default instance of
	 * {@link SpinnerIntModel} (that is, a model with value 0, step size 1, and
	 * no upper or lower limit).
	 *
	 * @see SpinnerIntModel
	 */
	public CustomJSpinner(boolean enterMoveFocus) {
		this(new SpinnerIntModel(0), enterMoveFocus);
	}

	/**
	 * Creates a new <code>JSpinner with the specified model.  The
	 * {@link #createEditor(SpinnerModel)} method is used to create an editor
	 * that is suitable for the model.
	 *
	 * @param model
	 *            the model (<code>null</code> not permitted).
	 *
	 * @throws NullPointerException
	 *             if <code>model</code> is <code>null</code>.
	 */
	public CustomJSpinner(@Nonnull SpinnerModel model, boolean enterMoveFocus) {
		super(model);
		if (model instanceof SpinnerIntModel) {
			this.addMouseWheelListener(new MouseWheelRoll(
				this,
				((SpinnerIntModel) model).getMinimum(),
				((SpinnerIntModel) model).getMaximum(),
				((SpinnerIntModel) model).getStepSize()
			));
		}

		if (enterMoveFocus) {
			if (this.getEditor() instanceof CustomJSpinner.IntegerEditor) {
				((CustomJSpinner.IntegerEditor) this.getEditor()).getTextField().addKeyListener(new KeyListener() {
					@Override
					public void keyTyped(KeyEvent e) {}
					@Override
					public void keyReleased(KeyEvent e) {}
					@Override
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_ENTER) {
							FocusManager.getCurrentManager().focusNextComponent();
						}
					}
				});
			}
		}
	}

	protected JComponent createEditor(SpinnerModel model) {
		if (model instanceof SpinnerDateModel) {
			return new DateEditor(this);
		} else if (model instanceof SpinnerNumberModel) {
			return new NumberEditor(this);
		} else if (model instanceof SpinnerListModel) {
			return new ListEditor(this);
		} else if (model instanceof SpinnerIntModel) {
			return new IntegerEditor(this);
		} else {
			return new DefaultEditor(this);
		}
	}

	/**
	 * This {@link MouseWheelListener} makes the spinner increment or decrement
	 * its value by rolling the mouse wheel.
	 *
	 * @author Nadahar
	 */
	protected static class MouseWheelRoll implements MouseWheelListener {

		private int minimum, maximum, stepSize;
		private CustomJSpinner spinner;

		private MouseWheelRoll(CustomJSpinner spinner, int minimum, int maximum, int stepSize) {
			this.spinner = spinner;
			this.minimum = minimum;
			this.maximum = maximum;
			this.stepSize = stepSize;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (!spinner.isEnabled()) {
				return;
			}
	        if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
	            return;
	        }

	        int value = (Integer) spinner.getValue();
	        value -= e.getWheelRotation() * stepSize;
	        if (e.getWheelRotation() < 0 && value == minimum + stepSize && minimum % stepSize != 0) {
	        	value = ((minimum / stepSize) * stepSize) + stepSize;
	        } else if (e.getWheelRotation() > 0 && value == maximum - stepSize && maximum % stepSize != 0) {
	        	value = (maximum / stepSize) * stepSize;
	        }
			value = Math.min(Math.max(value, minimum),maximum);
	        spinner.setValue(value);
		}
	}

    /**
     * An editor for a <code>JSpinner</code> whose model is a
     * <code>SpinnerIntModel</code>.  The value of the editor is
     * displayed with a <code>JFormattedTextField</code> whose format
     * is defined by a <code>NumberFormatter</code> instance whose
     * <code>minimum</code> and <code>maximum</code> properties
     * are mapped to the <code>SpinnerIntModel</code>.
     */
    public static class IntegerEditor extends DefaultEditor
    {
        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerIntModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>IntegerEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerIntModel</code>
         *
         * @see #getModel
         * @see #getFormat
         * @see SpinnerIntModel
         */
        public IntegerEditor(JSpinner spinner) {
            this(spinner, NumberFormat.getIntegerInstance(PMS.getLocale()));
        }

        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerIntModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>IntegerEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @param format the <code>NumberFormat</code> object that's used to display
         *     and parse the value of the text field.
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerIntModel</code>
         *
         * @see #getTextField
         * @see SpinnerIntModel
         * @see java.text.DecimalFormat
         */
        private IntegerEditor(JSpinner spinner, NumberFormat format) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerIntModel)) {
                throw new IllegalArgumentException(
                          "model not a SpinnerIntModel");
            }

            format.setGroupingUsed(false);
            format.setMaximumFractionDigits(0);
            SpinnerIntModel model = (SpinnerIntModel)spinner.getModel();
            NumberFormatter formatter = new IntegerEditorFormatter(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
            ftf.setHorizontalAlignment(JTextField.RIGHT);

			try {
				String minString = formatter.valueToString(model.getMinimum());
				String maxString = formatter.valueToString(model.getMaximum());
				// Trying to approximate the width difference between "m" and "0" by multiplying with 0.7
				ftf.setColumns((int) Math.round(0.7 * Math.max(maxString.length(), minString.length())));
			} catch (ParseException e) {
				// Nothing to do, the component width will simply be the default
			}
		}

        /**
         * This subclass of javax.swing.NumberFormatter maps the minimum/maximum
         * properties to a SpinnerIntModel and initializes the valueClass
         * of the NumberFormatter to match the type of the initial models value.
         */
        private static class IntegerEditorFormatter extends NumberFormatter {
            private final SpinnerIntModel model;

            IntegerEditorFormatter(SpinnerIntModel model, NumberFormat format) {
                super(format);
                this.model = model;
                setValueClass(model.getValue().getClass());
            }

			public Comparable getMinimum() {
                return  model.getMinimum();
            }

			@Override
			public Comparable getMaximum() {
				return model.getMaximum();
			}
		}

        /**
         * Return our spinner ancestor's <code>SpinnerIntModel</code>.
         *
         * @return <code>getSpinner().getModel()</code>
         * @see #getSpinner
         * @see #getTextField
         */
        public SpinnerIntModel getModel() {
            return (SpinnerIntModel)(getSpinner().getModel());
        }
    }
}
