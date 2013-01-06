/*
 * 09/26/2009
 *
 * BreadcrumbBarTest.java - A simple test application for the breadcrumb bar.
 * Copyright (C) 2009 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.ui.breadcrumbbar;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.*;


public class BreadcrumbBarTest extends JFrame implements PropertyChangeListener{

	private JTextField tf;


	public BreadcrumbBarTest() {

		BreadcrumbBar bb = new BreadcrumbBar();
		bb.addPropertyChangeListener(BreadcrumbBar.PROPERTY_LOCATION, this);
		//bb.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		cp.add(bb, BorderLayout.NORTH);

		tf = new JTextField(bb.getShownLocation().getAbsolutePath());
		cp.add(tf, BorderLayout.SOUTH);

		setContentPane(cp);
		setTitle("BreadcrumbBar Test");
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

	}


	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String substanceClass = "org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel";
				UIManager.installLookAndFeel("Substance", substanceClass);
				//String laf = UIManager.getSystemLookAndFeelClassName();
				String laf = substanceClass;
				try {
					UIManager.setLookAndFeel(laf);
				} catch (Exception e) {
					e.printStackTrace();
				}
				new BreadcrumbBarTest().setVisible(true);
			}
		});
	}


	public void propertyChange(PropertyChangeEvent e) {

		String propName = e.getPropertyName();

		if (BreadcrumbBar.PROPERTY_LOCATION.equals(propName)) {
			File loc = (File)e.getNewValue();
			tf.setText(loc.getAbsolutePath());
		}
	}


}