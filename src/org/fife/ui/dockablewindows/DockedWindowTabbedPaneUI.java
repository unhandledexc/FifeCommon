/*
 * 10/21/2005
 *
 * DockedWindowTabbedPaneUI.java - UI used by the tabbed panes holding
 * dockable windows in a DockableWindowPanel.
 * Copyright (C) 2005 Robert Futrell
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
package org.fife.ui.dockablewindows;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;


/**
 * The UI for tabbed panes holding dockable windows in a
 * <code>DockableWindowPanel</code>.  This UI is designed to
 * resemble tabbed panes in Microsoft Visual Studio .NET.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see org.fife.ui.dockablewindows.DockableWindow
 * @see org.fife.ui.dockablewindows.DockableWindowPanel
 */
class DockedWindowTabbedPaneUI extends BasicTabbedPaneUI {

	private List croppedTitlesList;


	/**
	 * Cosntructor.
	 */
	public DockedWindowTabbedPaneUI() {
		croppedTitlesList = new ArrayList();
	}


	/**
	 * Invoked by <code>installUI</code> to create a layout manager
	 * object to manage the <code>JTabbedPane</code>.
	 *
	 * @return A layout manager object.
	 * @see TabbedPaneLayout
	 * @see javax.swing.JTabbedPane#getTabLayoutPolicy
	 */
	protected LayoutManager createLayoutManager() {
		return new PluginTabbedPaneLayout();
	}


	/**
	 * Returns an instance of this UI for the specified tabbed pane.
	 *
	 * @param c The tabbed pane.
	 * @return An instance of this UI.
	 */
	public static ComponentUI createUI(JComponent c) {
		return new DockedWindowTabbedPaneUI();
	}


	/**
	 * Returns the minimum size for the tabbed pane.  This is overridden so
	 * that the tabbed pane can be given a very small width.  Tabbed panes'
	 * layout managers by default give them a minimum width equal to one of
	 * their tabs' widths, but we don't want to do that.
	 *
	 * @param c The tabbed pane.
	 * @return The minimum size for this tabbed pane.
	 */
	public Dimension getMinimumSize(JComponent c) {
		return new Dimension(60, 64);
	}


	/**
	 * Returns the insets for a tab.  Overridden to handle the case where
	 * the current LookAndFeel doesn't extend BasicLookAndFeel (and so
	 * some properties that BasicTabbedPaneUI that rely on aren't set).
	 *
	 * @param tabPlacement
	 * @param tabIndex
	 */
	protected Insets getTabInsets(int tabPlacement, int tabIndex) {
		// Happens when LAF is set to something other than a subclass of 
		// BasicLookAndFeel (e.g. GTK, Synth, Nimbus).  In that case, it's
		// possible that the LAF doesn't set some properties set in
		// BasicLookAndFeel that BasicTabbedPaneUI (and hence its subclasses)
		// assume are set.  See:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6634504
		if (tabInsets==null) {
			tabInsets = new Insets(0, 0, 0, 0);
		}
		return tabInsets;
	}


	/**
	 * Caches insets, colors, fonts, etc.  This method is overridden so we
	 * can set custom values for insets.
	 */
	protected void installDefaults() {
		super.installDefaults();
		contentBorderInsets = new Insets(0,0,2,0);
		tabAreaInsets = new Insets(2, 4, 0, 4);
	}


	/**
	 * Arranges the text, icon, etc. on a tab, and caches the "cropped"
	 * version of the tab's title, if necessary.
	 */
	protected void layoutLabel(int tabPlacement, 
						FontMetrics metrics, int tabIndex,
						String title, Icon icon,
						Rectangle tabRect, Rectangle iconRect, 
						Rectangle textRect, boolean isSelected ) {

		textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

		View v = getTextViewForTab(tabIndex);
		if (v != null)
			tabPane.putClientProperty("html", v);

		String croppedTitle = null;
		try {
			croppedTitle = SwingUtilities.layoutCompoundLabel(
											tabPane,
											metrics, title, icon,
											SwingUtilities.CENTER,
											SwingUtilities.CENTER,
											SwingUtilities.CENTER,
											SwingUtilities.TRAILING,
											tabRect,
											iconRect,
											textRect,
											textIconGap);
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			// Workaround for bug in some older 1.6 JRE's, which manifests
			// itself seemingly only when a RTL locale is used (such as
			// Arabic), and the "plugins" area of a split pane is made so
			// small that the plugins' names on the tabs has to be abbreviated
			// into "...".  If this happens on startup, it can cause some bad
			// problems.
			croppedTitle = "...";
		}

		int croppedCount = croppedTitlesList.size();
		int tabCount = tabPane.getTabCount();
		if (croppedCount<tabCount) {
			for (int i=croppedCount; i<tabCount; i++)
				croppedTitlesList.add("");
		}
		croppedTitlesList.set(tabIndex, croppedTitle);

		tabPane.putClientProperty("html", null);

	}


	protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
									int selectedIndex, 
									int x, int y, int w, int h) {
	}


	protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
									int selectedIndex, 
									int x, int y, int w, int h) {

		//g.setColor(UIManager.getColor("TabbedPane.shadow"));
		g.setColor(UIManager.getColor("TabbedPane.foreground"));
		y = y+h-1;
		if ((selectedIndex<0)) {
			g.drawLine(x+1,y, x+w-2,y);
		}
		else {
			// Break line to show visual connection to selected tab
			Rectangle selRect = getTabBounds(selectedIndex, calcRect);
			g.drawLine(x+1,y, selRect.x,y);
			if (selRect.x+selRect.width-1 < x+w-2)
				g.drawLine(selRect.x+selRect.width-1,y, x+w-2,y);
		}
	}


	protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
										int selectedIndex, 
										int x, int y, int w, int h) {
	}


	protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
										int selectedIndex, 
										int x, int y, int w, int h) {
	}


	/**
	 * Paints the background of a tab.
	 */
	protected void paintTabBackground(Graphics g, int tabPlacement,
			int tabIndex, int x, int y, int w, int h, boolean isSelected) {
		if (isSelected) {
			g.setColor(UIManager.getColor("TabbedPane.highlight"));
			//g.setColor(UIManager.getColor("Panel.background"));
			g.fillRect(x,y, w,h);
		}
	}


	/**
	 * Paints the border of a tab.
	 */
	protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
						int x, int y, int w, int h, boolean isSelected) {
		// NOTE: We assume the tab placement is JTabbedPane.BOTTOM.
		//g.setColor(UIManager.getColor("TabbedPane.shadow"));
		int x2 = x + w - 1;
		if (isSelected) {
			g.setColor(UIManager.getColor("TabbedPane.foreground"));
			int y2 = y + h - 1;
			g.drawLine(x,y2, x2,y2);
			g.drawLine(x2,y2, x2,y);
		}
		else if (tabPane.getSelectedIndex()!=tabIndex+1) {
			g.setColor(UIManager.getColor("TabbedPane.shadow"));
			if (!tabPane.getComponentOrientation().isLeftToRight()) {
				x2 = x; // RTL languages show tab "edge" on other side.
			}
			g.drawLine(x2,y+2, x2,y+h-3);
		}
	}


	/**
	 * Paints the text on a tab (the tab's title).
	 */
	protected void paintText(Graphics g, int tabPlacement,
						Font font, FontMetrics metrics, int tabIndex,
						String title, Rectangle textRect, 
						boolean isSelected) {
		// Get our (probably) cropped title; don't use the uncropped one.
		title = (String)croppedTitlesList.get(tabIndex);
		g.setFont(font);
		Color fg = tabPane.getForegroundAt(tabIndex);
//		if (isSelected && (fg instanceof UIResource)) {
if (isSelected) {
			Color selectedFG = UIManager.getColor(
								"TabbedPane.selectedForeground");
			if (selectedFG != null)
				fg = selectedFG;
		}
else {
	fg = UIManager.getColor("TabbedPane.shadow");
}
		g.setColor(fg);
		g.drawString(title, textRect.x, textRect.y+metrics.getAscent());
	} 


	/**
	 * Tab layout for a plugin tabbed pane.  All tabs area always in a single
	 * run (row).  If there is not enough room for all of the tabs in one run,
	 * the space available is divided equally among all of the tabs.<p>
	 *
	 * Much of this code is stolen from <code>BasicTabbedPaneUI</code>'s
	 * layout subclasses.
	 */
	class PluginTabbedPaneLayout extends TabbedPaneLayout {

		public PluginTabbedPaneLayout() {
		}

		protected int preferredTabAreaHeight(int tabPlacement, int width) {
			return calculateMaxTabHeight(tabPlacement);
		}

		protected int preferredTabAreaWidth(int tabPlacement, int height) {
			return calculateMaxTabWidth(tabPlacement);
		}

		public void layoutContainer(Container parent) {

			int tabPlacement = tabPane.getTabPlacement();
			Insets insets = tabPane.getInsets();
			int selectedIndex = tabPane.getSelectedIndex();
			Component visibleComponent = getVisibleComponent();

			calculateLayoutInfo();

			if (selectedIndex < 0) {
				if (visibleComponent != null) {
					// The last tab was removed, so remove the component
					setVisibleComponent(null);
				}
			}
			else {
				Component selectedComponent = tabPane.getComponentAt(selectedIndex);
				if (selectedComponent != null) {
					setVisibleComponent(selectedComponent);
				}

				int /*tx, ty, tw, */th; // tab area bounds
				int cx, cy, cw, ch; // content area bounds
				Insets contentInsets = getContentBorderInsets(tabPlacement);
				Rectangle bounds = tabPane.getBounds();
				int numChildren = tabPane.getComponentCount();

				if (numChildren > 0) {

					// NOTE: Here we're assuming tabs are on the bottom.
					// Calculate tab area bounds
					//tw = bounds.width - insets.left - insets.right;
					th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
					//tx = insets.left;
					//ty = bounds.height - insets.bottom - th;

					// calculate content area bounds
					cx = insets.left + contentInsets.left;
					cy = insets.top + contentInsets.top;
					cw = bounds.width - insets.left - insets.right - 
						contentInsets.left - contentInsets.right;
					ch = bounds.height - insets.top - insets.bottom - th -
						contentInsets.top - contentInsets.bottom;

					for (int i=0; i < numChildren; i++) {
						Component child = tabPane.getComponent(i);
						child.setBounds(cx, cy, cw, ch);
					}

				}

			}

		}

		/**
		 * Calculates the bounds for each tab.
		 *
		 * @param tabPlacement Always <code>JTabbedPane.BOTTOM</code>.
		 * @param tabCount The number of tabs.
		 */
		protected void calculateTabRects(int tabPlacement, int tabCount) {

			FontMetrics metrics = getFontMetrics();
			Dimension size = tabPane.getSize();
			Insets insets = tabPane.getInsets(); 
			Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
			//int fontHeight = metrics.getHeight();
			//int selectedIndex = tabPane.getSelectedIndex();
			boolean leftToRight = tabPane.getComponentOrientation().isLeftToRight();

			// Since we'll only ever have one run of tabs, the tab area's
			// height should be the height of the tallest tab.
			maxTabHeight = calculateMaxTabHeight(tabPlacement);

			// Get the starting x and y for the tab run.
			int x = tabAreaInsets.left;
			int y = size.height - insets.bottom - tabAreaInsets.bottom - maxTabHeight;

			runCount = 1;	// Always 1 run count (needed by other methods).

			if (tabCount == 0)
				return;

			// Compute the width of the area in which to put the tabs.
			int tabAreaWidth = size.width - insets.left - insets.right -
							tabAreaInsets.left - tabAreaInsets.right;

			// Compute the width the tabs will take up if they all get the
			// amount of space they want.
			int allTabsPreferredWidth = 0;
			for (int i=0; i<tabCount; i++) {
				allTabsPreferredWidth += calculateTabWidth(tabPlacement, i, metrics);
			}

			// First check to see whether all tabs fit in the tab area
			// width fully.  If they do, give them all the space they need.
			if (allTabsPreferredWidth <= tabAreaWidth) {
				for (int i=0; i<tabCount; i++) {
					Rectangle rect = rects[i];
					if (i > 0) {
						rect.x = rects[i-1].x + rects[i-1].width;
					}
					else {
						tabRuns[0] = 0;
						maxTabWidth = 0;
						rect.x = x;
					}
					rect.width = calculateTabWidth(tabPlacement, i, metrics);
					maxTabWidth = Math.max(maxTabWidth, rect.width);
					rect.y = y;
					rect.height = maxTabHeight;
				}
			}

			// If they all don't fit, make each tab have an equal share of
			// the space available.
			else {
				maxTabWidth = tabAreaWidth/tabCount;
				for (int i=0; i<tabCount; i++) {
					Rectangle rect = rects[i];
					rect.x = x + i*maxTabWidth;
					rect.y = y;
					rect.width = maxTabWidth;
					rect.height = maxTabHeight;
				}
			}

			// If right to left, flip x positions and adjust by widths
			if (!leftToRight) {
				int rightMargin = size.width 
							- (insets.right + tabAreaInsets.right);
				for (int i=0; i<tabCount; i++)
					rects[i].x = rightMargin - rects[i].x - rects[i].width;
			}

		}

	}


}