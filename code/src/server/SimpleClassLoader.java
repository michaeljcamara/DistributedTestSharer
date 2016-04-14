package server;

public class SimpleClassLoader extends ClassLoader {

	public Class<?> convertToClass(byte[] bytes) {
		
//		return defineClass("CorrectnessTest", bytes, 0, bytes.length);
//		return defineClass("SimpleTest", bytes, 0, bytes.length);
//		return defineClass("temp.SimpleTest", ByteBuffer.wrap(bytes),null);
		return defineClass(bytes, 0, bytes.length);
		
	}
}
