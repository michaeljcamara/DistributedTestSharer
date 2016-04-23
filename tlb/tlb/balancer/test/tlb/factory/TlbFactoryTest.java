package tlb.factory;

import org.junit.Test;
import tlb.utils.SystemEnvironment;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class TlbFactoryTest {
    @Test
    public void shouldThrowExceptionIfClassByGivenNameNotAssignableToInterfaceFactoryIsCreatedWith() {
        final TlbFactory<FooBarable> factory = new TlbFactory<FooBarable>(FooBarable.class, null);
        try {
            factory.getInstance("java.lang.String", new SystemEnvironment());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Class 'java.lang.String' is-not/does-not-implement 'class tlb.factory.FooBarable'"));
        }
    }
    
    @Test
    public void shouldUseConstructorWithCorrectSetOfArguments() {
        final TlbFactory<FooBarable> factory = new TlbFactory<FooBarable>(FooBarable.class, null, SystemEnvironment.class, String.class, Integer.class);
        FooBarable obj = factory.getInstance(FooBarable.class.getCanonicalName(), new SystemEnvironment(), new SystemEnvironment(), "string-foo", 100);
        assertThat(obj.usingConstructor, is("string and int"));
        assertThat(obj.string, is("string-foo"));
        assertThat(obj.integer, is(100));
    }

    @Test
    public void shouldUseConstructorWithCorrectSetOfArguments_whenNoArgsGiven() {
        final TlbFactory<FooBarable> factory = new TlbFactory<FooBarable>(FooBarable.class, null, SystemEnvironment.class);
        FooBarable obj = factory.getInstance(FooBarable.class.getCanonicalName(), new SystemEnvironment(), new SystemEnvironment());
        assertThat(obj.usingConstructor, is("env only"));
        assertThat(obj.string, is(nullValue()));
        assertThat(obj.integer, is(nullValue()));
    }
}
