package tlb.splitter.timebased;

import tlb.TlbSuiteFile;

/**
* @understands a test file as an element that needs to be balanced.
*/
public class TestFile implements Comparable<TestFile> {
    TlbSuiteFile fileName;
    Double time;

    public TestFile(TlbSuiteFile fileName, Double time) {
        this.fileName = fileName;
        this.time = time;
    }

    public int compareTo(TestFile o) {
        int i = o.time.compareTo(time);
        if (i == 0) {
            return o.fileName.getName().compareTo(this.fileName.getName());
        }
        return i;
    }
}
