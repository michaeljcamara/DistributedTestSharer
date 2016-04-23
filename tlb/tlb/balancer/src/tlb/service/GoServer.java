package tlb.service;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import tlb.TlbConstants;

import static tlb.TlbConstants.*;

import tlb.TlbSuiteFile;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.service.http.HttpAction;
import tlb.service.http.DefaultHttpAction;
import tlb.splitter.correctness.ValidationResult;
import tlb.storage.TlbEntryRepository;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;
import tlb.utils.XmlUtil;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands requesting and posting information to/from cruise
 */
public class GoServer extends SmoothingServer {
    private static final Logger logger = Logger.getLogger(GoServer.class.getName());

    private final HttpAction httpAction;
    private static final String JOB_NAME = "name";
    protected static final String TEST_TIME_FILE = "tlb/test_time.properties";
    private static final Pattern STAGE_LOCATOR = Pattern.compile("(.*?)/\\d+/(.*?)/\\d+");
    final String jobLocator;
    final String stageLocator;
    public static final String FAILED_TESTS_FILE = "tlb/failed_tests";

    private static final String INT = "\\d+";
    private static final Pattern NUMBER_BASED_LOAD_BALANCED_JOB = Pattern.compile("(.*?)-(" + INT + ")");
    private static final String HEX = "[a-fA-F0-9]";
    private static final String UUID = HEX + "{8}-" + HEX + "{4}-" + HEX + "{4}-" + HEX + "{4}-" + HEX + "{12}";
    private static final Pattern UUID_BASED_LOAD_BALANCED_JOB = Pattern.compile("(.*?)-(" + UUID + ")");

    public GoServer(SystemEnvironment environment) {
        this(environment, createHttpAction(environment));
    }

    public GoServer(SystemEnvironment environment, HttpAction httpAction) {
        super(environment);
        this.httpAction = httpAction;
        jobLocator = String.format("%s/%s/%s/%s/%s", v(Go.GO_PIPELINE_NAME), v(Go.GO_PIPELINE_LABEL), v(Go.GO_STAGE_NAME), v(Go.GO_STAGE_COUNTER), v(Go.GO_JOB_NAME));
        FileUtil fileUtil = new FileUtil(environment);
        stageLocator = String.format("%s/%s/%s/%s", v(Go.GO_PIPELINE_NAME), v(Go.GO_PIPELINE_COUNTER), v(Go.GO_STAGE_NAME), v(Go.GO_STAGE_COUNTER));
    }

