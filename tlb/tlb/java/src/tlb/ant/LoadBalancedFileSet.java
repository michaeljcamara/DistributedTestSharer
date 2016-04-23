package tlb.ant;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import tlb.TlbConstants;
import tlb.TlbFileResource;
import tlb.TlbSuiteFile;
import tlb.factory.TlbBalancerFactory;
import tlb.orderer.TestOrderer;
import tlb.splitter.AbstractTestSplitter;
import tlb.splitter.TestSplitter;
import tlb.utils.SuiteFileConvertor;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @understands splitting Junit test classes into groups
 */
public class LoadBalancedFileSet extends FileSet {
    private static final Logger logger = Logger.getLogger(LoadBalancedFileSet.class.getName());

    private final TestSplitter criteria;
    private final TestOrderer orderer;
    private String moduleName = TlbConstants.Balancer.DEFAULT_MODULE_NAME;

    public LoadBalancedFileSet(TestSplitter criteria, TestOrderer orderer) {
        this.criteria = criteria;
        this.orderer = orderer;
    }

    public LoadBalancedFileSet(SystemEnvironment systemEnvironment) {
        this(TlbBalancerFactory.getCriteria(systemEnvironment.val(AbstractTestSplitter.TLB_SPLITTER), systemEnvironment),
                TlbBalancerFactory.getOrderer(systemEnvironment.val(TestOrderer.TLB_ORDERER), systemEnvironment));
    }

    public LoadBalancedFileSet() {//used by ant
        this(new SystemEnvironment());
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Iterator iterator() {
        Iterator<FileResource> files = (Iterator<FileResource>) super.iterator();
        List<TlbFileResource> matchedFiles = new ArrayList<TlbFileResource>();
        while (files.hasNext()) {
            FileResource fileResource = files.next();
            matchedFiles.add(new JunitFileResource(fileResource));
        }

        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        List<TlbSuiteFile> suiteFiles = convertor.toTlbSuiteFiles(matchedFiles);

        if (logger.isInfoEnabled()) {
            logger.info("About to filter tests");
        }
        suiteFiles = criteria.filterSuites(suiteFiles, moduleName);
        if (logger.isInfoEnabled()) {
            logger.info("Done filtering. About to order tests.");
        }
        Collections.sort(suiteFiles, orderer);
        logger.info("Done ordering.");
        List<TlbFileResource> matchedTlbFileResources = convertor.toTlbFileResources(suiteFiles);

        List<FileResource> matchedFileResources = new ArrayList<FileResource>();
        for (TlbFileResource matchedTlbFileResource : matchedTlbFileResources) {
            JunitFileResource fileResource = (JunitFileResource) matchedTlbFileResource;
            matchedFileResources.add(fileResource.getFileResource());
        }
        return matchedFileResources.iterator();
    }

    public TestSplitter getSplitterCriteria() {
        return criteria;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
