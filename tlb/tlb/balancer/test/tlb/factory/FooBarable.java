package tlb.factory;

import tlb.utils.SystemEnvironment;

public class FooBarable {
    public final String usingConstructor;
    public String string;
    public Integer integer;

    public FooBarable(SystemEnvironment env) {
        usingConstructor = "env only";
    }

    public FooBarable(SystemEnvironment env, String string, Integer integer) {
        this.string = string;
        this.integer = integer;
        usingConstructor = "string and int";
    }
}
