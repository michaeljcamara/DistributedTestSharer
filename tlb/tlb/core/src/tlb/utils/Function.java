package tlb.utils;

/**
 * @understands executing a block with given argument and return
 */
public interface Function<T, E extends Exception, R> {
    R execute(T t) throws E;
}
