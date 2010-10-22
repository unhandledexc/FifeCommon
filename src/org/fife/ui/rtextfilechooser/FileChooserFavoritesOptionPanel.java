/*
 * 01/16/2008
 *
 * FileChooserFavoritesOptionPanel.java - Option panel for managing the
 * file chooser "Favorite Directories" list.
 * Copyright (C) 2008 Robert Futrell
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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.fife.ui.EscapableDialog;
import org.fife.ui.OptionsDialogPanel;
import org.fife.ui.ResizableFrameContentPane;
import org.fife.ui.UIUtil;
import org.fife.ui.modifiabletable.ModifiableTable;
import org.fife.ui.modifiabletable.ModifiableTableChangeEvent;
import org.fife.ui.modifiabletable.ModifiableTableListener;
import org.fife.ui.modifiabletable.RowHandler;


/**
 * Option panel for managing the "Favorite Directories" of an
 * <code>RTextFileChooser</code>.  This option panel can be added to an
 * options dialog if this file chooser has "favorites" enabled.
 *
 * @author Robert Futrell
 * @version 0.5
 */
public class FileChooserFavoritesOptionPanel extends OptionsDialogPanel
						implements ModifiableTableListener {

	private static final long serialVersionUID = 1L;

	public static final String FAVORITES_PROPERTY	= "Favorites";

	private FavoritesTableModel model;
	private ModifiableTable modifiableTable;

	private static final String MSG =
			"org.fife.ui.rtextfilechooser.FileChooserFavoritesOptionPanel";

	private static final String EDIT_FAVORITES_DIALOG_MSG =
					"org.fife.ui.rtextfilechooser.EditFavoriteDialog";


	/**
	 * Constructor.  All strings in the file chooser are initialized via the
	 * current locale.
	 */
	public FileChooserFavoritesOptionPanel() {

		ComponentOrientation orientation = ComponentOrientation.
									getOrientation(getLocale());

		ResourceBundle msg = ResourceBundle.getBundle(MSG);
	
		setName(msg.getString("Favorites"));
		setBorder(UIUtil.getEmpty5Border());
		setLayout(new BorderLayout());

		JPanel favoritesPanel = new JPanel(new BorderLayout());
		favoritesPanel.setBorder(new OptionPanelBorder(
								msg.getString("FavoritesSection")));
		String header = msg.getString("FavoriteTable.Header");
		model = new FavoritesTableModel(header);
		modifiableTable = new ModifiableTable(model,
				BorderLayout.SOUTH, ModifiableTable.ALL_BUTTONS);
		modifiableTable.setRowHandler(new FavoritesRowHandler());
		modifiableTable.addModifiableTableListener(this);
		JTable favoritesTable = modifiableTable.getTable();
		favoritesTable.setDefaultRenderer(Object.class, createCellRenderer());
		favoritesTable.setShowGrid(false);
		favoritesPanel.add(modifiableTable);
		add(favoritesPanel);

		applyComponentOrientation(orientation);

	}


	/**
	 * Creates and returns the renderer used in this option panel's table.
	 *
	 * @return The renderer.
	 */
	private DefaultTableCellRenderer createCellRenderer() {
		// Explicitly use a DefaultTableCellRenderer in case some future
		// version of Java uses a different one.  We'll manipulate it
		// later.
		DefaultTableCellRenderer r = new DefaultTableCellRenderer();
		r.setIcon(FileChooserIconManager.createFolderIcon());
		ComponentOrientation orientation = ComponentOrientation.
									getOrientation(getLocale());
		r.setComponentOrientation(orientation);
		return r;
	}


	/**
	 * Applies the settings entered into this dialog on the specified
	 * application.
	 *
	 * @param owner The application.  This application should implement
	 *        {@link FileChooserOwner}.
	 * @throws IllegalArgumentException If <code>owner</code> is not a
	 *         {@link FileChooserOwner}.
	 */
	protected void doApplyImpl(Frame owner) {
		if (!(owner instanceof FileChooserOwner)) {
			throw new IllegalArgumentException(
									"owner must be a FileChooserOwner");
		}
		FileChooserOwner fco = (FileChooserOwner)owner;
		installFavorites(fco.getFileChooser());
	}


	/**
	 * {@inheritDoc}
	 */
	protected OptionsPanelCheckResult ensureValidInputsImpl() {
		return null;
	}


	/**
	 * Returns the <code>JComponent</code> at the "top" of this Options
	 * panel.  This is the component that will receive focus if the user
	 * switches to this Options panel in the Options dialog.  As an added
	 * bonus, if this component is a <code>JTextComponent</code>, its
	 * text is selected for easy changing.
	 */
	public JComponent getTopJComponent() {
		return modifiableTable;
	}


	/**
	 * Changes the specified file chooser's favorites so they match
	 * those entered in this options panel.
	 *
	 * @param chooser The file choose whose favorites to modify.
	 * @see #setFavorites(RTextFileChooser)
	 */
	public void installFavorites(RTextFileChooser chooser) {
		model.setChooserFavorites(chooser);
	}


	/**
	 * Called whenever the extension/color mapping table is changed.
	 *
	 * @param e An event describing the change.
	 */
	public void modifiableTableChanged(ModifiableTableChangeEvent e) {
		hasUnsavedChanges = true;
		firePropertyChange(FAVORITES_PROPERTY, null, new Integer(e.getRow()));
	}


	/**
	 * Sets the favorites displayed in this option panel to those
	 * known by the specified file chooser.
	 *
	 * @param chooser The file chooser
	 * @see #installFavorites(RTextFileChooser)
	 */
	public void setFavorites(RTextFileChooser chooser) {
		model.initFavorites(chooser);
	}


	/**
	 * Sets the values displayed by this panel to reflect those in the
	 * application.  Child panels are not handled.
	 *
	 * @param owner The parent application.  This should implement
	 *        {@link FileChooserOwner}.
	 * @throws IllegalArgumentException If <code>owner</code> is not a
	 *         {@link FileChooserOwner}.
	 * @see #setValues(Frame)
	 */
	protected void setValuesImpl(Frame owner) {
		if (!(owner instanceof FileChooserOwner)) {
			throw new IllegalArgumentException(
									"owner must be a FileChooserOwner");
		}
		FileChooserOwner fco = (FileChooserOwner)owner;
		setFavorites(fco.getFileChooser());
	}


	/**
	 * Updates this panel's UI in response to a LaF change.  This is
	 * overridden to update the table's renderer.
	 */
	public void updateUI() {
		if (modifiableTable!=null) {
			// We explicitly set a DefaultTableCellRenderer, so this cast
			// is safe. Update the renderer's icon to the folder icon used
			// in this LaF.
			DefaultTableCellRenderer r = (DefaultTableCellRenderer)
				modifiableTable.getTable().getDefaultRenderer(Object.class);
			r.setIcon(FileChooserIconManager.createFolderIcon());
		}
		super.updateUI();
	}


	/**
	 * The dialog that allows the user to add or modify Favorite.
	 */
	private static class EditFavoriteDialog extends EscapableDialog
				implements ActionListener, DocumentListener {

		static final int OK		= 0;
		static final int CANCEL	= 1;

		private JTextField dirField;
		private JButton okButton;
		private JButton cancelButton;
		private int rc;

		public EditFavoriteDialog(JDialog owner) {

			super(owner);
			ComponentOrientation orientation = ComponentOrientation.
										getOrientation(getLocale());
			ResourceBundle msg = ResourceBundle.
								getBundle(EDIT_FAVORITES_DIALOG_MSG);
			JPanel contentPane = new ResizableFrameContentPane(
											new BorderLayout());
			contentPane.setBorder(UIUtil.getEmpty5Border());

			// Panel containing main stuff.
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
			JPanel temp = new JPanel(new BorderLayout());
			temp.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
			JLabel label = UIUtil.createLabel(msg,
								"Directory.Text", "Directory.Mnemonic");
			JPanel temp2 = new JPanel(new BorderLayout());
			temp2.add(label);
			if (orientation.isLeftToRight()) { // Space between label and text field.
				temp2.setBorder(BorderFactory.createEmptyBorder(0,0,0,5));
			}
			else {
				temp2.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
			}
			temp.add(temp2, BorderLayout.LINE_START);
			dirField = new JTextField(40);
			dirField.getDocument().addDocumentListener(this);
			label.setLabelFor(dirField);
			temp.add(dirField);
			JButton browseButton = new JButton(msg.getString("Browse.Text"));
			browseButton.setMnemonic((int)msg.getString("Browse.Mnemonic").
													charAt(0));
			browseButton.setActionCommand("Browse");
			browseButton.addActionListener(this);
			temp2 = new JPanel(new BorderLayout());
			temp2.add(browseButton);
			if (orientation.isLeftToRight()) { // Space between text field and button.
				temp2.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
			}
			else {
				temp2.setBorder(BorderFactory.createEmptyBorder(0,0,0,5));
			}
			temp.add(temp2, BorderLayout.LINE_END);
			topPanel.add(temp);
			contentPane.add(topPanel, BorderLayout.NORTH);

			// Panel containing buttons for the bottom.
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
			temp = new JPanel(new GridLayout(1,2, 5,5));
			okButton = UIUtil.createRButton(msg,
								"OK.Text", "OK.Mnemonic");
			okButton.addActionListener(this);
			temp.add(okButton);
			cancelButton = UIUtil.createRButton(msg,
								"Cancel.Text", "Cancel.Mnemonic");
			cancelButton.addActionListener(this);
			temp.add(cancelButton);
			buttonPanel.add(temp);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);

			// Get ready to go.
			setTitle(msg.getString("Title"));
			setContentPane(contentPane);
			getRootPane().setDefaultButton(okButton);
			setModal(true);
			applyComponentOrientation(orientation);
			pack();

		}

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source==okButton) {
				rc = OK;
				escapePressed();
			}
			else if (source==cancelButton) {
				escapePressed();
			}
			else {
				String command = e.getActionCommand();
				if ("Browse".equals(command)) {
					RDirectoryChooser chooser =
						new RDirectoryChooser((JDialog)getOwner());
					chooser.setChosenDirectory(new File(getDirectory()));
					chooser.setVisible(true);
					String chosenDir = chooser.getChosenDirectory();
					if (chosenDir!=null) {
						dirField.setText(chosenDir);
					}
				}
			}
		}

		public void changedUpdate(DocumentEvent e) {
		}

		public String getDirectory() {
			return dirField.getText();
		}

		public void insertUpdate(DocumentEvent e) {
			okButton.setEnabled(true);
		}

		public void removeUpdate(DocumentEvent e) {
			okButton.setEnabled(dirField.getDocument().getLength()>0);
		}

		public void setData(String dir) {
			dirField.setText(dir);
		}

		public int showEditFavoriteDialog() {
			rc = CANCEL; // Set here in case they "X" the dialog out.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dirField.requestFocusInWindow();
					dirField.selectAll();
				}
			});
			setLocationRelativeTo(getOwner());
			okButton.setEnabled(false);
			setVisible(true);
			return rc;
		}

	}


	/**
	 * Handles the addition, removal, and modifying of rows in the Favorites
	 * table.
	 */
	class FavoritesRowHandler implements RowHandler {

		private EditFavoriteDialog dialog;

		public Object[] getNewRowInfo(Object[] oldData) {
			if (dialog==null) {
				dialog = new EditFavoriteDialog(getOptionsDialog());
			}
			dialog.setData(oldData==null ? null : ((String)oldData[0]));
			int rc = dialog.showEditFavoriteDialog();
			if (rc==EditFavoriteDialog.OK) {
				return new Object[] { dialog.getDirectory() };
			}
			return null;
		}

		public boolean shouldRemoveRow(int row) {
			return true;
		}

		/**
		 * Not an override.  Implements <code>RowHandler#updateUI()</code>.
		 */
		public void updateUI() {
			if (dialog!=null) {
				SwingUtilities.updateComponentTreeUI(dialog);
			}
		}

	}


	/**
	 * The table model used by the Favorites table.
	 */
	private static class FavoritesTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames;

		public FavoritesTableModel(String favoriteHeader) {
			columnNames = new String[1];
			columnNames[0] = favoriteHeader;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int column) {
			return columnNames[column];
		}

		public void initFavorites(RTextFileChooser chooser) {
			setRowCount(0);
			String[] favorites = chooser.getFavorites(); // non-null
			for (int i=0; i<favorites.length; i++) {
				// DefaultTableModel uses Vectors internally, so we'll
				// use them here too.
				Vector v = new Vector(2);
				String favorite = favorites[i];
				v.add(favorite);
				addRow(v);
			}
		}

		public void setChooserFavorites(RTextFileChooser chooser) {
			chooser.clearFavorites();
			for (int i=0; i<getRowCount(); i++) {
				String favorite = (String)getValueAt(i, 0);
				chooser.addToFavorites(favorite);
			}
		}

	}


}