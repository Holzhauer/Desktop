package org.freeplane.core.util;

import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.features.mode.Controller;

/**
 * Provides methods and constants which are dependend on the underlying java version
 * 
 * @author robert.ladstaetter
 */
public class Compat {
	public static final String PROPERTY_FREEPLANE_USERDIR = "org.freeplane.userfpdir";
	public static final String JAVA_VERSION = System.getProperty("java.version");
	public static final String VERSION_1_6_0 = "1.6.0";

	private static enum OS {
		MAC, WINDOWS, OTHER
	};

	private static OS os = null;
	public static final Set<String> executableExtensions = new HashSet<String>(Arrays.asList(new String[] { "exe",
	        "com", "vbs", "bat", "lnk", "cmd" }));

	public static boolean isWindowsExecutable(final URI link) {
		if (link == null 
				|| !"file".equalsIgnoreCase(link.getScheme())) {
			return false;
		}
		return isWindowsOS() && executableExtensions.contains(FileUtils.getExtension(link.toString()));
	}

	public static URL fileToUrl(final File pFile) throws MalformedURLException {
		return pFile.toURI().toURL();
	}

	public static boolean isLowerJdk(final String version) {
		return JAVA_VERSION.compareTo(version) < 0;
	}

	public static boolean isMacOsX() {
		Compat.initOS();
		return os.equals(OS.MAC);
	}

	private static void initOS() {
		if (os == null) {
			String osProperty;
			try {
				osProperty = System.getProperty("os.name");
			}
			catch (final SecurityException e) {
				osProperty = "";
			}
			String debugOsName;
			try {
				debugOsName = System.getProperty("freeplane.debug.os.name", "");
			}
			catch (final SecurityException e) {
				debugOsName = "";
			}
			if (osProperty.startsWith("Mac OS") || debugOsName.startsWith("Mac")) {
				os = OS.MAC;
				return;
			}
			if (osProperty.startsWith("Windows") || debugOsName.startsWith("Windows")) {
				os = OS.WINDOWS;
				return;
			}
			os = OS.OTHER;
		}
	}

	public static boolean isWindowsOS() {
		Compat.initOS();
		return os.equals(OS.WINDOWS);
	}

	/**
	 * This is a correction of a method getFile of a class URL. Namely, on
	 * Windows it returned file paths like /C: etc., which are not valid on
	 * Windows. This correction is heuristic to a great extend. One of the
	 * reasons is that file: something every browser and every system uses
	 * slightly differently.
	 */
	private static String urlGetFile(final URL url) {
		
		if( !url.getProtocol().equals("file"))
			return null;
		//DOCEAR - decode url before use it as a string
		String fileName = sun.net.www.ParseUtil.decode(url.toString()).replaceFirst("^file:", "");
		final String osNameStart = System.getProperty("os.name").substring(0, 3);
		if (osNameStart.equals("Win") && url.getProtocol().equals("file")) {
			fileName = fileName.replace('/', File.separatorChar);
			return (fileName.indexOf(':') >= 0) ? fileName.replaceFirst("^\\\\*", "") : fileName;
		}
		else {
			return fileName;
		}
	}

	public static File urlToFile(final URL pUrl) throws URISyntaxException {
		final String path = Compat.urlGetFile(pUrl);
		if(path != null)
			return new File(path);
		else
			return null;
	}

