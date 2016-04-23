package tlb.splitter.timebased;

import org.junit.Test;
import tlb.TlbSuiteFileImpl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BucketTest {

    @Test
    public void testShouldCompareBucketsBasedOnBucketWeight() {
        Bucket smaller = bucket(1, 1.0, 2.0);
        Bucket bigger = bucket(2, 1.0, 3.0);
        assertThat(smaller.compareTo(bigger), is(-1));
        assertThat(bigger.compareTo(smaller), is(1));
    }

    @Test
    public void testShouldCompareBucketsSizeIfBucketWeightsAreSame() {
        Bucket smaller = bucket(1, 1.0, 2.0);
        Bucket bigger = bucket(2, 1.0, 1.0, 1.0);
        assertThat(smaller.compareTo(bigger), is(-1));
        assertThat(bigger.compareTo(smaller), is(1));
    }

    @Test
    public void testShouldReturn0WhenWeightAndSizeAreSame() {
        Bucket smaller = bucket(1, 1.0, 2.0);
        Bucket bigger = bucket(2, 2.0, 1.0);
        assertThat(smaller.compareTo(bigger), is(0));
        assertThat(bigger.compareTo(smaller), is(0));
    }

    private Bucket bucket(int partition, double... times) {
        Bucket bucket = new Bucket(partition);
        for (double time : times) {
            String s = "foo" + time;
            bucket.add(new TestFile(new TlbSuiteFileImpl(s), time));
        }
        return bucket;
    }
}