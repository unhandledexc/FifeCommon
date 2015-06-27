/*
 * 11/27/2004
 *
 * StandardAction.java - An action used by a GUIApplication implementation.
 * Copyright (C) 2004 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.ui.app;

import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.fife.ui.OS;


/**
 * The action type used by all instances of <code>GUIApplication</code>.  This
 * class not only provides ease-of-use methods, but knows how to look up its
 * properties in a resource bundle.<p>
 * 
 * For example, you could define an action to create a new document like so in
 * a properties file:
 * 
 * <pre>
 * NewAction=New
 * NewAction.Mnemonic=N
 * NewAction.ShortDesc=Creates a new text file.
 * NewAction.Accelerator=default N
 * </pre>
 *
 * <p>
 * This creates an action that can be used for a menu item (for example), with
 * label "New", mnemonic 'N', an appropriate short description that gets
 * displayed in the application's status bar on rollover, and an accelerator.
 * </p>
 * 
 * <p>
 * For accelerators, the standard syntax for key strokes defined 
 * <a href="https://docs.oracle.com/javase/7/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)">here</a>
 * can be used.  In addition, the string literal "default" maps to "ctrl" on
 * Windows and Linux, and "meta" on OS X.
 * </p>
 * 
 * <p>
 * In addition, OS-specific accelerators can be defined, for example:
 * </p>
 * 
 * <pre>
 * NextTabAction.Accelerator.OSX=meta shift BRACELEFT
 * NextTabAction.Accelerator.Windows=ctrl PAGE_DOWN
 * NextTabAction.Accelerator.Linux=ctrl TAB
 * </pre>
 * 
 * <p>
 * If the appropriate OS-specific accelerator is defined for an action, it is
 * used, otherwise, the OS-agnostic accelerator is used, if defined.
 * </p>
 * 
 * @author Robert Futrell
 * @version 0.6
 * @see org.fife.ui.app.GUIApplication
 */
public abstract class StandardAction extends AbstractAction {

	/**
	 * A property specific to StandardActions (though other custom actions can
	 * certainly use it) that stores the default accelerator for the action.
	 * <code>KeyStroke</code>s should be stored with this key.
	 */
	public static final String DEFAULT_ACCELERATOR =
			"StandardAction.DefaultAccelerator";


	/**
	 * The parent GUI application.
	 */
	private GUIApplication app;


	/**
	 * Creates the action.  The parent class should call
	 * {@link #setName(String)}, {@link #setIcon(String)}, or whatever other
	 * methods are necessary to set this action up.
	 *
	 * @param app The parent application.
	 */
	public StandardAction(GUIApplication app) {
		this.app = app;
	}


	/**
	 * Creates an action, initializing its properties from the parent
	 * application's resource bundle. The name of the action is found using the
	 * specified key.  If keys exist with the names:
	 * <ul>
	 *    <li><code>key + ".Mnemonic"</code>
	 *    <li><code>key + ".Accelerator"</code>
	 *    <li><code>key + ".ShortDesc"</code>
	 * </ul>
	 * then those properties are set as well.  Further, if an accelerator is
	 * defined, it is set as both the action's active accelerator and default
	 * accelerator.<p>
	 * 
	 * You can provide OS-specific accelerators for actions by defining any of
	 * the following properties:
	 * <ul>
	 *    <li><code>key + ".Accelerator.OSX"</code>
	 *    <li><code>key + ".Accelerator.Windows"</code>
	 *    <li><code>key + ".Accelerator.Linux"</code> (applies to Unix as well)
	 * </ul>
	 * This is useful for instances where different operating systems have
	 * different "standard" shortcuts for things.
	 * If the appropriate OS-specific accelerator is not defined, then the
	 * default value (<code>key + ".Accelerator"</code>) is used.
	 *
	 * @param app The parent application.
	 * @param key The key in the bundle for the name of this action.
	 */
	public StandardAction(GUIApplication app, String key) {
		this(app, key, null);
	}


	/**
	 * Creates an action, initializing its properties from the parent
	 * application's resource bundle. The name of the action is found using the
	 * specified key.  If keys exist with the names:
	 * <ul>
	 *    <li><code>key + ".Mnemonic"</code>
	 *    <li><code>key + ".Accelerator"</code>
	 *    <li><code>key + ".ShortDesc"</code>
	 * </ul>
	 * then those properties are set as well.  Further, if an accelerator is
	 * defined, it is set as both the action's active accelerator and default
	 * accelerator.<p>
	 * 
	 * You can provide OS-specific accelerators for actions by defining any of
	 * the following properties:
	 * <ul>
	 *    <li><code>key + ".Accelerator.OSX"</code>
	 *    <li><code>key + ".Accelerator.Windows"</code>
	 *    <li><code>key + ".Accelerator.Linux"</code> (applies to Unix as well)
	 * </ul>
	 * This is useful for instances where different operating systems have
	 * different "standard" shortcuts for things.
	 * If the appropriate OS-specific accelerator is not defined, then the
	 * default value (<code>key + ".Accelerator"</code>) is used.
	 *
	 * @param app The parent application.
	 * @param key The key in the bundle for the name of this action.
	 * @param icon The name of the icon resource for this action, or
	 *        <code>null</code> for no icon.
	 */
	public StandardAction(GUIApplication app, String key, String icon) {
		this(app, app.getResourceBundle(), key);
		if (icon!=null) {
			setIcon(icon);
		}
	}


