package tlb.service;

import tlb.TlbSuiteFile;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.splitter.correctness.ValidationResult;

import java.util.List;

/**
 * @understands talking to an external service to get and post data
 */
public interface Server {
    //TODO: this is horrible api, make it accept SuiteTimeEntry
    void testClassTime(String className, long time);

    //TODO: this is horrible api, make it accept SuiteResultEntry
    void testClassFailure(String className, boolean hasFailed);

    List<SuiteTimeEntry> getLastRunTestTimes();

    List<SuiteResultEntry> getLastRunFailedTests();

    void publishSubsetSize(int size);

    int partitionNumber();

    int totalPartitions();

    ValidationResult validateUniversalSet(List<TlbSuiteFile> universalSet, String moduleName);

    ValidationResult validateSubSet(List<TlbSuiteFile> subSet, String moduleName);

    ValidationResult verifyAllPartitionsExecutedFor(String moduleName);

    String partitionIdentifier();
}
