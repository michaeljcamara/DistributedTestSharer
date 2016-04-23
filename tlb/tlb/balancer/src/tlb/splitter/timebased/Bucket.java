package tlb.splitter.timebased;

import tlb.TlbSuiteFile;

import java.util.ArrayList;
import java.util.List;

/**
* @understands a bucket which needs to be filled up equally
*/
public class Bucket implements Comparable<Bucket> {

    int partition;
    Double time = 0.0;
    List<TlbSuiteFile> files = new ArrayList<TlbSuiteFile>();

    public Bucket(int partition) {
        this.partition = partition;
    }

    public int compareTo(Bucket o) {
        int i = time.compareTo(o.time);
        if (i == 0) {
            return new Integer(files.size()).compareTo(o.files.size());
        }
        return i;
    }

    public void add(TestFile testFile) {
        files.add(testFile.fileName);
        time += testFile.time;
    }

    public List<TlbSuiteFile> files() {
        return files;
    }

    public int index() {
        return partition;
    }
}
