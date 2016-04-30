package server;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CustomClassLoader extends URLClassLoader {

	/** This constructor is used when this class loader is initially set as the default system
	 * class loader.  It will set the initial classpath using URLs obtained from getNestedURLs
	 */
	public CustomClassLoader(ClassLoader l) throws IOException {
		super(getNestedURLs(), l.getParent());
	}

	/** This method will return URLs for all jar files in the local lib directory to the classpath,
	 * as well as the URL for the local bin directory.  This is used for help with initialization
	 */
	private static URL[] getNestedURLs() {

		String binDirString = CustomClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String libDirString = binDirString.substring(0, binDirString.length() - 4) + "lib/";

		File libDirFile = new File(libDirString);
		File[] jarFiles = libDirFile.listFiles();
		URL[] urls = new URL[jarFiles.length + 1];
		try {
			urls[0] = new URL("file://" + binDirString);

			for (int i = 1; i < urls.length; i++) {
				urls[i] = new URL(jarFiles[i - 1].toURI().toString());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return urls;
	}

	/** Add a specific URL to the class path.
	 * In order to facilitate this method, reflection needed to be use to access the method
	 * since it is protected.  The following technique was obtained from
	 * http://baptiste-wicht.com/posts/2010/05/tip-add-resources-dynamically-to-a-classloader.html 
	 */
	public static void addURLToSystemClassLoader(URL url) throws IntrospectionException {
		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

		try {
			Method method = classLoaderClass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(systemClassLoader, new Object[] { url });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IntrospectionException("Error when adding url to system ClassLoader ");
		}
	}

	/** Deprecated method: Trying to replicate the findClass() method of URLClassLoader using reflection*/
	public static Class findClassWithSystemClassLoader(String name) throws IntrospectionException {
		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

		Class convertedClass = null;

		for (Method m : classLoaderClass.getDeclaredMethods()) {
			System.out.print(m.getName() + " ");

			for (Class c : m.getParameterTypes()) {
				System.out.print(c.getName() + " ");
			}

			System.out.println(", return: " + m.getReturnType().getName());
		}
		try {
			Method method = classLoaderClass.getDeclaredMethod("findClass", new Class[] { String.class });
			method.setAccessible(true);
			convertedClass = (Class) method.invoke(systemClassLoader, new Object[] { name });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IntrospectionException("Error when finding class with system ClassLoader ");
		}

		return convertedClass;
	}
}