	public static void macAppChanges() {
		if (!Compat.isMacOsX()) {
			return;
		}
		try {
			final Class<?> macChanges = Controller.class.getClassLoader().loadClass(
			    "org.freeplane.plugin.macos.MacChanges");
			final Method method = macChanges.getMethod("apply", Controller.class);
			method.invoke(null, Controller.getCurrentController());
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void macMenuChanges() {
		if (!Compat.isMacOsX()) {
			return;
		}
		final Controller controller = Controller.getCurrentController();
		final Set<String> modes = controller .getModes();
		for (final String mode : modes) {
			final MenuBuilder builder = controller.getModeController(mode).getUserInputListenerFactory()
			    .getMenuBuilder();
			final String[] keys = { 
					"MB_ToggleMenubarAction", 
					"MP_ToggleMenubarAction", 
					"MB_QuitAction",
			        "MB_PropertyAction", 
			        "MB_AboutAction" 
			};
			for (final String key : keys) {
				if (builder.contains(key)) {
					builder.removeElement(key);
				}
			}
		}
	}
	final private static String CURRENT_VERSION_DIR= File.separatorChar + "1.3.x";
	
	/** the directory *including* the version directory. */
	public static String getApplicationUserDirectory() {
		String userFpDir = System.getProperty(PROPERTY_FREEPLANE_USERDIR);
		
		if(userFpDir == null) {
			userFpDir = getDefaultApplicationUserDirectory();
			if(userFpDir.endsWith("freeplane")){
				userFpDir += CURRENT_VERSION_DIR;
			}
		}
		return userFpDir;
	}

	/** the default FP directory *excluding* the version directory. */
    public static String getDefaultApplicationUserDirectory() {
    	Properties freeplaneProperties = new Properties();
		try {
			freeplaneProperties.load(Compat.class.getClassLoader().getResourceAsStream(ResourceController.FREEPLANE_PROPERTIES));
		} catch (IOException e) {
			LogUtils.warn(e);
		}
		String applicationName = freeplaneProperties.getProperty("ApplicationName", "Freeplane");
        return System.getProperty("user.home")+ File.separator + "."+ applicationName.toLowerCase();
	}

	static public String smbUri2unc(final URI uri) {
		String uriString;
 		uriString = ("//" + uri.getHost() + uri.getPath()) .replace('/', '\\');
		final String fragment = uri.getFragment();
		if(fragment != null)
			uriString = uriString + '#' + fragment;
		return uriString;
	}

	static public boolean isPlainEvent(final MouseEvent e) {
        final int modifiers = getModifiers(e);
        return modifiers == 0;
    }

	private static int getModifiers(final MouseEvent e) {
	    return e.getModifiersEx() & 
        		(InputEvent.CTRL_DOWN_MASK 
        				| InputEvent.META_DOWN_MASK
        				| InputEvent.SHIFT_DOWN_MASK
        				| InputEvent.ALT_DOWN_MASK
        				);
    }

	static public boolean isCtrlEvent(final MouseEvent e) {
         return isExtendedCtrlEvent(e, 0);
    }

	public static boolean isCtrlShiftEvent(MouseEvent e) {
		return isExtendedCtrlEvent(e, InputEvent.SHIFT_DOWN_MASK);
    }
	
	public static boolean isCtrlAltEvent(MouseEvent e) {
		return isExtendedCtrlEvent(e, InputEvent.ALT_DOWN_MASK);
    }
	
	static private boolean isExtendedCtrlEvent(final MouseEvent e, int otherModifiers) {
        final int modifiers = getModifiers(e);
		if (isMacOsX())
        	return modifiers == (InputEvent.META_DOWN_MASK | otherModifiers);
        return modifiers == (InputEvent.CTRL_DOWN_MASK|otherModifiers);
    }

	public static boolean isShiftEvent(MouseEvent e) {
        final int modifiers = getModifiers(e);
        return modifiers == InputEvent.SHIFT_DOWN_MASK;
    }

	public static void macAppFullScreen(JFrame frame) {
		if (!isMacOsX())
			return;
		try {
			Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
			Class<?> params[] = new Class[]{Window.class, Boolean.TYPE};
			Method method = util.getMethod("setWindowCanFullScreen", params);
			method.invoke(util, frame, true);
		}
		catch(ClassNotFoundException e) {
		}
		catch (Exception e) {
			LogUtils.warn("OS X Fullscreen FAIL", e);
		}
	}


}
