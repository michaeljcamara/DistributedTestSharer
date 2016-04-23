package tlb.storage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import tlb.domain.Entry;
import tlb.utils.Function;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * @understands storing and retrieving entries on the balancer
 */
public class TlbEntryRepository {
    private static final Logger logger = Logger.getLogger(TlbEntryRepository.class.getName());
    private final File file;

    public TlbEntryRepository(final File file) {
        this.file = file;
    }

    public void appendLine(String line) {
        performWrite(new Write<String>(line) {
            @Override
            byte[] getBytes(String line) {
                return line.getBytes();
            }
        });
    }

    private void performWrite(Write writeOp) {
        File cacheFile = getFile();
        if (! file.exists()) {
            file.getParentFile().mkdirs();
        }
        RandomAccessFile rw = null;
        try {
            synchronized (lock(cacheFile)) {
                try {
                    rw = new RandomAccessFile(cacheFile, "rw");
                    writeOp.invoke(rw);
                } finally {
                    if (rw != null) {
                        rw.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (logger.isDebugEnabled()) {
            int length = writeOp.lines.length;
            if (length > 0) {
                logger.debug(length > 1 ? String.format("Wrote %s lines with first line [ %s ] and last line [ %s ] to %s", length, writeOp.lines[0], writeOp.lines[length - 1], cacheFile.getAbsolutePath()) : String.format("Wrote line [ %s ] to %s", writeOp.lines[0], cacheFile.getAbsolutePath()));
            }
        }
    }

    private byte[] makeHeader(int lineCountValue) {
        return new byte[] {
                (byte) ((lineCountValue >> 24) & 0xFF),
                (byte) ((lineCountValue >> 16) & 0xFF),
                (byte) ((lineCountValue >> 8) & 0xFF),
                (byte) (lineCountValue & 0xFF)
        };
    }

    public <T> T loadContent(Function<FileInputStream, IOException, T> fn) {
        File cacheFile = getFile();
        FileInputStream in = null;
        T content = null;
        if (!cacheFile.exists()) {
            try {
                return fn.execute(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            synchronized (lock(cacheFile)) {
                in = new FileInputStream(cacheFile);
                byte[] linesHeader = new byte[4];
                in.read(linesHeader);
                content = fn.execute(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return content;
    }

    public List<String> loadLines() {
        List<String> lines = loadContent(new Function<FileInputStream, IOException, List<String>>() {
            public List<String> execute(FileInputStream fileInputStream) throws IOException {
                if (fileInputStream == null) {
                    return new ArrayList<String>();
                }
                return IOUtils.readLines(fileInputStream);
            }
        });
        logger.info(String.format("Cached %s lines from %s, the last of which was [ %s ]", lines.size(), getFile().getAbsolutePath(), lastLine(lines)));
        return lines;
    }

    public String loadBody() {
        return loadContent(new Function<FileInputStream, IOException, String>() {
            public String execute(FileInputStream fileInputStream) throws IOException {
               return IOUtils.toString(fileInputStream);
            }
        });
    }

    public File getFile() {
        return file;
    }

    public void cleanup() throws IOException {
        if (exists()) {
            FileUtils.forceDelete(getFile());
        }
    }

    public boolean exists() {
        return getFile().exists();
    }

    public String loadLastLine() {
        return lastLine(loadLines());
    }

    private String lastLine(List<String> lines) {
        return lines.size() > 0 ? lines.get(lines.size() - 1) : null;
    }

    public int lineCount() {
        File cacheFile = getFile();
        RandomAccessFile in = null;
        if (!cacheFile.exists() || cacheFile.length() == 0) {
            return 0;
        }
        try {
            synchronized (lock(cacheFile)) {
                try {
                        in = new RandomAccessFile(cacheFile, "r");
                        return getLineCount(in);
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                    }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String lock(File cacheFile) {
        return cacheFile.getAbsolutePath().intern();
    }

    private int getLineCount(DataInput in) throws IOException {
        byte[] linesHeader = new byte[4];
        in.readFully(linesHeader);
        return (linesHeader[3] +
                (((int) linesHeader[2]) << 8) +
                (((int) linesHeader[1]) << 16) +
                (((int) linesHeader[0]) << 24));
    }

    public void appendLines(List<? extends Entry> entries) {
        performWrite(new Write<Entry>(entries.toArray(new Entry[0])) {
            @Override
            byte[] getBytes(Entry line) {
                return line.dump().getBytes();
            }
        });
    }

    public abstract class Write<T> {
        private T[] lines;

        public Write(T... lines) {
            this.lines = lines;
        }

        public void invoke(RandomAccessFile file) throws IOException {
            file.write(makeHeader(lineCount() + lines.length));
            long length = file.length();
            file.seek(length);
            for (T line : lines) {
                file.write(getBytes(line));
            }
        }

        abstract byte[] getBytes(T line);
    }
}