	private static final String shortcutExtension(OS os) {
		String extension = ".Accelerator";
		String suffix = null;
		if (os != null) {
			switch (os) {
				case MAC_OS_X:
					suffix = ".OSX";
					break;
				case LINUX:
					suffix = ".Linux";
					break;
				case WINDOWS:
					suffix = ".Windows";
					break;
				case SOLARIS:
					suffix = ".Solaris";
					break;
				default:
					suffix = null;
					break;
			}
		}
		if (suffix != null) {
			extension += suffix;
		}
		return extension;
	}


	/**
	 * Creates an action, initializing its properties from a resource bundle.
	 * The name of the action is found using the specified key.  If keys exist
	 * with the names:
	 * <ul>
	 *    <li><code>key + ".Mnemonic"</code>
	 *    <li><code>key + ".Accelerator"</code>
	 *    <li><code>key + ".ShortDesc"</code>
	 * </ul>
	 * then those properties are set as well.  Further, if an accelerator is
	 * defined, it is set as both the action's active accelerator and default
	 * accelerator.<p>
	 * 
	 * You can provide OS-specific accelerators for actions by defining any of
	 * the following properties:
	 * <ul>
	 *    <li><code>key + ".Accelerator.OSX"</code>
	 *    <li><code>key + ".Accelerator.Windows"</code>
	 *    <li><code>key + ".Accelerator.Linux"</code> (applies to Unix as well)
	 * </ul>
	 * This is useful for instances where different operating systems have
	 * different "standard" shortcuts for things.
	 * If the appropriate OS-specific accelerator is not defined, then the
	 * default value (<code>key + ".Accelerator"</code>) is used.
	 *
	 * @param app The parent application.
	 * @param msg The bundle to localize from.  If this is <code>null</code>,
	 *        then <code>app.getResourceBundle()</code> is used.
	 * @param key The key in the bundle for the name of this action.
	 */
	public StandardAction(GUIApplication app, ResourceBundle msg, String key) {

		this(app);
		if (msg==null) {
			msg = app.getResourceBundle();
		}

		// TODO: Use msg.containsKey() when we drop 1.4/1.5 support
		try {
			setName(msg.getString(key));
		} catch (MissingResourceException mre) {
			// Swallow
		}

		// TODO: Use msg.containsKey() when we drop 1.4/1.5 support
		try {
			String mnemonicKey = key + ".Mnemonic";
			setMnemonic(msg.getString(mnemonicKey).charAt(0));
		} catch (MissingResourceException mre) {
			// Swallow
		}

		// Try OS-specific shortcut first, then generic shortcut
		KeyStroke ks = getKeyStroke(msg, key + shortcutExtension(OS.get()));
		if (ks == null) {
			ks = getKeyStroke(msg, key + shortcutExtension(null));
		}
		if (ks != null) {
			setAccelerator(ks);
			setDefaultAccelerator(ks);
		}

		// TODO: Use msg.containsKey() when we drop 1.4/1.5 support
		try {
			String shortDescKey = key + ".ShortDesc";
			setShortDescription(msg.getString(shortDescKey));
		} catch (MissingResourceException mre) {
			// Swallow
		}
	}


	/**
	 * Returns the accelerator for this action.
	 *
	 * @return The accelerator.
	 * @see #setAccelerator(KeyStroke)
	 */
	public KeyStroke getAccelerator() {
		return (KeyStroke)getValue(ACCELERATOR_KEY);
	}


	/**
	 * Returns the application.
	 *
	 * @return The application.
	 */
	public GUIApplication getApplication() {
		return app;
	}


	/**
	 * Returns the default accelerator for this action.  This is the
	 * accelerator that should be restored if the user chooses to "restore
	 * defaults" in the options dialog.
	 *
	 * @return The default accelerator.
	 * @see #setDefaultAccelerator(KeyStroke)
	 * @see #getAccelerator()
	 * @see #restoreDefaultAccelerator()
	 * @see #DEFAULT_ACCELERATOR
	 */
	public KeyStroke getDefaultAccelerator() {
		return (KeyStroke)getValue(DEFAULT_ACCELERATOR);
	}


	/**
	 * Returns the icon for this action.
	 *
	 * @return The icon.
	 * @see #setIcon(Icon)
	 */
	public Icon getIcon() {
		return (Icon)getValue(SMALL_ICON);
	}


	/**
	 * If a property is found in a resource bundle, it is assumed to be a string
	 * representation of a key stroke, and a <code>KeyStroke</code> is made
	 * from it.
	 *
	 * @param msg The resource bundle.
	 * @param key The key.
	 * @return The key stroke, or <code>null</code> if the property is not
	 *         found or the value is not a valid key stroke.
	 */
	private KeyStroke getKeyStroke(ResourceBundle msg, String key) {
		// TODO: Use msg.containsKey() when we drop 1.4/1.5 support
		KeyStroke ks = null;
		try {
			String temp = msg.getString(key);
			if (temp!=null) {
				temp = massageAcceleratorString(temp);
				ks = KeyStroke.getKeyStroke(temp);
			}
		} catch (MissingResourceException mre) {
			// Swallow
		}
		return ks;
	}


