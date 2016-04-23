package tlb.utils;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JunitExtRunner.class)
public class SystemEnvironmentTest {
    
    @Test
    public void shouldGetPropertyAvailableInGivenMap() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.EnvVar("foo")), is("bar"));
    }
    
    @Test
    public void shouldDefaultProperties() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        map.put("baz", "quux");
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.EnvVar("foo:baz")), is("bar"));
        assertThat(env.val(new SystemEnvironment.EnvVar("hello:baz")), is("quux"));
    }

    @Test
    public void shouldDefaultPropertiesWhileUsingLongPrefferedKeysChain_withRecursiveResolution() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        map.put("bar", "oo");
        map.put("baz", "baz-${foo}");
        map.put("quux", "quux_val-${f${bar}}");
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.EnvVar("hello:world:hello_world_key:foo : bar:quux:last_in_preference_key")), is("quux_val-bar"));
    }

    @Test
    public void shouldRecursivelyResolveVariables() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        map.put("bar", "oo");
        map.put("baz", "baz-${foo}");
        map.put("quux", "baz-${f${bar}}");
        map.put("complex", "${quux}|${q${bang}}");
        map.put("bang", "u${boom}");
        map.put("boom", "u${axe}");
        map.put("axe", "${X}");
        map.put("X", "x");
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.EnvVar("foo")), is("bar"));
        assertThat(env.val(new SystemEnvironment.EnvVar("baz")), is("baz-bar"));
        assertThat(env.val(new SystemEnvironment.EnvVar("quux")), is("baz-bar"));
        assertThat(env.val(new SystemEnvironment.EnvVar("complex")), is("baz-bar|baz-bar"));
    }

    @Test
    public void shouldNotFailForTemplateCharactersAppearingWhileResolvingVariables() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("fo$o", "ba${r");
        map.put("bar", "$o");
        map.put("baz", "baz-${fo${bar}}");
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.EnvVar("baz")), is("baz-ba${r"));
    }

    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.LINUX)
    public void shouldGetSystemEnvironmentVairableWhenNoMapPassed() throws Exception{
        SystemEnvironment env = new SystemEnvironment();
        assertThat(env.val(new SystemEnvironment.EnvVar("HOME")), is(System.getProperty("user.home")));
    }
    
    @Test
    public void shouldDefaultEnvVariableValues() {
        HashMap<String, String> map = new HashMap<String, String>();
        SystemEnvironment env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.DefaultedEnvVar("foo", "bar")), is("bar"));
        assertThat(env.val(new SystemEnvironment.EnvVar("foo")), is(nullValue()));
        map.put("foo", "baz");
        env = new SystemEnvironment(map);
        assertThat(env.val(new SystemEnvironment.DefaultedEnvVar("foo", "bar")), is("baz"));
        assertThat(env.val(new SystemEnvironment.EnvVar("foo")), is("baz"));
    }

    @Test
    public void shouldGenerateADigestOfVariablesSet() throws IOException {
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("foo", "bar");
        env.put("baz", "quux");
        SystemEnvironment sysEnv = new SystemEnvironment(env);

        assertThat(sysEnv.getDigest(), is(DigestUtils.md5Hex("baz:quux,foo:bar,".getBytes())));
    }

    @Test
    public void shouldHandleSystemEnvironmentWhileComputingDigest() throws IOException {//guards against serialization issues, System.getenv map is not serializable. This is to avoid regression.
        SystemEnvironment sysEnv = new SystemEnvironment();

        assertThat(sysEnv.getDigest(), not(nullValue()));
    }

    @Test
    public void shouldUnderstandTmpDir() {
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("foo", "bar");
        SystemEnvironment sysEnv = new SystemEnvironment(env);
        assertThat(sysEnv.tmpDir(), is(new File(System.getProperty("java.io.tmpdir") + "/" + sysEnv.getDigest()).getPath()));
    }
}
