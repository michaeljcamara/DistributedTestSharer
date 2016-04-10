package server;

public class SimpleClassLoader extends ClassLoader {

	public Class convertToClass(byte[] bytes) {
		
//		return defineClass("CorrectnessTest", bytes, 0, bytes.length);
		return defineClass(bytes, 0, bytes.length);
	}
}
