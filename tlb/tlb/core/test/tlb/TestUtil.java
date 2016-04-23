package tlb;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class TestUtil {
    private static final int MIN_ANONYMOUS_PORT = 1024;
    private static final int MAX_PORT_NUMBER = 65536;

    public static String fileContents(String filePath) throws IOException, URISyntaxException {
        return FileUtils.readFileToString(new File(TestUtil.class.getClassLoader().getResource(filePath).toURI()));
    }

    public static String deref(InputStream inputStream) throws IOException {
        return IOUtils.readLines(inputStream).toString();
    }

    public static File createFileInFolder(File folder, String fileName) {
        File file = new File(folder, fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        file.deleteOnExit();
        return file;
    }

    public static void updateEnv(SystemEnvironment env, String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Field variables = SystemEnvironment.class.getDeclaredField("variables");
        variables.setAccessible(true);
        Map<String, String> map = (Map<String, String>) variables.get(env);
        map.put(key, value);
        Field digest = SystemEnvironment.class.getDeclaredField("digest");
        digest.setAccessible(true);
        digest.set(env, null);
    }

    public static File mkdirInPwd(String dirName) {
        return mkdirIn(".", dirName);
    }

    public static File mkdirIn(String parent, String dirName) {
        File file = new File(parent, dirName);
        file.mkdirs();
        return file;
    }

    public static String findFreePort() {
        Random random = new Random(System.currentTimeMillis());
        for(int i = 0; i < 10; i++) {
            System.err.println("Attempting to find a free port...");
            int port = MIN_ANONYMOUS_PORT + random.nextInt(MAX_PORT_NUMBER - MIN_ANONYMOUS_PORT);
            System.err.println("Checking port number = " + port);
            try {
                new Socket("localhost", port);
                System.err.println("Busy on port number = " + port);
            } catch (IOException e) {
                System.err.println("Using port = " + port);
                return String.valueOf(port);
            }
        }
        throw new IllegalStateException("Failed to find a free port");
    }

    public static String convertToPlatformSpecificPath(String classRelPath) {
        return new File(classRelPath).getPath();
    }

    public static class LogFixture extends AppenderSkeleton {

        public void startListening() {
            startListening(Level.DEBUG);
        }

        public void startListening(Level level) {
            this.activateOptions();
            setLevel(level);
            Logger.getRootLogger().addAppender(this);
        }

        public void stopListening() {
            Logger.getRootLogger().removeAppender(this);
        }

//        public static Level getLevel() {
//            return Logger.getRootLogger().getLevel();
//        }

//        public static void enableDebug() {
//            setLevel(Level.DEBUG);
//        }

        public static void setLevel(Level level) {
            Logger.getRootLogger().setLevel(level);
        }

        private List<LoggingEvent> loggingEvents = new ArrayList<LoggingEvent>();

        public LogFixture() {
        }

        protected void append(LoggingEvent event) {
            loggingEvents.add(event);
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return false;
        }

        public List<String> messages() {
            ArrayList<String> messages = new ArrayList<String>();
            for (LoggingEvent loggingEvent : loggingEvents) {
                messages.add(loggingEvent.getRenderedMessage());
            }
            return messages;
        }

        public boolean contains(Level level, String message) {
            for (LoggingEvent event : loggingEvents) {
                if (event.getLevel().equals(level) && event.getMessage().toString().contains(message)) {
                    return true;
                }
            }
            return false;
        }

        public void assertHeard(String message) {
            assertHeard(message, 1);
        }

        public void clearHistory() {
            loggingEvents.clear();
        }

        public void assertHeard(String partOfMessage, int expectedOccurances) {
            int actualOccurances = totalOccurances(partOfMessage);
            assertThat(String.format("log message '%s' should have been heard %s times, but was actually heard %s times in %s statements %s", partOfMessage, expectedOccurances, actualOccurances, loggingEvents.size(), messages()), actualOccurances, is(expectedOccurances));
        }

        private int totalOccurances(String partOfMessage) {
            int numberOfOccurances = 0;
            for (LoggingEvent evt : loggingEvents) {
                if (evt.getRenderedMessage().contains(partOfMessage)) numberOfOccurances++;
            }
            return numberOfOccurances;
        }


        public void assertHeardException(Throwable expected) {
            boolean matched = false;
            for (LoggingEvent evt : loggingEvents) {
                ThrowableInformation throwableInformation = evt.getThrowableInformation();
                if (throwableInformation == null) continue;
                matched = match(throwableInformation.getThrowable(), expected);
                if (matched) break;
            }
            assertTrue(String.format("didnt find exception %s in heard throwables", expected), matched);
        }

        private boolean match(Throwable actual, Throwable expected) {
            if (actual == null) {
                return expected == null;
            }
            return expected.getClass().equals(actual.getClass()) && actual.getMessage().equals(expected.getMessage()) && match(actual.getCause(), expected.getCause());
        }

        public void assertNotHeard(String partOfMessage) {
            int actualOccurances = totalOccurances(partOfMessage);
            assertThat(String.format("log message %s should NOT have been heard at all, but was actually heard %s times in %s statements %s", partOfMessage, actualOccurances, loggingEvents.size(), messages()), actualOccurances, is(0));
        }
    }

    public static SystemEnvironment initEnvironment(String jobName) {
        Map<String, String> map = initEnvMap(jobName);
        return new SystemEnvironment(map);
    }

    private static Map<String, String> initEnvMap(String jobName) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.TlbServer.TLB_JOB_NAME, jobName);
        map.put(TlbConstants.TlbServer.TLB_JOB_VERSION, "build_no_19");
        return map;
    }

    public static SystemEnvironment initEnvironment(String jobName, int partitionNumber, int totalPartitions) {
        Map<String, String> env = initEnvMap(jobName);
        env.put(TlbConstants.TlbServer.TLB_PARTITION_NUMBER, String.valueOf(partitionNumber));
        env.put(TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS, String.valueOf(totalPartitions));
        return new SystemEnvironment(env);
    }


    public static File createTmpDir() {
        String tmpDir = System.getProperty("test.tmp.dir", System.getProperty(SystemEnvironment.TMP_DIR));
        final File file = new File(tmpDir, UUID.randomUUID().toString());
        file.mkdirs();
        try {
            FileUtils.forceDeleteOnExit(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + file, e);
        }
        return file;
    }

    public static Object invoke(final String methodName, final Object fromObject, final Object... args) throws IllegalAccessException {
        final Class[] types = types(args);
        return getSth(fromObject.getClass(), new Getter<Method>() {
            public Object eval(Method method) throws InvocationTargetException, IllegalAccessException {
                return method.invoke(fromObject, args);
            }

            public Method find(Class klass) throws NoSuchMethodException {
                return klass.getDeclaredMethod(methodName, types);
            }
        });
    }

    private static Class[] types(Object[] args) {
        final Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        return types;
    }

    public static Object deref(final String fieldName, final Object fromObject) throws IllegalAccessException {
        return getSth(fromObject.getClass(), new Getter<Field>() {
            public Object eval(Field field) throws IllegalAccessException {
                return field.get(fromObject);
            }

            public Field find(Class klass) throws NoSuchFieldException {
                return klass.getDeclaredField(fieldName);
            }
        });
    }

    private static Object getSth(Class klass, Getter getter) {
        try {
            AccessibleObject accessibleObject = getter.find(klass);
            accessibleObject.setAccessible(true);
            return getter.eval(accessibleObject);
        } catch (Exception e) {
            if (klass.equals(Object.class)) {
                throw new RuntimeException("Matching object could not be found in any of the classes in the hirarchy", e);
            }
            Class superKlass = klass.getSuperclass();
            return getSth(superKlass, getter);
        }
    }

    private static interface Getter<T extends AccessibleObject> {
        Object eval(T t) throws Exception;
        T find(Class klass) throws Exception;
    }

}
