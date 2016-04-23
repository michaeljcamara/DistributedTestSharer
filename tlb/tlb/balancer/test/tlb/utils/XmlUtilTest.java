package tlb.utils;

import tlb.TestUtil;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import org.dom4j.Element;
import org.dom4j.DocumentFactory;
import static org.hamcrest.core.Is.is;

import java.util.List;
import java.util.HashMap;

public class XmlUtilTest {
    @Test
    public void shouldUnderstandsLoadingStringAsXML() throws Exception{
        Element element = XmlUtil.domFor("" + //to fool intelliJ and keep it from formating it idiotically
                "<foo>" +
                "  <bar>baz</bar>" +
                "  <baz>" +
                "    <quux>bang</quux>" +
                "  </baz>" +
                "</foo>");
        List barTexts = element.selectNodes("//bar/.");
        assertThat(barTexts.size(), is(1));
        assertThat(((Element) barTexts.get(0)).getText(), is("baz"));
    }

    @Test
    public void shouldUnderstandAtomFeedStringAsXML() throws Exception{
        String stageFeedPage = TestUtil.fileContents("resources/stages_p1.xml");
        Element element = XmlUtil.domFor(stageFeedPage);
        List entryIds = element.selectNodes("//a:entry/a:id");
        assertThat(entryIds.size(), is(6));
        assertThat(((Element) entryIds.get(0)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/25/stage-baz/1"));
        assertThat(((Element) entryIds.get(1)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/25/stage-bar/1"));
        assertThat(((Element) entryIds.get(2)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/24/stage-baz/1"));
        assertThat(((Element) entryIds.get(3)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/24/stage-bar/1"));
        assertThat(((Element) entryIds.get(4)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/23/stage-baz/1"));
        assertThat(((Element) entryIds.get(5)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/23/stage-bar/1"));
    }
    
    @Test
    public void shouldNotGetMessedUpWhenXmlNamespaceURL() throws Exception{
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("a", "http://foo.com/bar/baz");
        DocumentFactory.getInstance().setXPathNamespaceURIs(map);

        String stageFeedPage = TestUtil.fileContents("resources/stages_p1.xml");
        Element element = XmlUtil.domFor(stageFeedPage);
        List entryIds = element.selectNodes("//a:entry/a:id");
        assertThat(entryIds.size(), is(6));
        assertThat(((Element) entryIds.get(0)).getText(), is("http://test.host:8153/go/pipelines/pipeline-foo/25/stage-baz/1"));
    }
}
