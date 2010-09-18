/*
 * 02/11/2005
 *
 * FileSystemTree.java - A JTree containing all files in the local host's
 * file system.
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
package org.fife.ui.rtextfilechooser;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.fife.ui.ToolTipTree;


/**
 * A tree containing all files in the local host's file system.  So that you
 * can create components that contain a <code>FileSystemTree</code>, the
 * following property change events are fired when the tree is manipulated:
 *
 * <ul>
 *   <li><code>WILL_EXPAND_PROPERTY</code> - When the tree is about to
 *       expand one of its nodes.  Upon receiving this property change event,
 *       you could set the cursor to the system wait cursor, for example (as
 *       the expansion could take some time; Java seems to have some trouble
 *       calling <code>File.isDirectory()</code> on network files...).</li>
 *   <li><code>EXPANDED_PROPERTY</code> - When the tree has completed
 *       expanding the node.  Upon receiving this property change event, you
 *       could set the cursor back to the default.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 0.8
 */
public class FileSystemTree extends ToolTipTree implements FileSelector {

	public static final String EXPANDED_PROPERTY		= "FileSystemTree.treeExpanded";
	public static final String WILL_EXPAND_PROPERTY	= "FileSystemTree.treeWillExpand";

	private static final String DUMMY_FILE_NAME		= "dummy";
	private static final File   DUMMY_FILE			= new File(DUMMY_FILE_NAME);
	private static final String EMPTY				= "";

	private DefaultTreeModel treeModel;
	private FileSystemTreeNode root;
	private HashMap rootNameCache;				// Cache of root names.
	private FileSystemView fileSystemView;
	protected FileChooserIconManager iconManager;

	protected JPopupMenu popup;
	private JMenu openInMenu;
	private Actions.SystemOpenAction systemEditAction;
	private Actions.SystemOpenAction systemViewAction;
	private Actions.CopyAction copyAction;
	protected FileSystemTreeActions.NewFileAction newFileAction; // Used in DirectoryTree too
	private FileSystemTreeActions.NewFolderAction newFolderAction;
	private FileSystemTreeActions.RefreshAction refreshAction;

	private FileSystemTreeRenderer cellRenderer;

	/**
	 * Whether we're running in a Java 6 or higher JVM.
	 */
	private static final boolean IS_JAVA_6_PLUS;

	private static final String BUNDLE_NAME =
						"org.fife.ui.rtextfilechooser.FileSystemTree";


	/**
	 * Constructor.  This will create a tree with a root node for each root
	 * drive on the local file system.
	 */
	public FileSystemTree() {

		// Initialize variables.
		fileSystemView = FileSystemView.getFileSystemView();
		iconManager = new FileChooserIconManager();
		rootNameCache = new HashMap();

		// Add all of our "root" nodes.
		root = new FileSystemTreeNode();
		for (Iterator i=RootManager.getInstance().iterator(); i.hasNext(); ) {
			File aRoot = (File)i.next();
			// Hack - We "know" all roots are directories, so why query
			// via isDirectory()?  This is a nice performance boost.
			root.add(createTreeNodeForImpl(aRoot, true));
			addCachedRootName(aRoot);
		}

		init();

	}


	/**
	 * Constructor.
	 *
	 * @param rootDir The root directory for the tree.
	 * @throws IllegalArgumentException If this directory does not exist.
	 */
	public FileSystemTree(File rootDir) {

		if (rootDir==null || !rootDir.isDirectory()) {
			throw new IllegalArgumentException("Invalid root dir: " + rootDir);
		}

		// Initialize variables.
		fileSystemView = FileSystemView.getFileSystemView();
		iconManager = new FileChooserIconManager();
		rootNameCache = new HashMap();

		// Add all of our "root" nodes.
		root = new FileSystemTreeNode();
		root.add(createTreeNodeForImpl(rootDir, true));
		addCachedRootName(rootDir);

		init();

	}


