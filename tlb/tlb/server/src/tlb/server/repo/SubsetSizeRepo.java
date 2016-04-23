package tlb.server.repo;

import tlb.domain.SubsetSizeEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @understands storage and retrival of size of subset of total suites run by job
 */
public class SubsetSizeRepo implements EntryRepo<SubsetSizeEntry, SubsetSizeRepo> {
    private volatile List<SubsetSizeEntry> entries;
    private volatile boolean dirty;
    private volatile String identifier;
    transient volatile protected EntryRepoFactory factory;

    public SubsetSizeRepo() {
        setEntries(new ArrayList<SubsetSizeEntry>());
    }

    private synchronized void setEntries(final List<SubsetSizeEntry> list) {
        entries = Collections.synchronizedList(list);
    }

    public Collection<SubsetSizeEntry> list() {
        return Collections.unmodifiableList(entries);
    }

    public Collection<SubsetSizeEntry> list(String version) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("versioning not allowed");
    }

    public void update(SubsetSizeEntry entry) {
        throw new UnsupportedOperationException("update not allowed on repository");
    }

    public void updateAll(List<SubsetSizeEntry> entry) {
        throw new UnsupportedOperationException("update not allowed on repository");
    }

    public synchronized void diskDumpTo(Writer writer) throws IOException {
        dumpTo(writer);
        dirty = false;
    }

    public synchronized String dump() throws IOException {
        StringWriter stringWriter = new StringWriter();
        dumpTo(stringWriter);
        return stringWriter.toString();
    }

    public void dumpTo(Writer writer) throws IOException {
        for (SubsetSizeEntry entry : entries) {
            writer.write(entry.dump());
        }
    }

    public synchronized void loadCopyFromDisk(Reader reader) throws IOException {
        dirty = false;
        load(reader);
    }

    private void load(Reader reader) throws IOException {
        LineNumberReader rdr = new LineNumberReader(reader);
        String line = null;
        entries.clear();
        while((line = rdr.readLine()) != null) {
            SubsetSizeEntry entry = parseLine(line);
            entries.add(entry);
        }
    }

    public void loadAndMarkDirty(Reader reader) throws IOException {
        load(reader);
        dirty = true;
    }

    public void copyFrom(SubsetSizeRepo otherRepo) {
        setEntries(new ArrayList<SubsetSizeEntry>(otherRepo.list()));
        dirty = true;
    }

    public synchronized void add(SubsetSizeEntry entry) {
        entries.add(entry);
        dirty = true;
    }

    public void setFactory(EntryRepoFactory factory) {
        this.factory = factory;
    }

    public boolean hasFactory() {
        return factory != null;
    }

    public void setNamespace(String namespace) {
        //doesn't need
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<SubsetSizeEntry> parse(String string) {
        return SubsetSizeEntry.parse(string);
    }

    public SubsetSizeEntry parseLine(String line) {
        return SubsetSizeEntry.parseSingleEntry(line);
    }

    public boolean isDirty() {
        return dirty;
    }
}
