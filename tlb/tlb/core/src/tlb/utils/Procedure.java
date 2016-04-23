package tlb.utils;

/**
 * @understands executing a block with given argument but without return
 */
public abstract class Procedure<T, E extends Exception> implements Function<T, E, Object> {
    public Object execute(T t) throws E {
        perform(t);
        return null;
    }

    public abstract void perform(T t) throws E;
}