	/**
	 * Adds a file name to the cached names (for roots).  We cache root
	 * directories' names because FileSystemView.getSystemDisplayName() can
	 * be costly, especially for disk drives on Windows (like A:\).
	 */
	private void addCachedRootName(File aRoot) {
		// Check separator character as a quick "hack" to check for Windows.
		// We don't call getName() for drives because for some reason it can
		// take a long time to get the name if it has one (such as A:\ and
		// C:\).
		if (File.separatorChar=='\\') {
			String absolutePath = aRoot.getAbsolutePath();
			if (absolutePath.length()==3 &&
					absolutePath.endsWith(":\\")) {
				rootNameCache.put(aRoot, absolutePath);
				return;
			}
		}
		rootNameCache.put(aRoot, getName(aRoot));
	}


	/**
	 * Collapses all tree nodes.
	 */
	protected void collapseAllNodes() {
		for (int i=getRowCount()-1; i>=0; i--) {
			collapseRow(i);
		}
	}


	/**
	 * Called right before the popup menu is displayed.  This method
	 * gives subclasses a chance to configure any extra actions or menu
	 * items they have added to the popup menu, so they are tailored to
	 * the selected item.  <code>popup</code> has been created before this
	 * method is called.  Subclasses should call the super implementation
	 * when overriding this method.
	 */
	protected void configurePopupMenuActions() {

		File selectedFile = getSelectedFile();

		openInMenu.setEnabled(IS_JAVA_6_PLUS && selectedFile!=null);
		if (IS_JAVA_6_PLUS) {
			systemEditAction.setEnabled(selectedFile!=null);
			systemViewAction.setEnabled(selectedFile!=null);
		}

		boolean enable = selectedFile!=null;
		copyAction.setEnabled(enable);

		// Only have the "Refresh" menu item enabled if a directory
		// item is selected.
		enable = selectedFile!=null && selectedFile.isDirectory();
		newFileAction.setEnabled(enable);
		newFolderAction.setEnabled(enable);
		refreshAction.setEnabled(enable);

	}


	/**
	 * Creates the popup menu for this file system tree.  Subclasses can
	 * override this method if they wish to add more menu items to the
	 * popup menu.
	 *
	 * @return The popup menu for this file system tree.
	 */
	protected JPopupMenu createPopupMenu() {

		JPopupMenu popup = new JPopupMenu();
		ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME);

		openInMenu = new JMenu(bundle.getString("PopupMenu.OpenIn"));
		if (IS_JAVA_6_PLUS) {
			systemEditAction = new Actions.SystemOpenAction(this, "edit");
			openInMenu.add(systemEditAction);
			systemViewAction = new Actions.SystemOpenAction(this, "open");
			openInMenu.add(systemViewAction);
		}
		popup.add(openInMenu);

		popup.addSeparator();

		copyAction = new Actions.CopyAction(this);
		popup.add(copyAction);

		popup.addSeparator();

		newFileAction = new FileSystemTreeActions.NewFileAction(this, bundle);
		popup.add(new JMenuItem(newFileAction));
		newFolderAction = new FileSystemTreeActions.NewFolderAction(this, bundle);
		popup.add(new JMenuItem(newFolderAction));

		popup.addSeparator();

		refreshAction = new FileSystemTreeActions.RefreshAction(this, bundle);
		JMenuItem item = new JMenuItem(refreshAction);
		popup.add(item);

