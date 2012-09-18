package org.archive.modules.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.StringExtractorTestBase.TestData;
import org.archive.modules.fetcher.UserAgentProvider;
import org.archive.net.LaxURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.Recorder;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExternalBrowserExtractorHtmlTest extends StringExtractorTestBase {

	final public static String[] VALID_TEST_DATA = new String[] {
		"{ \"tagName\": \"XMLHttpRequest\",\"url\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"=BROWSER_MISC\"}", //default embed
		"http://www.slashdot.org",
		"{ \"tagName\": \"img\",\"src\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"img/@src\"}", //img src
		"http://www.slashdot.org",
		"{ \"tagName\": \"div\",\"background\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"div/@background\"}", //div background
		"http://www.slashdot.org",
		"{ \"tagName\": \"div\",\"cite\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"div/@cite\"}", //div cite
		"http://www.slashdot.org",
		"{ \"tagName\": \"img\",\"longdesc\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"img/@longdesc\"}", //img longdesc
		"http://www.slashdot.org",
		"{ \"tagName\": \"img\",\"usemap\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"img/@usemap\"}", //img usemap
		"http://www.slashdot.org",
		"{ \"tagName\": \"img\",\"profile\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"img/@profile\"}", //img profile
		"http://www.slashdot.org",
		"{ \"tagName\": \"a\",\"datasrc\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"a/@datasrc\"}", //a datasrc
		"http://www.slashdot.org",
		
		"{ \"tagName\": \"a\",\"href\": \"http://www.slashdot.org/\", \"hop\":\"NAVLINK\", \"context\":\"a/@href\"}", //a href
		"http://www.slashdot.org",
		"{ \"tagName\": \"link\",\"href\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"link/@href\"}", //link href
		"http://www.slashdot.org",
		
		"{ \"tagName\": \"XMLHttpRequest\",\"url\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"=BROWSER_MISC\"}", //XMLHttpRequest url
		"http://www.slashdot.org",
		
		"{ \"tagName\": \"style\",\"innerText\": \"background: url('http://www.slashdot.org/');\", \"hop\":\"EMBED\", \"context\":\"=EMBED_MISC\"}", //style innerText
		"http://www.slashdot.org",
		"{ \"tagName\": \"div\",\"style\": \"background: url('http://www.slashdot.org/');\", \"hop\":\"EMBED\", \"context\":\"=EMBED_MISC\"}", //div style
		"http://www.slashdot.org",
		"{ \"tagName\": \"frame\",\"src\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"frame/@src\"}", //frame src
		"http://www.slashdot.org",
		"{ \"tagName\": \"iframe\",\"src\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"iframe/@src\"}", //frame src
		"http://www.slashdot.org",
		"{ \"tagName\": \"object\",\"classid\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"object/@classid\"}", //no codebase
		"http://www.slashdot.org",
		"{ \"tagName\": \"object\",\"data\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"object/@data\"}", //no codebase
		"http://www.slashdot.org",
		"{ \"tagName\": \"object\",\"code\": \"http://www.slashdot.org/\", \"hop\":\"EMBED\", \"context\":\"object/@code\"}", //no codebase
		"http://www.slashdot.org",
		"{ \"tagName\": \"object\",\"value\": \"http://www.slashdot.org/\", \"hop\":\"NAVLINK\", \"context\":\"object/@value\"}", //value
		"http://www.slashdot.org"
	};
	@Override
	protected String[] getValidTestData() {
		return VALID_TEST_DATA;
	}

	@Override
	protected Collection<TestData> makeData(String content, String destURI)
			throws Exception {
		//JSONArray jsonArray = new JSONArray(content);
		JSONObject json = new JSONObject(content);
		String hopValue = json.getString("hop");
		String contextValue = json.getString("context");
		
        List<TestData> result = new ArrayList<TestData>();
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
                
        UURI dest = UURIFactory.getInstance(destURI);
        LinkContext context = new HTMLLinkContext(contextValue);
        Hop hop = Hop.valueOf(hopValue);
        Link link = new Link(src, dest, context, hop);
        result.add(new TestData(euri, link));
        
        euri = new CrawlURI(src, null, null, LinkContext.NAVLINK_MISC);
        recorder = createRecorder(content);
        euri.setContentType("application/xhtml");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        result.add(new TestData(euri, link));
        
        return result;
	}

	
	@Override
	protected Extractor makeExtractor() {
        ExternalBrowserExtractorHtml result = new MockExternalBrowserExtractorHtml();
        
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();  
        result.setLoggerModule(ulm);
        CrawlMetadata metadata = new CrawlMetadata();
        metadata.afterPropertiesSet();
        result.setMetadata(metadata);
        result.setExecutionString("BLAH _URI_");
        result.afterPropertiesSet();
        result.setExtractOnly200Status(false);
        
        return result;
	}
	public void testMock(){
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String text = "{ \"blah\":\"value\" }";
    	extractor.setReturnValue(text);
    	String returnValue = extractor.executeCommand("",null);
    	assertEquals(text,returnValue);
	}
	public void testXHRAsLink() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		extractor.setTreatXHRAsEmbedLinks(false);
		String content = "{ \"tagName\": \"XMLHttpRequest\",\"url\": \"http://www.slashdot.org/\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("=BROWSER_MISC"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	
	public void testCodebaseRelative() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"object\",\"classid\": \"/blah\",\"codebase\":\"http://www.slashdot.org\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("object/@codebase"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah"), new HTMLLinkContext("object/@classid"), Hop.valueOf("EMBED")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testAppletCodebaseCode() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"applet\",\"code\": \"/blah\",\"codebase\":\"http://www.slashdot.org\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("applet/@codebase"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah.class"), new HTMLLinkContext("applet/@code"), Hop.valueOf("EMBED")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testCodebaseArchive() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"object\",\"classid\": \"/blah\",\"codebase\":\"http://www.slashdot.org\",\"archive\":\"blah.jar,blah2.jar, blah3.jar\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("object/@codebase"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah"), new HTMLLinkContext("object/@classid"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah.jar"), new HTMLLinkContext("object/@archive"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah2.jar"), new HTMLLinkContext("object/@archive"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah3.jar"), new HTMLLinkContext("object/@archive"), Hop.valueOf("EMBED")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testFormNoMethod() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"form\",\"action\": \"http://www.slashdot.org\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("form/@action"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testFormGet() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"form\",\"action\": \"http://www.slashdot.org\", \"method\":\"get\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("form/@action"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testFormPostNegative() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"form\",\"action\": \"http://www.slashdot.org\", \"method\":\"post\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        assertTrue(euri.getOutLinks().size()==0);
        assertNoSideEffects(euri);
	}
	public void testFormPostPositive() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		extractor.setExtractOnlyFormGets(false);
		String content = "{ \"tagName\": \"form\",\"action\": \"http://www.slashdot.org\", \"method\":\"post\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("form/@action"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testBaseTag() throws Exception{
		//TODO possible bug if base doesn't end in /
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"base\",\"href\": \"http://www.slashdot.org/sub1/\", \"method\":\"post\"}\n";
		content += "{\"tagName\":\"a\",\"href\":\"/blah\"}\n";
		content += "{\"tagName\":\"a\",\"href\":\"blah\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/blah"), new HTMLLinkContext("a/@href"), Hop.valueOf("NAVLINK")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/sub1/blah"), new HTMLLinkContext("a/@href"), Hop.valueOf("NAVLINK")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org/sub1/"), new HTMLLinkContext("base/@href"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testFrameAsLink() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		extractor.setTreatFramesAsEmbedLinks(false);
		String content = "{ \"tagName\": \"frame\",\"src\": \"http://www.slashdot.org\"}\n";
		content +="{\"tagName\":\"iframe\", \"src\":\"http://www.slashdot.org\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("frame/@src"), Hop.valueOf("NAVLINK")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.slashdot.org"), new HTMLLinkContext("iframe/@src"), Hop.valueOf("NAVLINK")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testValueNegative() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		extractor.setExtractValueAttributes(false);
		String content = "{ \"tagName\": \"object\",\"value\": \"http://www.slashdot.org\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        assertTrue(euri.getOutLinks().size()==0);
        assertNoSideEffects(euri);
	}
	public void testValueFlashvars() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "{ \"tagName\": \"param\",\"value\": \"http://www.slashdot.org?key=value&url=http://www.blah.com\",\"name\":\"flashvars\"}";
		
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.blah.com"), new HTMLLinkContext("param/@value"), Hop.valueOf("SPECULATIVE")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testComments() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		
		String content = "{\"tagName\": \"#comment\",\"value\": \"[if lte IE 6]>\\r\\n    <link rel=\\\"stylesheet\\\" href=\\\"/Content/Cached/MainSiteIE6-minCss.css\\\" type=\\\"text/css\\\" media=\\\"screen\\\" />\\r\\n        <script type=\\\"text/javascript\\\" src=\\\"/Content/Scripts/jquery.digitalarchivesIE.js\\\"></script> \\r\\n    <![endif]\"}";
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/#blah");
        
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
    	extractor.setReturnValue(content);
        extractor.process(euri);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(new Link(src, UURIFactory.getInstance("http://www.archive.org/Content/Cached/MainSiteIE6-minCss.css"), new HTMLLinkContext("link/@href"), Hop.valueOf("EMBED")));
        expected.add(new Link(src, UURIFactory.getInstance("http://www.archive.org/Content/Scripts/jquery.digitalarchivesIE.js"), new HTMLLinkContext("script/@src"), Hop.valueOf("EMBED")));
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
	}
	public void testExecutionStringGeneration() throws Exception{
		MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
		String content = "<a href='http://www.slashdot.org/'>blah</a>";
		UURI src = UURIFactory.getInstance("http://www.archive.org/start/#blah");
        
        CrawlURI euri = new CrawlURI(src, null, null, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentSize(content.length());
        
        extractor.setExecutionString("node myScript.js --url _URI_ --userAgent _USERAGENT_ --preload _PRELOADJSON_");
    	extractor.setReturnValue(content);
    	
        //extractor.process(euri);
        String execString = extractor.generateExecutionString(euri,content,"","", false);
        extractor.cleanupTempFiles();
        assertTrue(execString.startsWith("node myScript.js --url \"http://www.archive.org/start/\" --userAgent \"userAgent\" --preload "));
        String jsonText = execString.substring(execString.indexOf("--preload ")+10).trim().replace("\\", "");
        System.out.println(jsonText);
        JSONObject jobj = new JSONObject(jsonText);
        assertTrue(jobj.has("Content-Type"));
        assertEquals("text/html",jobj.getString("Content-Type"));
        assertTrue(jobj.has("body"));
        assertNoSideEffects(euri);
	}
	
	@Override
	   public void testExtraction() throws Exception {
	        try {
	            String[] valid = getValidTestData();
	            for (int i = 0; i < valid.length; i += 2) {
	                testOne(valid[i], valid[i + 1]);
	            }
	        } catch (Exception e) {
	            e.printStackTrace(); 
	            throw e;
	        }
	    }
	private void testOne(String text, String expectedURL) throws Exception {
        Collection<TestData> testDataCol = makeData(text, expectedURL);
        for (TestData testData: testDataCol) {
        	MockExternalBrowserExtractorHtml extractor = (MockExternalBrowserExtractorHtml)makeExtractor();
        	extractor.setReturnValue(text);
            extractor.process(testData.uri);
            HashSet<Link> expected = new HashSet<Link>();
            expected.add(testData.expectedResult);
            assertEquals(expected, testData.uri.getOutLinks());
            assertNoSideEffects(testData.uri);
        }
    }

}



class MockExternalBrowserExtractorHtml extends ExternalBrowserExtractorHtml{

	private String returnVal;
	public MockExternalBrowserExtractorHtml(){
		super();
		UserAgentProvider uap = new UserAgentProvider(){
			public String getUserAgent(){ return "userAgent";}
			public String getFrom(){ return "from";}
		};
		this.setUserAgentProvider(uap);
	}
	public void setReturnValue(String returnValue) { returnVal = returnValue; };
	
	@Override
	protected String executeCommand(String executionString, File tempOutFile){
		return returnVal;
	}

}
//this shouldn't be necessary. not sure why it won't compile without this.
class UnitTestUriLoggerModule implements UriErrorLoggerModule {
    private static final long serialVersionUID = 1L;
    
    final private static Logger LOGGER = 
        Logger.getLogger(UnitTestUriLoggerModule.class.getName());

    public void logUriError(URIException e, UURI u, CharSequence l) {
        LOGGER.log(Level.INFO, u.toString(), e);
    }

    public Logger getLogger(String name) {
        return LOGGER;
    }

    
    
}
