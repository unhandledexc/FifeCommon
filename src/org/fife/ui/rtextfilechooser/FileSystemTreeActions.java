package org.fife.ui.rtextfilechooser;

import java.awt.Component;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

import org.fife.ui.rtextfilechooser.FileSystemTree.FileSystemTreeNode;
import org.fife.ui.rtextfilechooser.extras.FileIOExtras;


/**
 * Actions used by <code>FileSystemTree</code>s.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class FileSystemTreeActions {


	/**
	 * Base class for file system tree-related actions.
	 */
	abstract static class AbstractTreeAction extends AbstractAction {

		protected FileSystemTree tree;
		private static ResourceBundle msg = ResourceBundle.getBundle(
							"org.fife.ui.rtextfilechooser.FileSystemTree");

		public AbstractTreeAction(FileSystemTree tree) {
			this.tree = tree;
		}

		protected String getString(String key) {
			return msg.getString(key);
		}

	}


	/**
	 * Action that handles deleting files.
	 */
	static class DeleteAction extends AbstractTreeAction {

		private Window window;

		public DeleteAction(Window parent, FileSystemTree tree) {
			super(tree);
			putValue(NAME, getString("Delete"));
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
			this.window = parent;
		}

		public void actionPerformed(ActionEvent e) {

			// Get the selected files.  If there are no selected files (i.e.,
			// they pressed "delete" when no files were selected), beep.
			File[] files = tree.getSelectedFiles();
			if (files==null || files.length==0) {
				UIManager.getLookAndFeel().provideErrorFeedback(window);
				return;
			}

			FileIOExtras extras = FileIOExtras.getInstance();
			if (extras!=null) {
				handleDeleteNative(files, extras);
			}
			else {
				handleDeleteViaJava(files);
			}

		}

		private void handleDeleteNative(File[] files, FileIOExtras extras) {
			if (extras.moveToRecycleBin(files, true, true)) {
				refresh();
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(window);
			}
		}

		private void handleDeleteViaJava(File[] files) {

			// Prompt to confirm the file deletion.
			int count = files.length;
			int choice;
			if (count==1) {
				String fileName = files[0].getName();
				choice = JOptionPane.showConfirmDialog(window,
					RTextFileChooser.msg.
							getString("DeleteConfirmPrompt") + fileName + "?");
			}
			else { // count>1
				choice = JOptionPane.showConfirmDialog(window,
					RTextFileChooser.msg.
							getString("DeleteMultipleConfirmPrompt"));
			}

			// If they chose "yes," delete the files.
			if (choice==JOptionPane.YES_OPTION) {
				for (int i=0; i<count; i++) {
					if (!files[i].delete()) {
						Object[] arguments = { files[i].getName() };
						String msg = MessageFormat.format(
							RTextFileChooser.msg.getString("DeleteFailText"),
							arguments);
						JOptionPane.showMessageDialog(window,
							msg,
							RTextFileChooser.msg.getString("Error"),
							JOptionPane.ERROR_MESSAGE);
					}
				}
				refresh();
			}

		}

		private void refresh() {
			FileSystemTreeNode node = (FileSystemTreeNode)tree.
								getLastSelectedPathComponent();
			if (node!=null) {
				FileSystemTreeNode parent= (FileSystemTreeNode)node.getParent();
				tree.refreshChildren(parent);
				((DefaultTreeModel)tree.getModel()).reload(parent);
			}
		}

	}


	/**
	 * Cell editor used when the user adds a new file or folder to the tree.
	 */
	private static class FileTreeCellEditor implements TreeCellEditor {

		private FileSystemTree tree;
		private File root;
		private DefaultTreeCellEditor delegate;

		public FileTreeCellEditor(final FileSystemTree fst,
									DefaultTreeCellRenderer renderer,
									File root, final boolean dir) {

			this.tree = fst;
			this.root = root;

			delegate = new DefaultTreeCellEditor(tree, renderer) {

				protected TreeCellEditor createTreeCellEditor() {
					TreeCellEditor editor = super.createTreeCellEditor();
					if (editor instanceof DefaultCellEditor) { // Always true
						DefaultCellEditor dce = (DefaultCellEditor)editor;
						dce.getComponent().addFocusListener(new FocusAdapter() {
							public void focusLost(FocusEvent e) {
								if (!e.isTemporary()) {
									cancelCellEditing();
								}
							}
						});
					}
					return editor;
				}

				protected void determineOffset(JTree tree, Object value,
						boolean selected, boolean expanded,
						boolean leaf, int row) {
					editingIcon = dir ? fst.iconManager.getFolderIcon() :
										fst.iconManager.fileIcon;
					if(editingIcon != null) {
						offset = renderer.getIconTextGap() +
								editingIcon.getIconWidth();
					}
					else {
						offset = renderer.getIconTextGap();
					}
				}

			};

		}

		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row) {
			FileSystemTreeNode fstn = (FileSystemTreeNode)value;
			value = fstn.getFile().getName();
			return delegate.getTreeCellEditorComponent(tree, value, selected,
													expanded, leaf, row);
		}

		public void addCellEditorListener(CellEditorListener l) {
			delegate.addCellEditorListener(l);
		}

		public void cancelCellEditing() {
			delegate.cancelCellEditing();
		}

		private boolean fileAlreadyExists() {
			File temp = new File(root, delegate.getCellEditorValue().toString());
			if (temp.exists()) {
				UIManager.getLookAndFeel().provideErrorFeedback(tree);
				return true;
			}
			return false;
		}

		public Object getCellEditorValue() {
			return new File(root, delegate.getCellEditorValue().toString());
		}

		public boolean isCellEditable(EventObject e) {
			return delegate.isCellEditable(e);
		}

		public void removeCellEditorListener(CellEditorListener l) {
			delegate.removeCellEditorListener(l);
		}

		public boolean shouldSelectCell(EventObject e) {
			return delegate.shouldSelectCell(e);
		}

		public boolean stopCellEditing() {
			if (fileAlreadyExists()) {
				return false;
			}
			return delegate.stopCellEditing();
		}

	}


	/**
	 * Allows the user to create a new file.
	 */
	static class NewFileAction extends AbstractTreeAction
								implements CellEditorListener {

		private TreeCellEditor editor;

		public NewFileAction(FileSystemTree tree) {
			super(tree);
			putValue(NAME, getString("NewFile"));
			putValue(MNEMONIC_KEY,
				new Integer(getString("NewFileMnemonic").charAt(0)));
		}

		public void actionPerformed(ActionEvent e) {

			TreePath path = tree.getSelectionPath();

			if (path!=null) { // Should always be true.

				FileSystemTreeNode parentNode = (FileSystemTreeNode)path.
							getLastPathComponent();
				File root = parentNode.getFile();

				if (root.isDirectory()) { // Should always be true

					String name = File.separatorChar=='/' ? "newFile" :
													"NewFile.txt";
					File f = new File(root, name);
					FileSystemTreeNode newChild = new FileSystemTreeNode(f);

					// We must do this before inserting a child node, due to
					// our use of "dummy" child nodes for tree visual appeal.
//System.out.println("pre-expand path == " + path);
//System.out.println("pre-expand node == " + parentNode);
//System.out.println("pre-expand child count == " + parentNode.getChildCount());
//if (parentNode.getChildCount()>0) {
//	System.out.println("pre-expand child 1 == " + parentNode.getChildAt(0));
//}
					tree.refreshChildren(parentNode);
//System.out.println("post-expand child count == " + parentNode.getChildCount());
//if (parentNode.getChildCount()>0) {
//	System.out.println("post-expand child 1 == " + parentNode.getChildAt(0));
//}
					DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
					model.insertNodeInto(newChild, parentNode, 0);
//System.out.println("post-insert child count == " + parentNode.getChildCount());
//if (parentNode.getChildCount()>0) {
//	System.out.println("post insert child 1 == " + parentNode.getChildAt(0));
//}

					// JTree only works with TreePaths returned by it (!);
					// creating TreePaths by its constructor or by other means
					// results in NPE's when adding a child node to a newly-
					// added child node.
					// Don't know why, but the lines below are magic!  Can't
					// be simplified...
					tree.expandPath(path); // Expand to show new child
					int parentRow = tree.getRowForPath(path);
//					System.out.println(tree.isExpanded(path));
					int newRow = parentRow + 1;
					TreePath newChildPath = tree.getPathForRow(newRow);
//System.out.println("newChildPath == " + newChildPath);
//System.out.println("--- " + tree.getPathForRow(newRow-1));
//System.out.println("--- " + tree.getPathForRow(newRow));
//System.out.println("--- " + tree.getPathForRow(newRow+1));
//System.out.println("--- " + parentNode.getChildCount() + " - " + parentNode.getChildAt(0));
					tree.setSelectionPath(newChildPath);
					tree.setEditable(true);
					editor = new FileTreeCellEditor(tree,
							(DefaultTreeCellRenderer)tree.getCellRenderer(),
							root, false);
					editor.addCellEditorListener(this);
					tree.setCellEditor(editor);
					tree.startEditingAtPath(newChildPath);

				}
				else {
					UIManager.getLookAndFeel().provideErrorFeedback(tree);
				}

			}

		}

		public void editingCanceled(ChangeEvent e) {

			// Removes the node that was being edited.
			TreePath path = tree.getSelectionPath();
			FileSystemTreeNode node = (FileSystemTreeNode)path.
												getLastPathComponent();
			FileSystemTreeNode parent = null;
			if (node!=null) { // Should always be true
				parent = (FileSystemTreeNode)node.getParent();
				tree.refreshChildren(parent);
				((DefaultTreeModel)tree.getModel()).reload(parent);
			}

			editor.removeCellEditorListener(this);
			editor = null;
			tree.setCellEditor(null);
			tree.setEditable(false);

			// Select the parent that was going to be added to.
			// Use parent's file, not node.getFile().getParentFile(),
			// as node.getFile() may be our DUMMY_FILE.
			if (parent!=null) {
				tree.setSelectedFile(parent.getFile());
			}

		}

		public void editingStopped(ChangeEvent e) {

			File file = (File)editor.getCellEditorValue();
			FileSystemTreeNode node = (FileSystemTreeNode)tree.
									getSelectionPath().getLastPathComponent();

			if (node!=null) { // Should always be true
				node.setUserObject(file);
				//System.out.println(file.getAbsolutePath());
				boolean res = false;
				try {
					res = file.createNewFile();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				if (!res) {
					UIManager.getLookAndFeel().provideErrorFeedback(tree);
				}
				tree.iconManager.removeIconFor(file);
				FileSystemTreeNode parent = (FileSystemTreeNode)node.getParent();
				tree.refreshChildren(parent);
				((DefaultTreeModel)tree.getModel()).reload(parent);
				// After sorting alphabetically, the selected item may no
				// longer be visible.
				tree.scrollPathToVisible(tree.getSelectionPath());
			}

			editor.removeCellEditorListener(this);
			editor = null;
			tree.setCellEditor(null);
			tree.setEditable(false);
			tree.requestFocusInWindow();

		}

	}


	/**
	 * Allows the user to create a new folder.
	 */
	static class NewFolderAction extends AbstractTreeAction
								implements CellEditorListener {

		private TreeCellEditor editor;

		public NewFolderAction(FileSystemTree tree) {
			super(tree);
			putValue(NAME, getString("NewFolder"));
			putValue(MNEMONIC_KEY,
					new Integer(getString("NewFolderMnemonic").charAt(0)));
		}

		public void actionPerformed(ActionEvent e) {

			TreePath path = tree.getSelectionPath();

			if (path != null) { // Should always be true.

				FileSystemTreeNode parentNode = (FileSystemTreeNode)path
						.getLastPathComponent();
				File root = parentNode.getFile();

				if (root.isDirectory()) { // Should always be true

					String name = File.separatorChar == '/' ? "newDir"
							: "New Folder";
					File f = new File(root, name);
					// Pass in "true" for directory property, as this directory
					// doesn't exist yet.
					FileSystemTreeNode newChild = tree.
												createTreeNodeForImpl(f, true);

					tree.refreshChildren(parentNode);
					DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
					model.insertNodeInto(newChild, parentNode, 0);

					// JTree only works with TreePaths returned by it (!);
					// creating TreePaths by its constructor or by other means
					// results in NPE's when adding a child node to a newly-
					// added child node.
					// Don't know why, but the lines below are magic!  Can't
					// be simplified...
					tree.expandPath(path); // Expand to show new child
					int parentRow = tree.getRowForPath(path);
					int newRow = parentRow + 1;
					TreePath newChildPath = tree.getPathForRow(newRow);
					tree.setSelectionPath(newChildPath);
					tree.setEditable(true);
					editor = new FileTreeCellEditor(tree,
							(DefaultTreeCellRenderer)tree.getCellRenderer(),
							root, false);
					editor.addCellEditorListener(this);
					tree.setCellEditor(editor);
					tree.startEditingAtPath(newChildPath);

				} else {
					UIManager.getLookAndFeel().provideErrorFeedback(tree);
				}

			}

		}

		public void editingCanceled(ChangeEvent e) {

			// Removes the node that was being edited.
			TreePath path = tree.getSelectionPath();
			FileSystemTreeNode node = (FileSystemTreeNode)path.
												getLastPathComponent();
			FileSystemTreeNode parent = null;
			if (node != null) { // Should always be true
				parent = (FileSystemTreeNode) node.getParent();
				tree.refreshChildren(parent);
				((DefaultTreeModel)tree.getModel()).reload(parent);
			}

			editor.removeCellEditorListener(this);
			editor = null;
			tree.setCellEditor(null);
			tree.setEditable(false);

			// Select the parent that was going to be added to.
			// Use parent's file, not node.getFile().getParentFile(),
			// as node.getFile() may be our DUMMY_FILE.
			if (parent!=null) {
				tree.setSelectedFile(parent.getFile());
			}

		}

		public void editingStopped(ChangeEvent e) {

			File file = (File) editor.getCellEditorValue();
			FileSystemTreeNode node = (FileSystemTreeNode)tree.
									getSelectionPath().getLastPathComponent();
			if (node != null) { // Should always be true
				node.setUserObject(file);
				// System.out.println(file.getAbsolutePath());
				if (!file.mkdir()) {
					UIManager.getLookAndFeel().provideErrorFeedback(tree);
				}
				tree.iconManager.removeIconFor(file);
				FileSystemTreeNode parent = (FileSystemTreeNode) node
						.getParent();
				tree.refreshChildren(parent);
				((DefaultTreeModel)tree.getModel()).reload(parent);
				// After sorting alphabetically, the selected item may no
				// longer be visible.
				tree.scrollPathToVisible(tree.getSelectionPath());
			}

			editor.removeCellEditorListener(this);
			editor = null;
			tree.setCellEditor(null);
			tree.setEditable(false);
			tree.requestFocusInWindow();

		}

	}


	/**
	 * Pastes files into the currently selected directory.<p>
	 *
	 * This action is currently pretty weak, and is only capable of pasting
	 * single files.  In the future, it would be easy to add the ability to
	 * paste multiple files and/or directories, but we need a way for the
	 * FileSystemTree to specify a handler for name collisions (e.g. files
	 * already existing).<p>
	 * 
	 * At the moment, if the selection is more than one file, or a directory,
	 * or if the destination file already exists, the system beeps and the
	 * copy does not occur.
	 */
	static class PasteAction extends AbstractTreeAction {

		public PasteAction(FileSystemTree tree) {
			super(tree);
			putValue(NAME, getString("Paste"));
			putValue(MNEMONIC_KEY,
					new Integer(getString("PasteMnemonic").charAt(0)));
			int mod = tree.getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_V, mod));
		}

		public void actionPerformed(ActionEvent e) {

			TreePath path = tree.getSelectionPath();
			boolean copied = false;

			if (path!=null) { // Should always be true.

				FileSystemTreeNode node = (FileSystemTreeNode)path.
							getLastPathComponent();
				File destDir = node.getFile();

				if (destDir.isDirectory()) { // Should always be true

					Clipboard clip = tree.getToolkit().getSystemClipboard();
					Transferable contents = clip.getContents(null);
					DataFlavor accepted = DataFlavor.javaFileListFlavor;

					if (contents.isDataFlavorSupported(accepted)) {

						try {

							List files = (List)contents.
												getTransferData(accepted);
							if (files!=null && files.size()==1) {

								File toCopy = (File)files.get(0);
								if (toCopy.isFile()) {
									File dest = new File(destDir,
														toCopy.getName());
									if (!dest.exists()) {
										Utilities.copyFile(toCopy, dest);
										tree.refreshChildren(node);
										((DefaultTreeModel)tree.getModel()).
															reload(node);
										tree.setSelectedFile(dest);
										copied = true;
									}
								}

							}

						} catch (UnsupportedFlavorException ufe) {
							ufe.printStackTrace(); // Never happens
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}

					}

				}

			}

			if (!copied) {
				UIManager.getLookAndFeel().provideErrorFeedback(tree);
			}

		}

		/**
		 * Returns whether the system clipboard contents are "valid" for this
		 * action to be enabled.  Currently this verifies that the contents
		 * are just a single file.  Applications can enable this action based
		 * on the return value of this method.
		 *
		 * @return Whether the contents of the clipboard are "valid" for this
		 *         action to be used.
		 */
		public boolean isClipboardContentValid() {
			Clipboard clip = tree.getToolkit().getSystemClipboard();
			Transferable contents = clip.getContents(null);
			DataFlavor accepted = DataFlavor.javaFileListFlavor;
			if (contents.isDataFlavorSupported(accepted)) {
				try {
					List files = (List)contents.getTransferData(accepted);
					if (files!=null && files.size()==1) {
						return ((File)files.get(0)).isFile();
					}
				} catch (UnsupportedFlavorException ufe) {
					ufe.printStackTrace(); // Never happens
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			return false;
		}

	}


	/**
	 * Action that "refreshes" the currently selected directory in the
	 * directory tree.
	 */
	static class RefreshAction extends AbstractTreeAction {

		public RefreshAction(FileSystemTree tree) {
			super(tree);
			putValue(NAME, getString("Refresh"));
			putValue(MNEMONIC_KEY,
				new Integer(getString("RefreshMnemonic").charAt(0)));
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		}

		public void actionPerformed(ActionEvent e) {

			TreePath path = tree.getSelectionPath();

			if (path!=null) { // Should always be true.

				FileSystemTreeNode node = (FileSystemTreeNode)path.
							getLastPathComponent();
				File file = node.getFile();
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

				// The file should be a directory.  The only catch is
				// that maybe the directory was deleted since the last
				// time we cached.  NOTE:  We MUST check whether the file
				// exists BEFORE we check whether it's a directory because
				// isDirectory() can return true for a directory that has
				// been removed after the File object was created, despite
				// what the Javadoc says.

				// If the directory no longer exists, refresh the structure
				// from the parent directory down, in case other stuff has
				// changed too.
				if (!file.exists()) {
					int count = path.getPathCount();
					if (count>1) {
						node = (FileSystemTreeNode)path.
										getPathComponent(count-2);
						tree.refreshChildren(node);
						model.reload(node);
					}
					else { // It's the one and only file in the path...
						tree.removeSelectionPath(path);
						model.reload(node);
					}
				}

				// If the directory still exists...
				else if (file.isDirectory()) {
					tree.refreshChildren(node);
					model.reload(node); // Causes repaint properly.
				}

				// Otherwise, they've removed what was a directory when we
				// cached data, and created a regular file in its place...
				// Again, we'll refresh the parent directory's cached data.
				else {
					int count = path.getPathCount();
					if (count>1) {
						node = (FileSystemTreeNode)path.
										getPathComponent(count-2);
						tree.refreshChildren(node);
						model.reload(node);
					}
					else { // It's the one and only file in the path...
						tree.removeSelectionPath(path);
						model.reload(node);
					}
				}

			}

		}

	}


}