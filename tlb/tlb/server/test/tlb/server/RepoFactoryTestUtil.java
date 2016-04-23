package tlb.server;

import tlb.server.repo.EntryRepo;

import java.io.IOException;
import java.io.StringWriter;

public class RepoFactoryTestUtil {
    public static String diskDump(EntryRepo repo) {
        StringWriter stringWriter = new StringWriter();
        try {
            repo.diskDumpTo(stringWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }
}
