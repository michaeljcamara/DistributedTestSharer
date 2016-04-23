package tlb.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.util.logging.Level;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class FileUtil {
    private SystemEnvironment env;
    public static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    public FileUtil(SystemEnvironment env) {
        this.env = env;
    }

    public String tmpDir() {
        File tmpDir = new File(env.tmpDir());
        createDirIfNecessary(tmpDir);
        return tmpDir.getAbsolutePath();
    }

    private void createDirIfNecessary(final File tmpDirectory) {
        final String tmpDir = tmpDirectory.getAbsolutePath();
        logger.info(String.format("checking for existance of directory %s as tlb tmpdir", tmpDir));
        if (tmpDirectory.exists()) {
            if (! tmpDirectory.isDirectory()) {
                String fileInsteedOfDirectoryMessage = String.format("tlb tmp dir %s is a file, it must be a directory", tmpDir);
                logger.warn(fileInsteedOfDirectoryMessage);
                throw new IllegalStateException(fileInsteedOfDirectoryMessage);
            }
            logger.info(String.format("directory %s exists, creation not required", tmpDir));
        } else {
            logger.info(String.format("directory %s doesn't exist, creating it now", tmpDir));
            try {
                FileUtils.forceMkdir(tmpDirectory);
            } catch (IOException e) {
                logger.warn(String.format("could not create directory %s", tmpDirectory.getAbsolutePath()), e);
                throw new RuntimeException(e);
            }
            logger.info(String.format("created directory %s, which is to be used as tlb tmp dir.", tmpDir));
        }
    }
    public String classFileRelativePath(String testClass) {
        return testClass.replaceAll("\\.", "/") + ".class";
    }

    public File getUniqueFile(String seedString) {
        String fileName = DigestUtils.md5Hex(seedString);
        File file = new File(tmpDir(), fileName);
        logger.info(String.format("unique file name %s translated to %s", seedString, file.getAbsolutePath()));
        return file;
    }

    public static List<File> toFileList(Iterator<File> reports) {
        List<File> foo = new ArrayList<File>();
        while (reports.hasNext()) {
            foo.add(reports.next());
        }
        return foo;
    }

    public static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf(".") == -1 ? fileName.length() : fileName.lastIndexOf(".");
        return fileName.substring(0, index);
    }

    public static String readIntoString(BufferedReader bufferedReader) {
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }
}
