package server;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.SecureClassLoader;

public class CustomClassLoader extends URLClassLoader{

	public CustomClassLoader(ClassLoader l) throws IOException  {

		// TODO: Need to create relative paths to bin/lib for servers
		// See if possible to use single URL to select multiple jars (without strict naming)
//		super(new URL[]{
//				CustomClassLoader.class.getProtectionDomain().getCodeSource().getLocation(),
////				new URL("file://C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/bin/"),
//				new URL("file:C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/lib/commons-net-3.4.jar"),
//				new URL("file:C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/lib/junit-4.12.jar"),
//				new URL("file:C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/lib/hamcrest-core-1.3.jar"),
//				}, l.getParent());
		
		super(getNestedURLs(), l.getParent());
		
	}
	
	
	private static URL[] getNestedURLs() {
		
		String binDirString = CustomClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String libDirString = binDirString.substring(0, binDirString.length()-4) + "lib/";
		
		File libDirFile = new File(libDirString);
		File[] jarFiles = libDirFile.listFiles();
		URL[] urls = new URL[jarFiles.length + 1];
		try {
			urls[0] = new URL("file://" + binDirString);

			for(int i = 1; i < urls.length; i++) {
				urls[i] = new URL(jarFiles[i-1].toURI().toString());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return urls;
	}
	
//	public void appendURL(URL url) {
//		addURL(url);
//	}
	
	public CustomClassLoader(URL[] urls) {
		super(urls);
	}
	
	//http://baptiste-wicht.com/posts/2010/05/tip-add-resources-dynamically-to-a-classloader.html
	public static void addURLToSystemClassLoader(URL url) throws IntrospectionException { 
		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader(); 
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class; 

		try { 
			Method method = classLoaderClass.getDeclaredMethod("addURL", new Class[]{URL.class}); 
			method.setAccessible(true); 
			method.invoke(systemClassLoader, new Object[]{url}); 
		} catch (Throwable t) { 
			t.printStackTrace(); 
			throw new IntrospectionException("Error when adding url to system ClassLoader "); 
		} 
	}
	
	public static Class findClassWithSystemClassLoader(String name) throws IntrospectionException { 
		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class; 
		
		Class convertedClass = null;
		
		for(Method m : classLoaderClass.getDeclaredMethods()) {
			System.out.print(m.getName() + " ");
			
			for(Class c : m.getParameterTypes()) {
				System.out.print(c.getName() + " ");
			}
			
			System.out.println(", return: " + m.getReturnType().getName());
			
		}

		try { 
			Method method = classLoaderClass.getDeclaredMethod("findClass", new Class[]{String.class}); 
			method.setAccessible(true); 
			convertedClass = (Class) method.invoke(systemClassLoader, new Object[]{name}); 
		} catch (Throwable t) { 
			t.printStackTrace(); 
			throw new IntrospectionException("Error when finding class with system ClassLoader "); 
		} 
		
		
		
		return convertedClass;
	}
	
//	public static Class convertToClass(String name, byte[] bytes, int off, int len, CodeSource source) throws IntrospectionException {
//		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
////		ClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader(); 
////		Class<URLClassLoader> classLoaderClass = URLClassLoader.class; 
//		Class<SecureClassLoader> classLoaderClass = SecureClassLoader.class;
////		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
//		Class convertedClass = null;
//		
//		try { 
//			for(Method m : classLoaderClass.getDeclaredMethods()) {
//				System.out.print(m.getName() + " ");
//				
//				for(Class c : m.getParameterTypes()) {
//					System.out.print(c.getName() + " ");
//				}
//				
//				System.out.println(", return: " + m.getReturnType().getName());
//				
//			}
//			Method method = classLoaderClass.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class, CodeSource.class}); 
////			Method method = classLoaderClass.getMethod("defineClass", new Class[]{String.class, bytes.getClass(), int.class, int.class});
//			method.setAccessible(true); 
////			convertedClass = (Class) method.invoke(systemClassLoader, new Object[]{"asd", bytes, off, len}); 
//			convertedClass = (Class) method.invoke(systemClassLoader, new Object[]{name, bytes, off, len, source});
//		} catch (Throwable t) { 
//			t.printStackTrace(); 
//			throw new IntrospectionException("Error when converting bytes to class"); 
//		} 
//		
//		return convertedClass;
//	}

	
//	private static URL[] getUrls() throws IOException {
//		FTPClient client = new FTPClient();
//		client.connect(InetAddress.getByName("127.0.0.1"), 81);
//		client.login("user","user");
//		
//		return new URL[]{new URL("ftp://127.0.0.1:81/")};
//	}

//	public CustomClassLoader(ClassLoader launcherClassLoader) {
//		super(getUrls(launcherClassLoader), launcherClassLoader.getParent());
//    }
//
//    private static URL[] getUrls(ClassLoader cl) {
//        System.out.println("---MyLoader--- inside #constructor(" + cl + ")...");
//        return ((URLClassLoader) cl).getURLs();
//    }
//
    @Override public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        System.out.println("---MyLoader--- inside loadClass(" + name + ", " + resolve + ")...");
        return super.loadClass(name, resolve);
    }
	
//	public CustomClassLoader(URL[] urls) throws MalformedURLException {
//		
//		super(urls,);
////		super(urls);
////		super(new URL[]{new URL("ftp://127.0.0.1:81/")});
////		URL[] urls = new URL[]{new URL("ftp://127.0.0.1:81/")};
//////		URLClassLoader loader = new URLClassLoader({new URL("ftp://127.0.0.1:81/")}));
////		URLClassLoader loader = new URLClassLoader(urls);
//	}
	
	
//	public Class convertToClass(byte[] bytes) {
//		
////		return defineClass("CorrectnessTest", bytes, 0, bytes.length);
//		return defineClass(bytes, 0, bytes.length);
//	}
//	
//	
//	public URL getResource(String name) {
//		return findResource(name);
//	}
//	
//	public Class getClass(String name) throws ClassNotFoundException {
//		return findClass(name);
//	}
//	
//	public Class getLoadedClass(String name) {
//		return findLoadedClass(name);
//	}
	
    public Class<?> convertToClass(byte[] bytes) {
		return defineClass(bytes, 0, bytes.length);
		
	}

}