	/**
	 * Returns the mnemonic for this action.
	 *
	 * @return The mnemonic, or <code>-1</code> if not defined.
	 * @see #setMnemonic(int)
	 */
	public int getMnemonic() {
		Integer i = (Integer)getValue(MNEMONIC_KEY);
		return i!=null ? i.intValue() : -1;
	}


	/**
	 * Returns the name of this action.
	 *
	 * @return The name of this action.
	 * @see #setName(String)
	 */
	public String getName() {
		return (String)getValue(NAME);
	}


	/**
	 * Returns the short description for this action.
	 *
	 * @return The description.
	 * @see #setShortDescription(String)
	 */
	public String getShortDescription() {
		return (String)getValue(SHORT_DESCRIPTION);
	}


	/**
	 * Ensures an accelerator string uses the right modifier key for the
	 * current OS.  If the modifier "default" is found, it is replaced
	 * with the appropriate default shortcut modifier.
	 *
	 * @param accelerator The accelerator string, for example,
	 *        <code>"default O"</code> or <code>"ctrl SPACE"</code>.
	 * @return A (possibly) modified version of that string.
	 */
	private String massageAcceleratorString(String accelerator) {

		final String DEFAULT = "default ";
		int index = accelerator.indexOf(DEFAULT);

		if (index>-1) {
			String replacement = app.getOS()==OS.MAC_OS_X ? "meta ":"control ";
			accelerator = accelerator.substring(0, index) + replacement +
					accelerator.substring(index + DEFAULT.length());
		}

		return accelerator;
	}


	/**
	 * Restores the default accelerator for this action, clearing the
	 * accelerator if there is no default.
	 *
	 * @see #getDefaultAccelerator()
	 * @see #setDefaultAccelerator(KeyStroke)
	 */
	public void restoreDefaultAccelerator() {
		setAccelerator(getDefaultAccelerator());
	}


	/**
	 * Sets the accelerator for this action.
	 *
	 * @param accelerator The new accelerator, or <code>null</code> for none.
	 * @see #getAccelerator()
	 * @see #setDefaultAccelerator(KeyStroke)
	 */
	public void setAccelerator(KeyStroke accelerator) {
		putValue(ACCELERATOR_KEY, accelerator);
	}


	/**
	 * Sets the default accelerator for this action.  Applications typically
	 * won't call this method directly, as the default accelerator is usually
	 * set from the properties file we're loaded from.
	 * 
	 * @param accelerator The new default accelerator, which may be
	 *        <code>null</code>.
	 * @see #getDefaultAccelerator()
	 * @see #setAccelerator(KeyStroke)
	 * @see #restoreDefaultAccelerator()
	 * @see #DEFAULT_ACCELERATOR
	 */
	public void setDefaultAccelerator(KeyStroke accelerator) {
		putValue(DEFAULT_ACCELERATOR, accelerator);
	}


	/**
	 * Sets the icon of this action.
	 *
	 * @param icon The icon.
	 * @see #getIcon()
	 * @see #setIcon(String)
	 * @see #setIcon(URL)
	 */
	public void setIcon(Icon icon) {
		putValue(SMALL_ICON, icon);
	}


	/**
	 * Sets the icon of this action.  This method is equivalent to:
	 * <pre>setIcon(getClass().getResource(res))</pre>.
	 *
	 * @param res The resource containing the icon.
	 * @see #getIcon()
	 * @see #setIcon(URL)
	 * @see #setIcon(Icon)
	 */
	public void setIcon(String res) {
		setIcon(getClass().getResource(res));
	}
	
	
	/**
	 * Sets the icon of this action.
	 *
	 * @param res The resource containing the icon.
	 * @see #getIcon()
	 * @see #setIcon(String)
	 * @see #setIcon(Icon)
	 */
	public void setIcon(URL res) {
		setIcon(new ImageIcon(res));
	}
	
	
	/**
	 * Sets the mnemonic for this action.
	 *
	 * @param mnemonic The new mnemonic.  A value of <code>-1</code> means
	 *        "no mnemonic."
	 * @see #getMnemonic()
	 */
	public void setMnemonic(int mnemonic) {
		// TODO: When we drop 1.4 support, use Integer.valueOf(mnemonic).
		putValue(MNEMONIC_KEY, mnemonic>0 ? new Integer(mnemonic) : null);
	}


	/**
	 * Sets the name of this action.
	 *
	 * @param name The name of this action.
	 * @see #getName()
	 */
	public void setName(String name) {
		putValue(NAME, name);
	}


	/**
	 * Sets the short description for this action.
	 *
	 * @param desc The description.
	 * @see #getShortDescription()
	 */
	public void setShortDescription(String desc) {
		putValue(SHORT_DESCRIPTION, desc);
	}


}