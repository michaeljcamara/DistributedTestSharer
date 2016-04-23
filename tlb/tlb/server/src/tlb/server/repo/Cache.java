package tlb.server.repo;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.util.List;
import java.util.UUID;

/**
 * @understands caching against a string key
 */
public class Cache<T> {
    public static final CacheManager CACHE_MANAGER = CacheManager.create();
    private final net.sf.ehcache.Cache cache;

    public Cache() {
        this(1000);
    }

    public Cache(final int maxElementsInMemory) {
        cache = new net.sf.ehcache.Cache(
                new CacheConfiguration()
                        .name(UUID.randomUUID().toString())
                        .maxElementsInMemory(maxElementsInMemory)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                        .overflowToDisk(false)
                        .eternal(true)
                        .diskPersistent(false));
        CACHE_MANAGER.addCache(cache);
    }

    public void put(final String key, final T value) {
        cache.put(new Element(key, value));
    }

    public T get(final String key) {
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return (T) element.getValue();
    }

    public void remove(final String key) {
        cache.remove(key);
    }

    public List<String> keys() {
        return cache.getKeys();
    }

    public void clear() {
        cache.removeAll();
    }
}