		popup.applyComponentOrientation(getComponentOrientation());
		return popup;

	}


	/**
	 * Creates a tree node for the specified file.
	 *
	 * @param file The file for which to create a tree node.
	 * @return The tree node for the file.
	 */
	private FileSystemTreeNode createTreeNodeFor(File file) {
		return createTreeNodeForImpl(file, file.isDirectory());
	}


	/**
	 * Creates a tree node for the specified file.  This version of the method
	 * is for when you know ahead of time whether the file is a directory
	 * such as when the file is a "root").  This greatly speeds things up,
	 * as isDirectory() is notoriously slow.
	 *
	 * @param file The file for which to create a tree node.
	 * @param directory Whether the specified file is a directory.
	 * @return The tree node for the file.
	 */
	private FileSystemTreeNode createTreeNodeForImpl(File file,
											boolean directory) {

		// The node for the file.
		FileSystemTreeNode dmtn = new FileSystemTreeNode(file);

		// Make it have a "+/-" icon beside it if this node represents a
		// directory containing files.
		if (directory) {
			// NOTE:  We're just putting in a dummy file no matter what
			// for performance, as any kind of File querying methods in
			// Java degrade performance.
			//File[] files = fileSystemView.getFiles(file, false);
			//if (files!=null && files.length>0)
				dmtn.add(new FileSystemTreeNode(DUMMY_FILE));
		}

		return dmtn;

	}


	/**
	 * Displays the popup menu at the specified location.
	 *
	 * @param p The location at which to display the popup.
	 * @see #createPopupMenu
	 * @see #configurePopupMenuActions()
	 */
	private synchronized void displayPopupMenu(Point p) {

		// Create the popup menu if necessary.
		if (popup==null) {
			popup = createPopupMenu();
		}

		// Configure and display it!
		configurePopupMenuActions();
		popup.show(this, p.x, p.y);

	}


	/**
	 * Does any filtering and sorting of an array of files so that they will
	 * be displayed properly.  For example this method sorts the array so
	 * that directories are all listed before regular files.  Subclasses can
	 * override this method to do other things, such as only display
	 * directories.
	 *
	 * @param files The array of files to filter and sort.
	 * @return The filtered and sorted array of files.
	 */
	protected File[] filterAndSort(File[] files) {

		int num = files.length;
		ArrayList dirList = new ArrayList();
		ArrayList fileList = new ArrayList();

		// First, separate the directories from regular files so we can
		// sort them individually.  This part could be made more compact,
		// but it isn't just for a tad more speed.
		for (int i=0; i<num; i++) {
			if (files[i].isDirectory())
				dirList.add(files[i]);
			else
				fileList.add(files[i]);
		}

		Collections.sort(fileList);
		Collections.sort(dirList);
		dirList.addAll(fileList);

		File[] fileArray = new File[dirList.size()];
		return (File[])dirList.toArray(fileArray);

	}


	/**
	 * Called when a node has expanded.
	 */
	public void fireTreeExpanded(TreePath e) {

		super.fireTreeExpanded(e);

		// We fire a property change at the beginning and end of a node
		// expanding so that anyone interested only has to register a
		// PropertyChangeLister to know when nodes are expanding (so they can
		// display a wait cursor, for example).  Otherwise, they'd have to add
		// both a TreeExpansionListener and a TreeWillExpandListener.  Cheap,
		// I know, but oh well.
		firePropertyChange(EXPANDED_PROPERTY, null, null);

	}


	/**
	 * Called when a node is about to be expanded.  This method is overridden
	 * so that the node that is being expanded will be populated with its
	 * subdirectories, if necessary.
	 */
	public void fireTreeWillExpand(TreePath e) throws ExpandVetoException {

		// We fire a property change at the beginning and end of a node
		// expanding so that anyone interested only has to register a
		// PropertyChangeLister to know when nodes are expanding (so they can
		// display a wait cursor, for example).  Otherwise, they'd have to add
		// both a TreeExpansionListener and a TreeWillExpandListener.  Cheap,
		// I know, but oh well.
		firePropertyChange(WILL_EXPAND_PROPERTY, null, null);

		super.fireTreeWillExpand(e);

		FileSystemTreeNode dmtn =
					(FileSystemTreeNode)e.getLastPathComponent();

		// If the only child is the dummy one, we know we haven't populated
		// this node with true children yet.
		int childCount = dmtn.getChildCount();
		if (childCount==1 && ((FileSystemTreeNode)dmtn.getChildAt(0)).
				containsFile(DUMMY_FILE)) {
			refreshChildren(dmtn);
		}

	}


	/**
	 * Returns the child of the specified node containing the specified file.
	 *
	 * @param node The node whose children you want to search.
	 * @param file The file for which to search.
	 * @return The child node representing the specified file, or
	 *         <code>null</code> if none of the children specified the file.
	 */
	private FileSystemTreeNode getChildRepresentingFile(
								FileSystemTreeNode node, Object file) {
		if (file==null)
			return null;
		int childCount = node.getChildCount();
		for (int i=0; i<childCount; i++) {
			FileSystemTreeNode child =
						(FileSystemTreeNode)node.getChildAt(i);
			if (file.equals(child.getUserObject()))
				return child;
		}
		return null;
	}


	/**
	 * Returns the display name for a given file.
	 *
	 * @param file The file for which to get the display name.
	 * @return The display name.
	 */
	protected String getName(File file) {
		if (file==null)
			return null;
		String name = (String)rootNameCache.get(file);
		if (name!=null)
			return name;
		name = fileSystemView.getSystemDisplayName(file);
		if (name!=null && !EMPTY.equals(name))
			return name;
		return file.getAbsolutePath();
	}


	/**
	 * This method is overridden to ensure that this tree is given a little
	 * bit of room if it is placed into a <code>JScrollPane</code> (which
	 * is likely).  If you don't do this, the tree's preferred width will only
	 * be the maximum width of one of its visible nodes, so it will appear
	 * "squished" into whatever frame/dialog it's in.
	 *
	 * @return The preferred size of this tree.
	 */
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		size.width = Math.max(size.width, 300);
		return size;
	}


	/**
	 * Returns the file currently selected by the user.
	 *
	 * @return The file currently selected, or <code>null</code>
	 *         if no file is selected.
	 * @see #getSelectedFileName
	 * @see #setSelectedFile
	 */
	public File getSelectedFile() {
		TreePath path = getSelectionPath();
		if (path!=null) {
			Object comp = path.getLastPathComponent();
			if (comp instanceof FileSystemTreeNode) {
				FileSystemTreeNode node = (FileSystemTreeNode)comp;
				return (File)node.getUserObject();
			}
		}
		return null;
	}


	/**
	 * Returns any selected files.  This will always be either a zero-length
	 * array, or an array containing only the value returned from
	 * {@link #getSelectedFile()}, since this component only allows selection
	 * of one file at a time.
	 *
	 * @return The selected files.
	 */
	public File[] getSelectedFiles() {
		File file = getSelectedFile();
		if (file!=null) {
			return new File[] { file };
		}
		return new File[0];
	}


	/**
	 * Returns the name of the file currently selected by the user.
	 *
	 * @return The name of the file currently selected, or <code>null</code>
	 *         if no file is selected.
	 * @see #getSelectedFile
	 * @see #setSelectedFile
	 */
	public String getSelectedFileName() {
		File file = getSelectedFile();
		return file!=null ? file.getAbsolutePath() : null;
	}


	/**
	 * Returns the string to use for the tooltip.
	 *
	 * @return The tooltip text.
	 */
	public String getToolTipText(MouseEvent e) {
		String tip = null;
		int x = e.getX();
		int y = e.getY();
		TreePath path = getPathForLocation(x, y);
		if (path!=null) {
			Object comp = path.getLastPathComponent();
			if (comp!=null && comp instanceof FileSystemTreeNode) {
				FileSystemTreeNode node = (FileSystemTreeNode)comp;
				return getName((File)node.getUserObject());
			}
		}
		return tip;
	}


	/**
	 * Does any initialization common to all constructors.
	 */
	private void init() {

		// Make it so they can only select one node at a time.
		TreeSelectionModel tsm = getSelectionModel();
		tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tsm.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				scrollPathToVisible(e.getPath());
			}
		});

		// Set the root.  Note that this must come BEFORE we
		// set the cell renderer (below), otherwise, each tree node's
		// width will initially be incorrect (fixed when the node is
		// expanded/collapsed).
		treeModel = new DefaultTreeModel(root);
		setModel(treeModel);

		cellRenderer = new FileSystemTreeRenderer();
		setCellRenderer(cellRenderer);

		// Make everything look nice.
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		setShowsRootHandles(true);
		setRootVisible(false);

		setTransferHandler(new TreeTransferHandler());
		setDragEnabled(true);

	}


	/**
	 * Called when a mouse event occurs in this file system tree.  This method
	 * is overridden so that we can display our popup menu if necessary.
	 *
	 * @param e The mouse event.
	 */
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if (e.isPopupTrigger()) {
			displayPopupMenu(e.getPoint());
		}
	}


	/**
	 * Refreshes the children of the specified node (representing a directory)
	 * to accurately reflect the files inside of it.
	 *
	 * @param node The node.
	 */
	void refreshChildren(FileSystemTreeNode node) {
		node.removeAllChildren();
		File file = node.getFile();
		if (file.isDirectory()) {
			File[] children = fileSystemView.getFiles(file, false);
			File[] filteredChildren = filterAndSort(children);
			int numChildren = filteredChildren.length;
			for (int i=0; i<numChildren; i++)
				node.add(createTreeNodeFor(filteredChildren[i]));
		}
	}


	/**
	 * Changes the "root" of this tree.
	 *
	 * @param rootFile The new root.  If this is <code>null</code>, then all
	 *        of the file system's roots are used.  If it is a directory, then
	 *        that directory is used.  If it is a plain file, or does not
	 *        exist, an {@link IllegalArgumentException} is thrown.
	 */
	public void setRoot(File rootFile) {

		if (rootFile==null) {
			// Add all of our "root" nodes.
			root = new FileSystemTreeNode();
			for (Iterator i=RootManager.getInstance().iterator(); i.hasNext(); ) {
				File aRoot = (File)i.next();
				// Hack - We "know" all roots are directories, so why query
				// via isDirectory()?  This is a nice performance boost.
				root.add(createTreeNodeForImpl(aRoot, true));
				addCachedRootName(aRoot);
			}
		}

		// Create our single "root" node.
		else if (rootFile.isDirectory()) {
			root = new FileSystemTreeNode();
			root.add(createTreeNodeForImpl(rootFile, true));
			addCachedRootName(rootFile);
		}

		else {
			throw new IllegalArgumentException(
					"root must be 'null' or a directory");
		}

		treeModel = new DefaultTreeModel(root);
		setModel(treeModel);

	}


	/**
	 * Selects the given file in the tree.  If the file does not exist,
	 * then the selection is cleared.
	 *
	 * @param file The file to select.
	 * @return Whether the file exists and was selected.
	 * @see #getSelectedFile
	 */
	public boolean setSelectedFile(final File file) {

		collapseAllNodes();

		if (file==null || !file.exists()) {
			clearSelection();
			return false;
		}

		ArrayList parents = new ArrayList();
		File f2 = file;
		while (f2!=null) {
			parents.add(f2);
			f2 = f2.getParentFile();
		}
		int numParents = parents.size();

		FileSystemTreeNode temp = root;
		TreePath path = null;
		for (int i=numParents-1; i>=0; i--) {
			temp = getChildRepresentingFile(temp, parents.get(i));
			if (temp==null) {
				// Happens e.g. when this is a DirectoryTree instance, and
				// "file" is an actual file, not a directory.  In this
				// case we'll just leave the tree as-is (expanded to the
				// directory of the file specified).
				clearSelection();
				return false;
			}
			path = new TreePath(temp.getPath());
			// Won't work on a leaf node, but who cares...
			expandPath(path);
		}
		setSelectionPath(path);
		scrollPathToVisible(path);

		return true;

	}


	/**
	 * Overridden so that the renderer is also updated on a LnF change.
	 * If we don't do this, the renderer will still work, but will use the
	 * "background color" and other colors from the original LnF.
	 */
	public void updateUI() {

		super.updateUI();

		// Don't create a new one the first time through (done in ctor).
		if (cellRenderer!=null) {
			// NOTE: DefaultTreeCellRenderer caches icons, colors, etc.,
			// so we cannot simply call cellRenderer.updateUI(),
			// unfortunately; we must create a new one ourselves.
			//SwingUtilities.updateComponentTreeUI(cellRenderer);
			cellRenderer = new FileSystemTreeRenderer();
			setCellRenderer(cellRenderer);
		}

		if (popup!=null) {
			SwingUtilities.updateComponentTreeUI(popup);
		}

	}


	static {
		// Some actions only work with Java 6+.
		String ver = System.getProperty("java.specification.version");
		IS_JAVA_6_PLUS = !ver.startsWith("1.4") && !ver.startsWith("1.5");
	}


	/**
	 * The tree node used by the file tree.  This class is mainly here
	 * for debugging purposes and serves no real purpose.
	 */
	public static class FileSystemTreeNode extends DefaultMutableTreeNode {

		public FileSystemTreeNode() {
		}

		public FileSystemTreeNode(Object userObject) {
			super(userObject);
		}

		public boolean containsFile(File file) {
			return (file!=null && file.equals(userObject));
		}

		public boolean equals(Object o2) {
			if (o2 instanceof FileSystemTreeNode) {
				File f2 = ((FileSystemTreeNode)o2).getFile();
				File file = getFile();
				if (file==null) {
					return f2==null;
				}
				return f2!=null && file.equals(f2);
			}
			return false;
		}

		public File getFile() {
			return (File)userObject;
		}

		/**
		 * Overridden since {@link #equals(Object)} was overridden, to keep
		 * them consistent.
		 */
		public int hashCode() {
			return userObject==null ? 0 : userObject.hashCode();
		}

		public String toString() {
			File file = getFile();
			String fileName = file==null ? "<null>" : file.getAbsolutePath();
			return "[FileSystemTreeNode: file=" + fileName + "]";
		}

	}


	/**
	 * Renderer for the file tree.
	 */
	private class FileSystemTreeRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		public Component getTreeCellRendererComponent(JTree tree,
									Object value, boolean sel,
									boolean expanded, boolean leaf,
									int row, boolean hasFocus) 
		{

			super.getTreeCellRendererComponent(tree, value, sel, expanded,
										leaf, row, hasFocus);

			// Make the node have the proper icon and only display the
			// file name.

			// We must check "instanceof File" because it appears that Metal
			// and Motif LnF's call this method during a JTree's setRoot()
			// call (although Windows LnF doesn't... ???), which throws a
			// ClassCastException, as this is evidently called before the
			// root is replaced (and so the root node contains default sample
			// data such as "Colors" or some junk).  So if we check this, we
			// don't cast to File before the stuff has changed to File.
			Object userObj = ((DefaultMutableTreeNode)value).getUserObject();
			if (userObj instanceof File && row!=-1) {
				File file = (File)userObj;
				setText(FileSystemTree.this.getName(file));
				setIcon(iconManager.getIcon(file));
			}

			return this;

		}

	}


	/**
	 * Transfer handler for copying files from a FileSystemTree.
	 */
	private static class TreeTransferHandler extends TransferHandler {

		public boolean canImport(JComponent comp, DataFlavor[] flavors) {
			return false;
		}

		protected Transferable createTransferable(JComponent c) {
			List list = null;
			FileSystemTree tree = (FileSystemTree)c;
			File file = tree.getSelectedFile();
			if (file!=null) {
				list = new ArrayList(1);
				list.add(file);
			}
			return new FileListTransferable(list);
		}

		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}

	}


}