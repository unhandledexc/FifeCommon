/*
 * 08/09/2004
 *
 * FSATextField.java - A "File System-Aware" text field that keeps a drop-down
 * list popuplated with files matching the text typed in by the user.
 * Copyright (C) 2004 Robert Futrell
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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;


/**
 * A "File System-Aware" text field.  When the user is typing into this text
 * field, it automagically locates all files that begin with the text typed in,
 * and populates a text field-style list with file choices.  This is similar to
 * the text field found in the "Run" dialog in Microsoft Windows.<p>
 * When using this component, you MUST set the parent window (i.e., the
 * window that contains this text field) so that the drop-down file menu
 * properly disappears/resizes, etc.  For example:<p>
 * <code>
 *   public class MyDialog extends JDialog {
 *      ...
 *      public MyDialog() {
 *         FSATextField textField = new FSATextField();
 *         fsaTextField.discoverParentWindow();
 *         ...
 *         getContentPane().add(fsaTextField);
 * </code></p>
 *
 * @author Robert Futrell
 * @version 0.4
 */
public class FSATextField extends JTextField implements ComponentListener,
						DocumentListener, ListSelectionListener {

	private static final long serialVersionUID = 1L;

	/**
	 * Whether or not the underlying OS is case-insensitive.
	 */
	private static final boolean IGNORE_CASE = FSATextField.getIgnoreCase();

	/**
	 * File filter used if directories-only mode is set.
	 */	
	private FilenameFilter directoriesOnlyFilenameFilter;

	/**
	 * These are an attempt to speed things up in the "common" case.  If the
	 * user types a letter into the text field but the last position of a
	 * file separator is the same as it was last time, then the "current
	 * directory" hasn't changed, so we can just keep the File instance for
	 * the directory we already have.  We do this because doing a regex match
	 * loop through multiple file names can be slow on some systems.
	 */
	private File directory;
	private String dirName;

	/**
	 * Whether the next matching filename is auto-inerted when the drop-down
	 * list is visible.  This property is only honored when this text field
	 * is "file system aware."
	 */
	private boolean autoCompleteFileName;

	/**
	 * Variables used by the text field to implement the "combo-box"
	 * style list.
	 */
	private JList fileList;
	private DefaultListModel fileListModel;
	private JScrollPane scrollPane;
	private JWindow popupWindow;
	private boolean directoriesOnly;
	private long lastCount = -1;
	private boolean ignoreDocumentUpdates = false;

	private Runnable listValueChangedRunnable;

	/**
	 * Cached values of the files in the current directory and the number
	 * of files in the current directory.
	 */
	private String[] containedFiles;
	private int num;

	/**
	 * The "current directory;" if the user is typing a relative pathname
	 * (i.e., not absolute), then assume this is the root directory.
	 */
	private String currentDirectory;

	/**
	 * Whether or not the text field pops up a list of matching files.
	 */
	private boolean fileSystemAware;

	private Window parent;
	private JPanel contentPane;


	/**
	 * Creates a new FSATextField that previews both files and directories,
	 * and whose current directory is the program's current directory.
	 */
	public FSATextField() {
		this(false, System.getProperty("user.dir"));
	}


	/**
	 * Creates a new FSATextField.
	 *
	 * @param directoriesOnly Whether this text field should preview only
	 *        directories (or both files and directories).
	 * @param currentDirectory The directory for which the text field should
	 *        assume relative filenames are in.
	 */
	public FSATextField(boolean directoriesOnly, File currentDirectory) {
		this(directoriesOnly, currentDirectory.getAbsolutePath());
	}


	/**
	 * Creates a new FSATextField.
	 *
	 * @param directoriesOnly Whether this text field should preview only
	 *        directories (or both files and directories).
	 * @param currentDirectory The directory for which the text field should
	 *        assume relative filenames are in.
	 */
	public FSATextField(boolean directoriesOnly, String currentDirectory) {

		enableEvents(AWTEvent.FOCUS_EVENT_MASK);
		setDirectoriesOnly(directoriesOnly);

		fileListModel = new DefaultListModel();
		fileList = new JList(fileListModel);
		fileList.addListSelectionListener(this);
		contentPane = new JPanel(new GridLayout(1,1));
		scrollPane = new JScrollPane(fileList);
		contentPane.add(scrollPane);

		setCurrentDirectory(currentDirectory);

		fileSystemAware = true;
		setAutoCompleteFileName(true);

		listValueChangedRunnable = new ListValueChangedRunnable();

		installStandardKeyActions();

	}


	/**
	 * Adds an item to the file list.
	 *
	 * @param item The item to add to the file list.
	 * @see #removeAllItems
	 */
	public void addItem(String item) {
		fileListModel.addElement(item);
	}


	/**
	 * Fills in the remainder of the next "matching" file name into this text
	 * field.  The next "matching" file is the first file displayed in the
	 * drop-down list.  This effect is similar to that seen in Microsoft
	 * Office.  The text will be selected so it can be overwritten by the
	 * user typing.
	 *
	 * @param entered The text currently in the text field, or
	 *        <code>null</code> if the drop-down list is not currently
	 *        visible.
	 * @see #getAutoCompleteFileName()
	 * @see #setAutoCompleteFileName(boolean)
	 */
	private void autoCompleteFileName(String entered) {
		//if (popupWindow!=null && popupWindow.isVisible()) {
		if (entered!=null) { // popup must be visible
			char ch = entered.charAt(entered.length()-1);
			if (ch!='/' && ch!='\\') { // Don't auto-complete for dirs.
				String value = (String)fileListModel.get(0);
				int len = getDocument().getLength();
				if (getCaretPosition()==len && len<value.length()) {
					String remaining = value.substring(len);
					ignoreDocumentUpdates = true;
					replaceSelection(remaining);
					select(len, getDocument().getLength());
					ignoreDocumentUpdates = false;
				}
			}
		}
	}


	/**
	 * Called when the text component's document receives a style change.
	 * Since it is a plain document, this is never called.
	 */
	public void changedUpdate(DocumentEvent e) {
	}


	/**
	 * Called when the parent dialog/frame of this text field is hidden.
	 * This should not be overridden.
	 */
	public void componentHidden(ComponentEvent e) {
		processParentComponentEvent(e);
	}


	/**
	 * Called when the parent dialog/frame of this text field is moved.
	 * This should not be overridden.
	 */
	public void componentMoved(ComponentEvent e) {
		processParentComponentEvent(e);
	}


	/**
	 * Called when the parent dialog/frame of this text field is resized.
	 * This should not be overridden.
	 */
	public void componentResized(ComponentEvent e) {
		processParentComponentEvent(e);
	}


	/**
	 * Called when the parent dialog/frame of this text field is shown.
	 * This should not be overridden.
	 */
	public void componentShown(ComponentEvent e) {
		processParentComponentEvent(e);
	}


	/**
	 * Creates the popup window for this text field.
	 *
	 * @return The popup window.
	 */
	private JWindow createPopupWindow() {

		//System.err.println("... Creating a new popupWindow!");
		JWindow popupWindow = new JWindow(parent);

		popupWindow.setFocusableWindowState(false);
		popupWindow.setContentPane(contentPane);

		return popupWindow;

	}


	/**
	 * Sets the parent window of this text field (i.e., the window this text
	 * field is in).  This needs to be set prior to the window being shown
	 * so that the file list can properly hide itself when necessary.<p>
	 *
	 * If you are adding this text field to a panel, and you're not yet sure
	 * who is the parent window, you can wrap this call in a
	 * <code>Runnable</code> and run it via
	 * <code>SwingUtilities.invokeLater()</code>.
	 *
	 * @return Whether a parent window was found.  If you use this component
	 *         correctly, this should always return <code>true</code>.
	 */
	public boolean discoverParentWindow() {

		// Get rid of old popup window if it exists.  We must create a new
		// one, as if the user has called discoverParentWindow(), they
		// have probably re-parented this text field, so we must in turn
		// "re-parent" our popup.
		if (popupWindow!=null) {
			//popupWindow.remove(contentPane);
			popupWindow.setContentPane(new JPanel());
			popupWindow.dispose();
			popupWindow = null;
		}

		if (this.parent!=null) {
			parent.removeComponentListener(this);
		}
		Component parent = this.getParent();
		while (parent!=null) {
			if (parent instanceof Window) {
				this.parent = (Window)parent;
				this.parent.addComponentListener(this);
				return true;
			}
			parent = parent.getParent();
		}

		return false;

	}


	/**
	 * Returns whether this text field auto-completes the next matching
	 * filename when the drop-down list is visible.  Note that this property
	 * is only honored when this text field is "file system aware".
	 *
	 * @return Whether the next matching filename is auto-inserted.
	 * @see #setAutoCompleteFileName(boolean)
	 */
	public boolean getAutoCompleteFileName() {
		return autoCompleteFileName;
	}


	/**
	 * Returns the current directory for this text field.
	 *
	 * @return The current directory.
	 * @see #setCurrentDirectory
	 */
	public String getCurrentDirectory() {
		return currentDirectory;
	}


	/**
	 * Returns whether or not this text field is file-system-aware.  If it is,
	 * then the drop-down list expands to display all matching files and
	 * directories as the user types.
	 *
	 * @return Whether or not this text field is file-system aware.
	 * @see #setFileSystemAware
	 */
	public boolean getFileSystemAware() {
		return fileSystemAware;
	}


	/**
	 * Returns whether or not the current OS is case-insensitive (e.g.,
	 * is Windows or OS X).
	 *
	 * @return Whether or not the underlying OS is case-insensitive.
	 */
	private static final boolean getIgnoreCase() {
		String os = System.getProperty("os.name");
		boolean ignoreCase = false;
		if (os!=null) {
			os = os.toLowerCase();
			ignoreCase =	os.indexOf("windows")>-1   ||
						os.indexOf("mac os x")>-1;
		}
		return ignoreCase;
	}


	/**
	 * Called when text is inserted into this text component.
	 */
	public void insertUpdate(DocumentEvent e) {
		if (isShowing() && fileSystemAware && !ignoreDocumentUpdates) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					String entered = updateComboBoxContents();
					if (getAutoCompleteFileName()) {
						autoCompleteFileName(entered);
					}
				}
			});
		}
	}


	/**
	 * Installs key actions for scrolling through the file list.  We make the
	 * up and down arrow keys scroll through the list, and the Enter key hide
	 * the popup window, if it exists, then call the root pane's action if it
	 * exists.
	 */
	private void installExtraKeyActions() {

		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "OnDown");
		actionMap.put("OnDown", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (popupWindow!=null && popupWindow.isVisible()) {
					int index = fileList.getSelectedIndex();
					int size = fileList.getModel().getSize();
					index = (index<size-1) ? (index+1) : 0;
					fileList.setSelectedIndex(index);
					fileList.ensureIndexIsVisible(index);
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "OnUp");
		actionMap.put("OnUp", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (popupWindow!=null && popupWindow.isVisible()) {
					int index = fileList.getSelectedIndex();
					index = (index>0) ? (index-1) :
							(fileList.getModel().getSize()-1);
					fileList.setSelectedIndex(index);
					fileList.ensureIndexIsVisible(index);
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
												"OnPageDown");
		actionMap.put("OnPageDown", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (popupWindow!=null && popupWindow.isVisible()) {
					int index = fileList.getSelectedIndex();
					int size = fileList.getModel().getSize();
					if (index<size-1) {
						int visRows = fileList.getVisibleRowCount();
						index = Math.min(index+visRows, size-1);
						fileList.setSelectedIndex(index);
						fileList.ensureIndexIsVisible(index);
					}
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
												"OnPageUp");
		actionMap.put("OnPageUp", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (popupWindow!=null && popupWindow.isVisible()) {
					int index = fileList.getSelectedIndex();
					if (index>0) {
						int visRows = fileList.getVisibleRowCount();
						index = Math.max(index-visRows, 0);
						fileList.setSelectedIndex(index);
						fileList.ensureIndexIsVisible(index);
					}
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "OnEnter");
		actionMap.put("OnEnter", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Hide the popup window if necessary and call the
				// default binding.
				if (popupWindow!=null && popupWindow.isVisible())
					setPopupVisible(false);
				JRootPane root = SwingUtilities.getRootPane(
											FSATextField.this);
				if (root != null) {
					InputMap im = root.getInputMap(
								JComponent.WHEN_IN_FOCUSED_WINDOW);
					ActionMap am = root.getActionMap();
					if (im != null && am != null) {
						Object obj = im.get(KeyStroke.getKeyStroke(
										KeyEvent.VK_ENTER,0));
						if (obj != null) {
							Action action = am.get(obj);
							if (action != null) {
								action.actionPerformed(
									new ActionEvent(
										root, e.getID(),
										e.getActionCommand(),
										e.getWhen(),
										e.getModifiers()));
							}
						}
					}
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "OnEsc");
		actionMap.put("OnEsc", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Hide the popup window if necessary;
				// otherwise, call the default binding.
				if (popupWindow!=null && popupWindow.isVisible()) {
					setPopupVisible(false);
					return;
				}
				JRootPane root = SwingUtilities.getRootPane(
											FSATextField.this);
				if (root != null) {
					InputMap im = root.getInputMap(
								JComponent.WHEN_IN_FOCUSED_WINDOW);
					ActionMap am = root.getActionMap();
					if (im != null && am != null) {
						Object obj = im.get(KeyStroke.getKeyStroke(
										KeyEvent.VK_ESCAPE,0));
						if (obj != null) {
							Action action = am.get(obj);
							if (action != null) {
								action.actionPerformed(
									new ActionEvent(
										root, e.getID(),
										e.getActionCommand(),
										e.getWhen(),
										e.getModifiers()));
							}
						}
					}
				}
			}
		});

	}


	/**
	 * Installs the standard key actions unique to <code>FSATextField</code>s.
	 */
	private void installStandardKeyActions() {

		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getActionMap();

		// Tab and Shift+Tab are tradionally focus-shifting keys.  Swing will
		// not allow us to override the action associated with Tab since it
		// is a focus event, and focus events occur before apps are notified
		// of the keypress or focus event.  So to "override" the Tab key's
		// action, we must disable focus events and manually handle them
		// ourselves.
		setFocusTraversalKeysEnabled(false);

		// Tab simply clears selection if there is a selection and the dot
		// is at the end of the text.  This is to facilitate users taking
		// advantage of the auto-completing of the next matching filename.
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "OnTab");
		actionMap.put("OnTab", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Skip to end of selection if necessary, otherwise,
				// do the default and focus the next component.
				Caret c = getCaret();
				if (c!=null) {
					int dot = c.getDot();
					if (dot>c.getMark() && dot==getDocument().getLength()){
						c.setDot(dot);
						if (popupWindow!=null && popupWindow.isVisible()){
							popupWindow.setVisible(false);
						}
						return;
					}
				}
				// Programmatically focus next component as focus keys have
				// been disabled.
				KeyboardFocusManager.getCurrentKeyboardFocusManager().
										focusNextComponent(); 
			}
		});

		// Make Shift+Tab focus the previous component (as it normally would).
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
								InputEvent.SHIFT_MASK), "OnShiftTab");
		actionMap.put("OnShiftTab", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Programmatically focus previous component as focus keys
				// have been disabled.
				KeyboardFocusManager.getCurrentKeyboardFocusManager().
										focusPreviousComponent(); 
			}
		});

	}


	/**
	 * Returns whether or not this text field previews both files and
	 * directories or just directories.
	 *
	 * @return Whether or not this text field shows only directories.
	 * @see #setDirectoriesOnly
	 */
	public boolean isDirectoriesOnly() {
		return directoriesOnly;
	}


	/**
	 * Called when the parent dialog/frame fires a component event.  This
	 * method is here so we can hide the drop-down file list when the parent
	 * frame is moved or resized.
	 *
	 * @param e The component event fired by the parent dialog/frame.
	 */
	private void processParentComponentEvent(ComponentEvent e) {
		switch (e.getID()) {
			case ComponentEvent.COMPONENT_HIDDEN:
			case ComponentEvent.COMPONENT_MOVED:
			case ComponentEvent.COMPONENT_RESIZED:
				updatePopupWindowDimensions();
				if (popupWindow!=null) {
					setPopupVisible(false);
				}
				break;
			case ComponentEvent.COMPONENT_SHOWN:
		}
	}


	/**
	 * Process the focus events of this text field.  This is overridden so
	 * we can hide the drop-down file list if the text field loses focus.
	 *
	 * @param e The focus event fired by this text field.
	 */
	public void processFocusEvent(FocusEvent e) {
		if (e.getID()==FocusEvent.FOCUS_LOST) {
//			if (!e.isTemporary() && e.getOppositeComponent()!=popupWindow) {
				setPopupVisible(false);
//			}
		}
		super.processFocusEvent(e);
	}


	/**
	 * Removes all items from the file list.
	 *
	 * @see #addItem
	 */
	public void removeAllItems() {
		fileListModel.removeAllElements();
	}


	/**
	 * Called when text is removed from the text component's document.
	 */
	public void removeUpdate(DocumentEvent e) {
		if (isShowing() && fileSystemAware && !ignoreDocumentUpdates) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					updateComboBoxContents();
				}
			});
		}
	}


	/**
	 * Sets whether this text field auto-completes the next matching
	 * filename when the drop-down list is visible.  Note that this property
	 * is only honored when this text field is "file system aware".
	 *
	 * @param auto Whether the next matching filename is auto-inserted.
	 * @see #getAutoCompleteFileName()
	 */
	public void setAutoCompleteFileName(boolean auto) {
		this.autoCompleteFileName = auto;
	}


	/**
	 * Sets the current directory for this text field.  The current directory
	 * is the directory in which the text field assumes typed relative
	 * files reside in.
	 *
	 * @param currentDirectory The new "current directory" for this combo
	 *        box.  This value should be an absolute pathname.
	 * @see #getCurrentDirectory
	 */
	public void setCurrentDirectory(File currentDirectory) {
		this.currentDirectory = currentDirectory.getAbsolutePath();
	}


	/**
	 * Sets the current directory for this text field.  The current directory
	 * is the directory in which the text field assumes typed relative
	 * files reside in.
	 *
	 * @param currentDirectory The new "current directory" for this combo
	 *        box.  This value should be an absolute pathname.
	 * @see #getCurrentDirectory
	 */
	public void setCurrentDirectory(String currentDirectory) {
		this.currentDirectory = currentDirectory;
	}


	/**
	 * Sets whether or not this text field previews both files and
	 * directories or just directories.
	 *
	 * @param directoriesOnly Whether or not to preview only directories.
	 * @see #isDirectoriesOnly
	 */
	public void setDirectoriesOnly(boolean directoriesOnly) {
		// Lazily create the file filter used.
		if (directoriesOnly) {
			directoriesOnlyFilenameFilter = new FilenameFilter() {
				public boolean accept(File parentDir, String fileName) {
					return new File(parentDir.getAbsolutePath(),
									fileName).isDirectory();
				}
			};
		}
		this.directoriesOnly = directoriesOnly;
	}


	/**
	 * Sets the document for this text field.  This is overridden so we can
	 * add a document listener to it.
	 */
	public void setDocument(Document document) {
		if (document!=null) {
			Document oldDocument = getDocument();
			if (oldDocument!=null)
				oldDocument.removeDocumentListener(this);
			super.setDocument(document);
			document.addDocumentListener(this);
		}
	}


	/**
	 * Toggles whether or not this dialog is file-system-aware.  This
	 * property should be set to <code>false</code> when programmatically
	 * inserting text into the text field; otherwise, it has a bad habit of
	 * stealing the focus from the currently focused component, etc.
	 *
	 * @param aware Whether or not this text field should be file-system
	 *        aware.
	 * @see #getFileSystemAware
	 */
	public void setFileSystemAware(boolean aware) {
		fileSystemAware = aware;
	}


	/**
	 * Sets the cell renderer for the dropdown file list.
	 *
	 * @param renderer The cell renderer to use.
	 */
	public void setListCellRenderer(ListCellRenderer renderer) {
		fileList.setCellRenderer(renderer);
	}


	/**
	 * Toggles the display of the drop-down file list.
	 *
	 * @param visible Whether or not the file list is to be visible.
	 */
	private void setPopupVisible(boolean visible) {
		if (visible) {
			if (popupWindow==null) {
				popupWindow = createPopupWindow();
			}
			popupWindow.pack();
			Rectangle bounds = getBounds();
			Point loc = getLocation();
			loc.x = 0;		// Why must we do this?
			SwingUtilities.convertPointToScreen(loc, this);
			popupWindow.setLocation(loc.x, loc.y+bounds.height-bounds.y);
			popupWindow.setVisible(true);
		}
		else {
			if (popupWindow!=null) {
				popupWindow.setVisible(false);
			}
		}
	}


	/**
	 * Overridden so that we always have a document listener on the text field.
	 */
	public void setUI(javax.swing.plaf.TextUI ui) {

		// Add the document listener.
		Document document = getDocument();
		if (document!=null)
			document.removeDocumentListener(this);
		super.setUI(ui);
		getDocument().addDocumentListener(this);

		// Update the scroll pane in the drop-down window.
		if (popupWindow!=null)
			SwingUtilities.updateComponentTreeUI(popupWindow);

		// Install extra key actions.
		installExtraKeyActions();

	}


	/**
	 * Updates the text field's dropdown list to contain files matching
	 * the characters typed by the user into the text field's text field.
	 *
	 * @return The text currently entered into the text field if the popup
	 *         is visible, or <code>null</code> if it is not visible.
	 */
	private String updateComboBoxContents() {

		// Get the path information typed in.
		String text = getText();

		if (text.length()==0) {
			setPopupVisible(false);
			lastCount = 0;
			return null;
		}

		// We're nice and allow the user to type either '/' or '\\' as the
		// separator on any OS.
		int lastSeparator = Math.max(text.lastIndexOf('/'),
								text.lastIndexOf('\\')) + 1;

		// Get the path for the file they're typing.  If they haven't typed a
		// separator char yet, assume it's a relative path from the current
		// directory (and they're typing the name of a file in that
		// directory). If they have typed a separator char, check to see if
		// it's a relative directory path or an absolute one.
		File t2 = null;
		if (lastSeparator!=0) {
			String pathPart = text.substring(0, lastSeparator);
			t2 = new File(pathPart);
			if (!t2.isAbsolute()) {
				t2 = new File(currentDirectory, pathPart);
			}
		}
		else {
			t2 = new File(currentDirectory);
		}
	
		// An attempt to speed things up in the common case.  If the
		// directory they're working in hasn't changed, we don't
		// have to get the list of files in the directory (as it is cached).
		if (!t2.equals(directory)) {

			directory = t2;

			if (!directory.isDirectory()) {
				lastCount = -1;
				setPopupVisible(false);
				num = 0;
				containedFiles = null;
				return null;
			}

			dirName = directory.getAbsolutePath();
			if (dirName.charAt(dirName.length()-1)!=File.separatorChar)
				dirName += File.separatorChar; // Only need to add slash if not == "C:\" on Windows.

			// If they only want to see directories, we have to take a little
			// more care.
			if (directoriesOnly) {
				containedFiles = directory.list(directoriesOnlyFilenameFilter);
			}
			else {
				containedFiles = directory.list();
			}

			// We must check for null here in case an IO error occurs.
			if (containedFiles!=null) {
				num = containedFiles.length;
				if (num>0) {
					Arrays.sort(containedFiles, IGNORE_CASE ?
							String.CASE_INSENSITIVE_ORDER : null);
				}
			}
			else {
				num = 0;
			}

		} // End of if (!t2.equals(directory))

		if (num > 0) {
			removeAllItems();
			// We only match on the file name since the canonical-file path
			// is cached in dirName.
			String dirPart = text.substring(0, lastSeparator);
			String fpFileName = text.substring(lastSeparator);
			int fpFileNameLength = fpFileName.length();
			long count = 0;
			int i = Arrays.binarySearch(containedFiles, fpFileName,
					IGNORE_CASE ? String.CASE_INSENSITIVE_ORDER : null);
			if (i<0) { // index = -insertion_point - 1
				i = -(i+1);
			}
			while (i<num && containedFiles[i].regionMatches(IGNORE_CASE,
								0, fpFileName, 0,fpFileNameLength)) {
				addItem(dirPart + containedFiles[i]);
				count++;
				i++;
			}
			if (count!=lastCount && count>0) {
				// This tricks the popup menu's list to "resize" properly.
				setPopupVisible(true);
			}
			else if (count==0) {
				setPopupVisible(false);
			}
			lastCount = count;
			return count>0 ? text : null;
		}

		else {
			lastCount = -1;
			lastSeparator = 0;
			setPopupVisible(false);
			return null;
		}

	}


	/**
	 * Updates the size of the drop-down file list to match the width of the
	 * text field itself.
	 */
	private void updatePopupWindowDimensions() {
		Dimension size = getSize();
		contentPane.setMinimumSize(new Dimension(size.width,
								contentPane.getMinimumSize().height));
		contentPane.setPreferredSize(new Dimension(size.width,
								contentPane.getPreferredSize().height));
		contentPane.setMaximumSize(new Dimension(size.width,
								contentPane.getMaximumSize().height));
	}


	/**
	 * Called when the user manually selects a popup menu list item with
	 * the mouse.  This synchronizes the text field's text value with
	 * the selected list item.
	 *
	 * @param e The event.
	 */
	public void valueChanged(ListSelectionEvent e) {
		// NOTE: Use SwingUtilities#invokeLater() here to avoid a Swing
		// "bug" - Calling JList#getSelectedValue() from a list selection
		// listener can cause spurious IndexOutOfBoundsExceptions due to the
		// order in which the listener & the JList's model were added.
		// See Java bug: http://bugs.sun.com/view_bug.do?bug_id=4816818
		// for more information.
		SwingUtilities.invokeLater(listValueChangedRunnable);
	}


	/**
	 * Runnable queued on the EDT to run whenever the dropdown list's
	 * selection changes.  It is safe to reuse this runnable object because
	 * it's only ever executed on the EDT, so it'll only ever be run one
	 * at a time.
	 */
	private class ListValueChangedRunnable implements Runnable {

		public void run() {
			ignoreDocumentUpdates = true;
			String selectedValue = (String)fileList.getSelectedValue();
			if (selectedValue!=null) {
				setText(selectedValue);
			}
			ignoreDocumentUpdates = false;
		}

	}


}