    private static URI createUri(SystemEnvironment environment) {
        try {
            return new URI(environment.val(new SystemEnvironment.EnvVar(Go.GO_SERVER_URL)));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpAction createHttpAction(SystemEnvironment environment) {
        return new DefaultHttpAction(createClient(environment), createContext(environment));
    }

    private static HttpContext createContext(SystemEnvironment environment) {
        AuthCache authCache = new BasicAuthCache();

        BasicScheme basicAuth = new BasicScheme();
        URI uri = createUri(environment);
        HttpHost host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        authCache.put(host, basicAuth);

        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute(ClientContext.AUTH_CACHE, authCache);

        return context;
    }

    private static DefaultHttpClient createClient(SystemEnvironment environment) {
        DefaultHttpClient client = DefaultHttpAction.createClient();
        URI uri = createUri(environment);
        if (environment.val(new SystemEnvironment.EnvVar(USERNAME)) != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(uri.getHost(), uri.getPort()), new UsernamePasswordCredentials(environment.val(new SystemEnvironment.EnvVar(USERNAME)), environment.val(new SystemEnvironment.EnvVar(PASSWORD))));
        }
        return client;
    }

    public List<String> getJobs() {
        ArrayList<String> jobNames = new ArrayList<String>();
        for (Attribute jobLink : jobLinks(String.format("%s/pipelines/%s.xml", cruiseUrl(), stageLocator))) {
            jobNames.add(rootFor(jobLink.getValue()).attributeValue(JOB_NAME));
        }
        logger.info(String.format("jobs found %s", jobNames));
        return jobNames;
    }

    @SuppressWarnings({"unchecked"})
    private List<Attribute> jobLinks(String url) {
        Element stage = rootFor(url);
        return (List<Attribute>) stage.selectNodes("//jobs/job/@href");
    }

    public Element rootFor(String url) {
        String xmlString = httpAction.get(url);
        return XmlUtil.domFor(xmlString);
    }

    private Object cruiseUrl() {
        String url = v(Go.GO_SERVER_URL);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String v(String key) {
        return environment.val(new SystemEnvironment.EnvVar(key));
    }

    @Override
    protected void postTestTimesToServer(String body) {
        postLinesToServer(body, artifactFileUrl(TEST_TIME_FILE));
    }

    private void postLinesToServer(String buffer, String url) {
        httpAction.put(url, buffer);
    }

    @Override
    protected void postFailedTestsToServer(List<SuiteResultEntry> resultEntries) {
        String failures = SuiteResultEntry.dumpFailures(resultEntries);
        postLinesToServer(failures, artifactFileUrl(FAILED_TESTS_FILE));
    }

    private String artifactFileUrl(String atrifactFile) {
        return String.format("%s/files/%s/%s", cruiseUrl(), jobLocator, atrifactFile);
    }

    public List<SuiteTimeEntry> fetchLastRunTestTimes() {
        return getLastRunTestTimes(pearJobs());
    }

    List<SuiteTimeEntry> getLastRunTestTimes(List<String> pearJobs) {
        return SuiteTimeEntry.parse(tlbArtifactPayloadLines(lastRunArtifactUrls(pearJobs, TEST_TIME_FILE)));
    }

    private List<String> lastRunArtifactUrls(List<String> jobNames, String urlSuffix) {
        String stageFeedUrl = String.format("%s/api/pipelines/%s/stages.xml", cruiseUrl(), v(Go.GO_PIPELINE_NAME));
        String stageDetailUrl = lastRunStageDetailUrl(stageFeedUrl);
        List<Attribute> jobLinks = jobLinks(stageDetailUrl);
        return tlbArtifactUrls(jobLinks, jobNames, urlSuffix);
    }

    private String tlbArtifactPayloadLines(List<String> tlbTestTimeUrls) {
        StringBuffer buffer = new StringBuffer();
        for (String tlbTestTimeUrl : tlbTestTimeUrls) {
            try {
                buffer.append(httpAction.get(tlbTestTimeUrl) + "\n");
            } catch (RuntimeException e) {
                continue; //FIXME!
            }
        }
        return buffer.toString();
    }

    private List<String> tlbArtifactUrls(List<Attribute> jobLinks, List<String> jobNames, String urlSuffix) {
        ArrayList<String> tlbAtrifactUrls = new ArrayList<String>();
        for (Attribute jobLink : jobLinks) {
            Element jobDom = rootFor(jobLink.getValue());
            String jobName = jobDom.attribute("name").getValue().trim();
            if (jobNames.contains(jobName)) {
                String atrifactBaseUrl = jobDom.selectSingleNode("//artifacts/@baseUri").getText();
                tlbAtrifactUrls.add(String.format("%s/%s", atrifactBaseUrl, urlSuffix));
            }
        }
        return tlbAtrifactUrls;
    }

    @SuppressWarnings({"unchecked"})
    private String lastRunStageDetailUrl(String stageFeedUrl) {
        return findLastRunStageDetailUrl(stageFeedUrl, Integer.parseInt(environment.val(TlbConstants.Go.GO_STAGE_FEED_MAX_SEARCH_DEPTH)), 0);
    }

    private String findLastRunStageDetailUrl(String stageFeedUrl, int digNoMoreThan, int current) {
        if (digNoMoreThan == current) {
            throw new IllegalStateException(String.format("Couldn't find a historical run for stage in '%s' pages of stage feed.", digNoMoreThan));
        }
        Element stageFeedPage = rootFor(stageFeedUrl);
        List<Element> list = stageFeedPage.selectNodes("//a:entry");
        for (Element element : list) {
            String stageLocator = element.selectSingleNode("./a:title").getText();
            if (sameStage(stageLocator)) {
                return element.selectSingleNode("./a:link/@href").getText();
            }
        }
        return findLastRunStageDetailUrl(stageFeedPage.selectSingleNode("//a:link[@rel='next']/@href").getText(), digNoMoreThan, ++current);
    }

    private boolean sameStage(String stageLocator) {
        Matcher matcher = STAGE_LOCATOR.matcher(stageLocator);
        if (!matcher.matches()) {
            return false;
        }
        boolean samePipeline = environment.val(new SystemEnvironment.EnvVar(Go.GO_PIPELINE_NAME)).equals(matcher.group(1));
        boolean sameStage = environment.val(new SystemEnvironment.EnvVar(Go.GO_STAGE_NAME)).equals(matcher.group(2));
        return samePipeline && sameStage;
    }

    @Override
    protected void postSubsetSizeToServer(int size) {
        httpAction.put(artifactFileUrl(TlbConstants.TEST_SUBSET_SIZE_FILE), String.format("%s\n", size));
    }

    List<SuiteResultEntry> getLastRunFailedTests(List<String> jobNames) {
        String failedTests = null;
        try {
            failedTests = tlbArtifactPayloadLines(lastRunArtifactUrls(jobNames, FAILED_TESTS_FILE));
        } catch (Exception e) {
            logger.warn("Couldn't find tests that failed in the last run", e);
            failedTests = "";
        }
        return SuiteResultEntry.parseFailures(failedTests);
    }

    public List<SuiteResultEntry> getLastRunFailedTests() {
        return getLastRunFailedTests(pearJobs());
    }

    protected String jobName() {
        return environment.val(new SystemEnvironment.EnvVar(Go.GO_JOB_NAME));
    }

    private String jobBaseName() {
        Matcher matcher = NUMBER_BASED_LOAD_BALANCED_JOB.matcher(jobName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = UUID_BASED_LOAD_BALANCED_JOB.matcher(jobName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return jobName();
    }

    private Pattern getMatcher() {
        return Pattern.compile(String.format("^%s-(" + INT + "|" + UUID + ")$", jobBaseName()));
    }

    private List<String> jobsInTheSameFamily(List<String> jobs) {
        List<String> family = new ArrayList<String>();
        Pattern pattern = getMatcher();
        for (String job : jobs) {
            if (pattern.matcher(job).matches()) {
                family.add(job);
            }
        }
        return family;
    }

    public List<String> pearJobs() {
        List<String> jobs = jobsInTheSameFamily(getJobs());
        Collections.sort(jobs);
        return jobs;
    }

    public int partitionNumber() {
        return pearJobs().indexOf(jobName()) + 1; //partition number is one based
    }

    public int totalPartitions() {
        return pearJobs().size();
    }

    public ValidationResult validateUniversalSet(List<TlbSuiteFile> universalSet, String moduleName) {
        throw new UnsupportedOperationException("Correctness-check feature is only available when working against TLB server. Go server support does not include correctness-checking yet.");
    }

    public ValidationResult validateSubSet(List<TlbSuiteFile> subSet, String moduleName) {
        throw new UnsupportedOperationException("Correctness-check feature is only available when working against TLB server. Go server support does not include correctness-checking yet.");
    }

    public ValidationResult verifyAllPartitionsExecutedFor(String moduleName) {
        throw new UnsupportedOperationException("Correctness-check feature is only available when working against TLB server. Go server support does not include correctness-checking yet.");
    }

    public String partitionIdentifier() {
        return String.format("job: '%s', partition: %s/%s", jobName(), partitionNumber(), totalPartitions());
    }
}
