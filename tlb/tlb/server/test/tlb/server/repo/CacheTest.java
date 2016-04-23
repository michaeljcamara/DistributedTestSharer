package tlb.server.repo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class CacheTest {

    private Cache<TestObject> testObjectCache;
    private TestObject cachedObject;

    final class TestObject implements Serializable {}

    @Before
    public void setUp() {
        testObjectCache = new Cache<TestObject>();
        cachedObject = new TestObject();
        testObjectCache.put("foo", cachedObject);
    }

    @Test
    public void shouldLoadObjectsPutIntoCache() {
        assertThat(testObjectCache.get("foo"), sameInstance(cachedObject));
    }

    @Test
    public void shouldRemoveObjectsOnRequest() {
        testObjectCache.remove("foo");
        assertThat(testObjectCache.get("foo"), nullValue());
    }
    
    @Test
    public void shouldListKeysSet() {
        List<String> keys = testObjectCache.keys();

        assertThat(keys.size(), is(1));
        assertThat(keys, hasItem("foo"));
    }

    @Test
    public void shouldClearAllKeys() {
        testObjectCache.clear();
        List<String> keys = testObjectCache.keys();

        assertThat(keys.size(), is(0));
        assertThat(testObjectCache.get("foo"), nullValue());
    }

}
