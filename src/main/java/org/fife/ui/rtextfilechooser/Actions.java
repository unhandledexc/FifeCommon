/*
 * 09/30/2009
 *
 * Actions.java - Actions used in a file chooser.
 * Copyright (C) 2009 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.ui.rtextfilechooser;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.fife.ui.rtextfilechooser.extras.FileIOExtras;


/**
 * Actions for the file chooser.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface Actions {


	/**
	 * Adds the currently-viewed directory to the file chooser "favorites."
	 */
	static class AddToFavoritesAction extends FileChooserAction {

		public AddToFavoritesAction(RTextFileChooser chooser) {
			super(chooser);
			putValue(Action.NAME, chooser.getString("AddToFavorites"));
		}

		public void actionPerformed(ActionEvent e) {
			File dir = chooser.getCurrentDirectory();
			chooser.addToFavorites(dir.getAbsolutePath());
			chooser.focusFileNameField(false);
		}

	}


	/**
	 * Copies any files selected in the file chooser's view.
	 */
	static class CopyAction extends FileChooserAction {

		private FileSelector chooser;

		public CopyAction(FileSelector chooser) {
			super(null);
			this.chooser = chooser;
			putValue(Action.NAME, getString("Copy"));
			int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_C, mod));
		}

		public void actionPerformed(ActionEvent e) {

			// Get the selected files.  If there are no selected files (i.e.,
			// they pressed "Ctrl+C" when no files were selected), beep.
			File[] files = null;
			if (chooser instanceof RTextFileChooser) {
				// Horrible hack!!!  File chooser shouldn't actually
				// implement FileSelector!  But it's view does...
				files = ((RTextFileChooser)chooser).getView().getSelectedFiles();
			}
			else { // FileSystemTree
				files = chooser.getSelectedFiles();
			}
			if (files==null || files.length==0) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}

			List<File> fileList = Arrays.asList(files);
			FileListTransferable flt = new FileListTransferable(fileList);
			Clipboard clipboard = Toolkit.getDefaultToolkit().
												getSystemClipboard();
			clipboard.setContents(flt, flt);

		}

	}


	/**
	 * Copies the full path of any selected files to the clipboard.
	 */
	static class CopyFullPathAction extends FileChooserAction {

		private FileSelector chooser;

		public CopyFullPathAction(FileSelector chooser) {
			super(null);
			this.chooser = chooser;
			putValue(Action.NAME, getString("CopyFullPath"));
			int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			mod |= InputEvent.SHIFT_MASK;
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_C, mod));
		}

		public void actionPerformed(ActionEvent e) {

			// Get the selected files.  If there are no selected files (i.e.,
			// they pressed "Ctrl+Shift+C" when no files were selected), beep.
			File[] files = null;
			if (chooser instanceof RTextFileChooser) {
				// Horrible hack!!!  File chooser shouldn't actually
				// implement FileSelector!  But it's view does...
				files = ((RTextFileChooser)chooser).getView().getSelectedFiles();
			}
			else { // FileSystemTree
				files = chooser.getSelectedFiles();
			}
			if (files==null || files.length==0) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}

			Clipboard clipboard = Toolkit.getDefaultToolkit().
					getSystemClipboard();
			StringBuilder sb = new StringBuilder(files[0].getAbsolutePath());
			for (int i = 1; i < files.length; i++) {
				sb.append('\n').append(files[i].getAbsolutePath());
			}

			StringSelection transferable = new StringSelection(sb.toString());
			clipboard.setContents(transferable, transferable);
			
		}

	}


	/**
	 * Action that handles deleting files.
	 */
	static class DeleteAction extends FileChooserAction {

		private boolean hard;

		/**
		 * Constructor.
		 *
		 * @param chooser The file chooser.
		 * @param hard Whether this is a "hard" delete (i.e. permanently delete,
		 *        rather than go through OS means and possibly put into a
		 *        Recycle Bin).
		 */
		public DeleteAction(RTextFileChooser chooser, boolean hard) {
			super(chooser);
			putValue(Action.NAME, getString("Delete"));
			this.hard = hard;
			int modifiers = hard ? InputEvent.SHIFT_MASK : 0;
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, modifiers));
		}

		public void actionPerformed(ActionEvent e) {

			// Get the selected files.  If there are no selected files (i.e.,
			// they pressed "delete" when no files were selected), beep.
			File[] files = chooser.getView().getSelectedFiles();
			if (files==null || files.length==0) {
				UIManager.getLookAndFeel().provideErrorFeedback(chooser);
				return;
			}

			FileIOExtras extras = FileIOExtras.getInstance();
			if (!hard && extras!=null) {
				handleDeleteNative(files, extras);
			}
			else {
				handleDeleteViaJava(files);
			}

		}

		/**
		 * Uses the native means for deleting a file.  This allows us to use
		 * Windows' Recycle Bin, for example.
		 *
		 * @param files The files to delete.
		 * @param extras The native class that actually does the deletion.
		 */
		private void handleDeleteNative(File[] files, FileIOExtras extras) {
			Window parent = SwingUtilities.getWindowAncestor(chooser);
			if (extras.moveToRecycleBin(parent, files, true, true)) {
				refresh();
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(chooser);
			}
		}

		/**
		 * Deletes files with pure Java.  Only does a hard delete.
		 *
		 * @param files The files to delete.
		 */
		private void handleDeleteViaJava(File[] files) {

			// Prompt to confirm the file deletion.
			int count = files.length;
			int choice;
			if (count==1) {
				String fileName = files[0].getName();
				choice = JOptionPane.showConfirmDialog(chooser,
					chooser.getString("DeleteConfirmPrompt") + fileName + "?");
			}
			else { // count>1
				choice = JOptionPane.showConfirmDialog(chooser,
					chooser.getString("DeleteMultipleConfirmPrompt"));
			}

			// If they chose "yes," delete the files.
			if (choice==JOptionPane.YES_OPTION) {
				for (int i=0; i<count; i++) {
					if (!files[i].delete()) {
						Object[] arguments = { files[i].getName() };
						String msg = MessageFormat.format(
									chooser.getString("DeleteFailText"),
									arguments);
						JOptionPane.showMessageDialog(chooser,
									msg, chooser.errorDialogTitle,
									JOptionPane.ERROR_MESSAGE);
					}
				}
				refresh();
			}

		}

		private void refresh() {
			chooser.refreshView();
			// file name field contained names of file(s) to delete, so
			// clear them out.
			chooser.focusFileNameField(true);
		}

	}


	/**
	 * Base class for all file chooser actions.
	 */
	static abstract class FileChooserAction extends AbstractAction {

		protected RTextFileChooser chooser;
		private static ResourceBundle msg = ResourceBundle.getBundle(
							"org.fife.ui.rtextfilechooser.FileChooserPopup");

		public FileChooserAction(RTextFileChooser chooser) {
			this.chooser = chooser;
		}

		protected String getString(String key) {
			return msg.getString(key);
		}

	}


	/**
	 * Pastes files into the currently selected directory.
	 */
	static class PasteAction extends FileChooserAction {

		public PasteAction(RTextFileChooser chooser) {
			super(chooser);
			putValue(NAME, getString("Paste"));
			int mod = chooser.getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_V, mod));
		}

		public void actionPerformed(ActionEvent e) {

			File destDir = chooser.getCurrentDirectory();
			boolean copying = false;

			if (destDir.isDirectory()) { // Should always be true

				Clipboard clip = chooser.getToolkit().getSystemClipboard();
				Transferable contents = clip.getContents(null);
				DataFlavor accepted = DataFlavor.javaFileListFlavor;

				try {

					@SuppressWarnings("unchecked")
					List<File> files = (List<File>)contents.
											getTransferData(accepted);
					Window parent = SwingUtilities.getWindowAncestor(chooser);
					FilePasteCallback callback =
							new DefaultFilePasteCallback(parent) {
						@Override
						public void pasteOperationCompleted(int pasteCount){
							super.pasteOperationCompleted(pasteCount);
							chooser.refreshView();
						}
					};

					if (files!=null && files.size()>0) {
						FilePasteThread.paste(parent,
									files, destDir, callback);
						copying = true;
					}

				} catch (UnsupportedFlavorException ufe) {
					ufe.printStackTrace(); // Never happens
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

			}

			if (!copying) {
				UIManager.getLookAndFeel().provideErrorFeedback(chooser);
			}

		}

		/**
		 * Sets the enabled state of this action based on whether the system
		 * clipboard contains a list of files to copy).
		 */
		public void checkEnabledState() {
			setEnabled(Utilities.getClipboardContainsFileList());
		}

	}


	/**
	 * Refreshes the files displayed in the file chooser's view.
	 */
	static class RefreshAction extends FileChooserAction {

		public RefreshAction(RTextFileChooser chooser) {
			super(chooser);
			putValue(Action.NAME, getString("Refresh"));
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		}

		public void actionPerformed(ActionEvent e) {
			chooser.refreshView();
		}

	}


	/**
	 * Renames a file.
	 */
	static class RenameAction extends FileChooserAction {

		public RenameAction(RTextFileChooser chooser) {
			super(chooser);
			putValue(Action.NAME, getString("Rename"));
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		}

		public void actionPerformed(ActionEvent e) {

			File file = chooser.getView().getSelectedFile();
			if (file==null) {
				UIManager.getLookAndFeel().provideErrorFeedback(chooser);
				return;
			}

			String oldName = file.getName();
			String newName = JOptionPane.showInputDialog(chooser,
				chooser.getString("NewNamePrompt") + oldName + ":", oldName);
			if (newName!=null && !newName.equals(oldName)) {
				try {
					// If they have a separator char in the name, assume
					// they've typed a full path.  Otherwise, just rename
					// it and place it in the same directory.
					if (newName.indexOf(File.separatorChar)==-1) {
						newName = chooser.getCurrentDirectory().
							getCanonicalPath() + File.separatorChar + newName;
					}
					File newFile = new File(newName);
					if (!file.renameTo(newFile)) {
						throw new Exception(chooser.getString("RenameFailText"));
					}
					chooser.refreshView();
				} catch (Exception e2) {
					JOptionPane.showMessageDialog(chooser,
						chooser.getString("RenameErrorMessage") + e2,
						chooser.errorDialogTitle, JOptionPane.ERROR_MESSAGE);
				}
			}

		}

	}


	/**
	 * Opens a file with the default system editor or viewer.  Only works when
	 * using Java 6.
	 */
	/*
	 * NOTE: This method is a FileChooserAction only so we can use its
	 * ResourceBundle.  This is somewhat of a hack.
	 */
	public static class SystemOpenAction extends FileChooserAction {

		private FileSelector chooser;
		private OpenMethod method;

		public static enum OpenMethod {

			OPEN("open", "SystemOpenViewer"),
			EDIT("edit", "SystemOpenEditor");

			private OpenMethod(String method, String localizationKey) {
				this.method = method;
				this.localizationKey = localizationKey;
			}

			private OpenMethod opposite() {
				return this == OPEN ? EDIT : OPEN;
			}

			private String method;
			private String localizationKey;

		}

		public SystemOpenAction(FileSelector chooser, OpenMethod method) {
			super(null);
			this.chooser = chooser;
			this.method = method;
			putValue(Action.NAME, getString(method.localizationKey));
		}

		public void actionPerformed(ActionEvent e) {

			File file = chooser.getSelectedFile();
			if (file==null) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}

			Object desktop = getDesktop();
			if (desktop!=null) {
				// Since OSes can be finicky over whether something is
				// considered a "viewer" or an "editor," and usually folks
				// just want to open the application in *something*, if what
				// they ask for fails we'll try the other application type.
				if (!openImpl(desktop, method, file)) {
					if (!openImpl(desktop, method.opposite(), file)) {
						UIManager.getLookAndFeel().provideErrorFeedback(null);
					}
				}
			}

		}

		private static final Object getDesktop() {

			try {

				Class<?> desktopClazz = Class.forName("java.awt.Desktop");
				Method m = desktopClazz.
					getDeclaredMethod("isDesktopSupported");

				boolean supported = ((Boolean)m.invoke(null)).booleanValue();
				if (supported) {
					m = desktopClazz.getDeclaredMethod("getDesktop");
					return m.invoke(null);
				}

			} catch (RuntimeException re) {
				throw re; // Keep FindBugs happy
			} catch (Exception e) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
			}

			return null;

		}

		private static final boolean openImpl(Object desktop,
				OpenMethod method, File file) {
			try {
				Method m = desktop.getClass().getDeclaredMethod(
							method.method, new Class[] { File.class });
				m.invoke(desktop, new Object[] { file });
				return true;
			} catch (RuntimeException re) {
				throw re; // Keep FindBugs happy
			} catch (Exception e) {
				// Likely the UnsupportedOperationException or IOException
				// if the file association does not exist or app fails to load.
				// Swallow and return false below.
			}
			return false;
		}

	}


	/**
	 * Displays the "properties" dialog for any selected files.
	 */
	/*
	 * This is a File Chooser action only to get at its resource bundle, so
	 * this is kind of a hack.
	 */
	static class PropertiesAction extends FileChooserAction {

		private FileSelector selector;

		public PropertiesAction(FileSelector selector) {
			super(null);
			putValue(NAME, "Properties");
			final int alt = InputEvent.ALT_MASK;
			KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, alt);
			putValue(ACCELERATOR_KEY, ks);
			this.selector = selector;
		}

		public void actionPerformed(ActionEvent e) {

			FileIOExtras extras = FileIOExtras.getInstance();
			if (extras==null) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}

			File[] selected = null;
			if (selector instanceof RTextFileChooser) {
				RTextFileChooser chooser = (RTextFileChooser)selector;
				selected = chooser.getView().getSelectedFiles();
			}
			else {
				selected = selector.getSelectedFiles();
			}
			
			Window parent = SwingUtilities.
					getWindowAncestor((Component)selector);
			for (int i=0; i<selected.length; i++) {
				extras.showFilePropertiesDialog(parent, selected[i]);
			}

		}

	}


	/**
	 * Action that makes the file chooser display one directory "higher."
	 */
	static class UpOneLevelAction extends FileChooserAction {

		public UpOneLevelAction(RTextFileChooser chooser) {
			super(chooser);
			putValue(Action.NAME, getString("UpOneLevel"));
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
		}

		public void actionPerformed(ActionEvent e) {
			File parent = chooser.getCurrentDirectory().getParentFile();
			if (parent!=null) {
				chooser.setCurrentDirectory(parent);
			}
			else { // Should never happen, logic in RTextFileChooser prevents it
				UIManager.getLookAndFeel().provideErrorFeedback(chooser);
			}
		}

	}


}