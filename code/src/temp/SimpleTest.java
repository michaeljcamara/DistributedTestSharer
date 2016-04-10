package temp;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SimpleTest {

	@Test
	public void doSimpleTest() {
		
		SimpleResource a = new SimpleResource();
		SimpleResource b = a;
		
		assertEquals("Simple test", a, b);
	}
}
