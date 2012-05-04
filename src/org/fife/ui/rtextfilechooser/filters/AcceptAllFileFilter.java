/*
 * 05/04/2012
 *
 * EverythingFilter - File filter that accepts all files.
 * Copyright (C) 2012 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.ui.rtextfilechooser.filters;

import java.io.File;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;


/**
 * File filter that accepts all files.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class AcceptAllFileFilter extends FileFilter {


	public boolean accept(File f) {
		return true;
	}


	public String getDescription() {
		return UIManager.getString("FileChooser.acceptAllFileFilterText");
	}


}