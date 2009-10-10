/*
 * 04/13/2004
 *
 * OptionsDialogPanel.java - Base class for option panels that
 * go into an OptionsDialog.
 * Copyright (C) 2003 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.fife.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;


/**
 * Base class for panels that go into an instance of <code>OptionsDialog</code>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class OptionsDialogPanel extends JPanel {

	/**
	 * The name of this options panel to be used in the the options dialog's
	 * selection tree.
	 */
	private String name;

	/**
	 * The icon to use for this options panel in the options dialog, if any.
	 */
	private Icon icon;

	/**
	 * Whether or not this panel has any unsaved changes.  The Options panel
	 * should set this whenever an Option value is changed by the user, but
	 * it doesn't have to clear it (this is done by the parent
	 * <code>OptionsDialog</code>).
	 */
	protected boolean hasUnsavedChanges;

	/**
	 * A collection of "child" option panels in the options dialog tree.
	 */
	private ArrayList childPanels;

	/**
	 * Parent panel.  null if no parent panel.
	 */
	private OptionsDialogPanel parent;


	/**
	 * Constructor.
	 */
	public OptionsDialogPanel() {
		this("<Unnamed>");
	}


	/**
	 * Constructor.
	 *
	 * @param name The name of this options panel to be used in the options
	 *        dialog's selection tree.
	 */
	public OptionsDialogPanel(String name) {
		this.name = name;
		this.hasUnsavedChanges = false;
		childPanels = new ArrayList(0);
	}


	/**
	 * Adds a "child" option panel to this one.
	 *
	 * @param child The option panel to add as a child.
	 * @see #getChildPanelCount
	 * @see #getChildPanel
	 */
	public void addChildPanel(final OptionsDialogPanel child) {
		childPanels.add(child);
		child.parent = this;
	}


	/**
	 * Applies the settings entered into this panel on the specified
	 * application.  Child panels are also handled.
	 *
	 * @param owner The application.
	 * @see #setValues(Frame)
	 */
	public final void doApply(Frame owner) {
		doApplyImpl(owner);
		for (int i=0; i<getChildPanelCount(); i++) {
			getChildPanel(i).doApply(owner);
		}
	}


	/**
	 * Applies the settings entered into this panel on the specified
	 * application.  Child panels are not handled.
	 *
	 * @param owner The application.
	 * @see #doApply(Frame)
	 */
	protected abstract void doApplyImpl(Frame owner);


	/**
	 * Checks whether or not all input the user specified on this panel is
	 * valid.  This should be overridden to check, for example, whether
	 * text fields have valid values, etc.  This method will be called
	 * whenever the user clicks "OK" or "Apply" on the options dialog to
	 * ensure all input is valid.  If it isn't, the component with invalid
	 * data will be given focus and the user will be prompted to fix it.
	 * 
	 *
	 * @return <code>null</code> if the panel has all valid inputs, or an
	 *         <code>OptionsPanelCheckResult</code> if an input was invalid.
	 *         This component is the one that had the error and will be
	 *         given focus, and the string is an error message that will be
	 *         displayed.
	 */
	public abstract OptionsPanelCheckResult ensureValidInputs();


	/**
	 * Returns the specified child option panel.
	 *
	 * @param index The index of the child option panel to return.
	 * @return The child option panel.
	 * @see #addChildPanel
	 * @see #getChildPanelCount
	 */
	public OptionsDialogPanel getChildPanel(int index) {
		return (OptionsDialogPanel)childPanels.get(index);
	}


	/**
	 * Gets the number of "child" option panels.
	 *
	 * @return The child option panel count.
	 * @see #addChildPanel
	 * @see #getChildPanel
	 */
	public int getChildPanelCount() {
		return childPanels.size();
	}


	/**
	 * Returns the icon to display for this options panel, if any.
	 *
	 * @return The icon for this options dialog panel, or <code>null</code>
	 *         if there isn't one.
	 * @see #setIcon(Icon)
	 */
	public Icon getIcon() {
		return icon;
	}


	/**
	 * Returns the string used to describe this panel in the left-hand
	 * tree pane of the options dialog.
	 *
	 * @return This option panel's name.
	 */
	public String getName() {
		return name;
	}


	/**
	 * Returns the Options dialog that contains this options panel.
	 *
	 * @return The options dialog, or <code>null</code> if this panel
	 *         hasn't been added to an Options dialog yet.
	 */
	public OptionsDialog getOptionsDialog() {
		Container parent = getParent();
		while (parent!=null && !(parent instanceof OptionsDialog))
			parent = parent.getParent();
		return (OptionsDialog)parent;
	}


	/**
	 * Returns the parent options dialog panel.  This value may be
	 * <code>null</code> if there is no parent panel (e.g. the parent
	 * of this panel is the options dialog itself).
	 *
	 * @return The parent options dialog panel.
	 */
	protected OptionsDialogPanel getParentPanel() {
		return parent;
	}


	/**
	 * Returns the <code>JComponent</code> at the "top" of this Options
	 * panel.  This is the component that will receive focus if the user
	 * switches to this Options panel in the Options dialog.  As an added
	 * bonus, if this component is a <code>JTextComponent</code>, its
	 * text is selected for easy changing.
	 */
	public abstract JComponent getTopJComponent();


	/**
	 * Returns whether or not this Options panel has unsaved changes.  Note
	 * that these changes may or may not be invalid.
	 *
	 * @see #setUnsavedChanges
	 */
	public boolean hasUnsavedChanges() {
		return hasUnsavedChanges;
	}


	/**
	 * Sets the icon to use for this option panel in the dialog.
	 *
	 * @param icon The icon to use, or <code>null</code> for none.
	 * @see #getIcon()
	 */
	public void setIcon(Icon icon) {
		this.icon = icon;
	}


	/**
	 * Sets the name of this options panel.
	 *
	 * @param name The name to use for this options panel.
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Sets whether or not the "Unsaved changes" flag for this Options panel
	 * is set.  You should call this method with a parameter set to
	 * <code>false</code> before displaying an Options dialog.
	 *
	 * @param hasUnsavedChanges Whether or not the flag should be set.
	 * @see #hasUnsavedChanges
	 */
	public void setUnsavedChanges(boolean hasUnsavedChanges) {
		this.hasUnsavedChanges = hasUnsavedChanges;
	}


	/**
	 * Sets the values displayed by this panel to reflect those in the
	 * application.  Child panels are also handled.
	 *
	 * @param owner The parent application.
	 * @see #doApply(Frame)
	 */
	public final void setValues(Frame owner) {
		setValuesImpl(owner);
		for (int i=0; i<getChildPanelCount(); i++) {
			getChildPanel(i).setValues(owner);
		}
		// Clear the "unsaved changes" flag for the panel.
		setUnsavedChanges(false);
	}


	/**
	 * Sets the values displayed by this panel to reflect those in the
	 * application.  Child panels are not handled.
	 *
	 * @param owner The parent application.
	 * @see #setValues(Frame)
	 */
	protected abstract void setValuesImpl(Frame owner);


	/**
	 * Returns the name of this options panel, since this is the value
	 * that is diaplayed in the Options dialog's JList.
	 */
	public final String toString() {
		return name;
	}


	/**
	 * The class that is returned from <code>ensureValidInputs</code>; it
	 * contains a <code>JComponent</code> that had invalid input, and a
	 * <code>String</code> to display as the error message.
	 */
	public static class OptionsPanelCheckResult {

		public OptionsDialogPanel panel;
		public JComponent component;
		public String errorMessage;

		public OptionsPanelCheckResult(OptionsDialogPanel panel) {
			this.panel = panel;
		}

		public OptionsPanelCheckResult(OptionsDialogPanel panel,
						JComponent component, String errorMessage) {
			this.panel = panel;
			this.component = component;
			this.errorMessage = errorMessage;
		}

	}


	/**
	 * A border useful for dividing sections of an Options panel.
	 */
	public static class OptionPanelBorder implements Border {

		private String title;
		private Insets insets;
		//private static final Font font = new Font("dialog", Font.PLAIN, 8);
		private Font font;
		private static final int HEIGHT	= 25;

		/**
		 * Constructor.
		 *
		 * @param title The title of the border.
		 */
		public OptionPanelBorder(String title) {
			this.title = title;
			insets = new Insets(HEIGHT,8,8,8);
		}

		/**
		 * Returns the insets of the border.
		 *
		 * @param c Not used.
		 */
		public Insets getBorderInsets(Component c) {
			return insets;
		}

		/**
		 * Returns whether or not the border is opaque.
		 *
		 * @return This method always returns <code>true</code>.
		 */
		public boolean isBorderOpaque() {
			return true;
		}

		/**
		 * Paints the border for the specified component with the specified
		 * position and size.
		 *
		 * @param c The component that has this border.
		 * @param g The graphics context with which to paint.
		 * @param x The x-coordinate of the border.
		 * @param y The y-coordinate of the border.
		 * @param width The width of the component.
		 * @param height The height of the component.
		 */
		public void paintBorder(Component c, Graphics g, int x, int y,
							int width, int height) {

			g.setColor(Color.BLUE);
			font = javax.swing.UIManager.getFont("Label.font");
			FontMetrics fm = c.getFontMetrics(font);
			int titleWidth = fm.stringWidth(title);
			int middleY = y + HEIGHT/2;
			int titleY = middleY + fm.getHeight()/2;

			ComponentOrientation orientation = c.getComponentOrientation();
			if (orientation.isLeftToRight()) {
				g.drawString(title, x,titleY);
				g.setColor(c.getBackground().darker());
				g.drawLine(x+titleWidth+5, middleY, x+width, middleY);
			}
			else {
				int titleX = x+width-titleWidth-1;
				g.drawString(title, titleX,titleY);
				g.setColor(c.getBackground().darker());
				g.drawLine(x,middleY, titleX-5,middleY);
			}

		}

	}


}