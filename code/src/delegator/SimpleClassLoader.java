package delegator;

public class SimpleClassLoader extends ClassLoader {

	/** This method accesses the protected defineClass() method, allowing it
	 * to be used by other classes.  It will convert an array of bytes to a
	 * class object
	 */
	public Class<?> convertToClass(byte[] bytes) {
		return defineClass(bytes, 0, bytes.length);

	}
}
