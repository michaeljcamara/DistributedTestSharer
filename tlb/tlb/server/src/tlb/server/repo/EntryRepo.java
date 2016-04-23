package tlb.server.repo;

import tlb.domain.Entry;

import java.io.*;
import java.util.Collection;
import java.util.List;

/**
 * @understands storage and retrieval of records 
 */
public interface EntryRepo<T extends Entry, Impl> extends Serializable {
    Collection<T> list();

    void update(T entry);

    void updateAll(List<T> entry);

    String dump() throws IOException;

    void copyFrom(Impl otherRepo);

    void add(T entry);

    void setFactory(EntryRepoFactory factory);

    boolean hasFactory();

    void setNamespace(String namespace);

    void setIdentifier(String type);

    String getIdentifier();

    List<T> parse(String string);

    T parseLine(String line);

    boolean isDirty();

    void diskDumpTo(Writer writer) throws IOException;

    void dumpTo(Writer writer) throws IOException;

    void loadCopyFromDisk(Reader reader) throws IOException;

    void loadAndMarkDirty(Reader reader) throws IOException;
}
