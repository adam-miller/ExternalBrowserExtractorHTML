package org.archive.modules.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.archive.io.ReplayCharSequence;
import org.archive.io.SinkHandlerLogThread;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.UserAgentProvider;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.DevUtils;
import org.archive.util.FileUtils;
import org.archive.util.TextUtils;
import org.archive.util.UriUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Browser based link extraction for use with an external process.
 * Will execute the specified local command and parse a returned set of outlinks.
 * This will open the URI in a browser and by default, download scripts and css.
 * The scripts/css downloaded by the browser will not follow any robots restrictions, so use carefully.
 * 
 * TODO: Look into trying to pass robots rules to browser.
 * TODO: Maybe parse data- attributes?
 * 
 * @contributor adam
 *
 */
public class ExternalBrowserExtractorHtml extends ExtractorHTML implements InitializingBean, BrowserExtractorHtml  {

    private static Logger logger =
            Logger.getLogger(ExternalBrowserExtractorHtml.class.getName());
    
    /**
     * browser will always execute javascript on it's own.
     * TODO: Look into possibly disabling javascript in the browser to allow this.
     */
    {
    	setExtractJavascript(true);
    }
    
    /**
     * Override the user agent for the external browser.
     */
    {
    	setUserAgent("");
    }
    public String getUserAgent(){
    	return (String) kp.get("userAgent");
    }
    public void setUserAgent(String userAgent){
    	kp.put("userAgent", userAgent);
    }

    /**
     * Have the external browser re-load the URI from scratch instead of using the local cached copy. (default=false);
     */
    {
    	setShouldBrowserReload(false);
    }
    public boolean getShouldBrowserReload(){
    	return (Boolean) kp.get("shouldBrowserReload");
    }
    public void setShouldBrowserReload(boolean shouldRelaod){
    	kp.put("shouldBrowserReload", shouldRelaod);
    }
    
    /**
     * Lenght of time (in milliseconds) to wait for browser to finish processing. (default=120000ms)
     */
    {
    	setCommandTimeout(120000L);
    }
    public long getCommandTimeout(){
    	return (Long) kp.get("commandTimeout");
    }
    public void setCommandTimeout(long timeoutInMillis){
    	kp.put("commandTimeout", timeoutInMillis);
    }
    
    
    /**
     * If true, XMLHttpRequests are treated as embedded resources (like
     * IMG, 'E' hop-type), otherwise they are treated as navigational links.
     * Default is true.
     */
    {
        setTreatXHRAsEmbedLinks(true);
    }
    public boolean getTreatXHRAsEmbedLinks() {
        return (Boolean) kp.get("treatXHRAsEmbedLinks");
    }
    public void setTreatXHRAsEmbedLinks(boolean asEmbeds) {
    	kp.put("treatXHRAsEmbedLinks",asEmbeds);
    }
   
    /**
     * The command line string to execute
     */
    public String getExecutionString() {
        return (String) kp.get("executionString");
    }
    public void setExecutionString(String executionString) {
        kp.put("executionString",executionString);
    }
         
    public UserAgentProvider getUserAgentProvider() {
        return (UserAgentProvider) kp.get("userAgentProvider");
    }
    @Autowired
    public void setUserAgentProvider(UserAgentProvider provider) {
        kp.put("userAgentProvider",provider);
    }
    
    {
        setExtractOnly200Status(true);
    }
    public boolean getExtractOnly200Status() {
        return (Boolean) kp.get("extractOnly200Status");
    }
    public void setExtractOnly200Status(boolean only200) {
    	kp.put("extractOnly200Status",only200);
    }
    
    static final String WHITESPACEORCOMMA = "(\\s|[,])";
    private final static String META_TAG_REGEX = "(?is)<(?:((meta)\\s+[^>]*+))>";
    
	public ExternalBrowserExtractorHtml() { 
		super();
	}



	protected void extract(CrawlURI curi, CharSequence cs){		
		
		if(getExtractOnly200Status() && curi.getFetchStatus()!=200){
			return;
		}
		Matcher tags = TextUtils.getMatcher(META_TAG_REGEX,cs);
		while(tags.find()) {
			if(Thread.interrupted()){
				break;
			}
			else if (tags.start(2) > 0) {
				// <meta> match
				int start = tags.start(1);
				int end = tags.end(1);
				assert start >= 0: "Start is: " + start + ", " + curi;
				assert end >= 0: "End is :" + end + ", " + curi;
				if (processMeta(curi,
						cs.subSequence(start, end))) {
					// meta tag included NOFOLLOW; abort processing
					break;
				}
			}
		}
		TextUtils.recycleMatcher(tags);
		
		
		String returnValue="";
		File tempInputFile = null;
		File tempOutputFile = null;
		try{
			int sn;
			Thread thread = Thread.currentThread();
			if (thread instanceof SinkHandlerLogThread) {
				sn = ((SinkHandlerLogThread)thread).getSerialNumber();
			} else {
				sn = System.identityHashCode(thread);
			}
			String tempPrefix = "tt-"+sn+"-"+System.identityHashCode(curi);
			tempInputFile = File.createTempFile(tempPrefix, "tmp.html");
			tempOutputFile = File.createTempFile(tempPrefix, "tmp_out.json");
			
			boolean shouldReload = getShouldBrowserReload();
			if(!shouldReload){ //if preload is set, write to temp file.
		        OutputStream outStream = null;
		        try {
		            outStream = org.apache.commons.io.FileUtils.openOutputStream(tempInputFile); 
		            IOUtils.write(cs.toString(), outStream);
		        } catch (Exception e) {
		        	shouldReload=true;  //okay to recover here. we just don't include the preloaded file.
					logger.warning("Error occured while trying to set pre-loaded content - Will reload. Error Description: " + e.getMessage());
				}
		        finally {
		            IOUtils.closeQuietly(outStream); 
		        }
			}
			String fullExecString = generateExecutionString(curi,cs,tempInputFile.getCanonicalPath(), tempOutputFile.getCanonicalPath(),shouldReload);
			
			returnValue = executeCommand(fullExecString,tempOutputFile);

			
		} catch (IOException e) {
			logger.warning("Error creating temp files - skipping browser extraction" + e.getMessage());
			return;
		}
		finally {
			//TODO should probably not extract error pages by default

			if(tempInputFile!=null)
				FileUtils.deleteSoonerOrLater(tempInputFile);
			if(tempOutputFile!=null)
				FileUtils.deleteSoonerOrLater(tempOutputFile);
			
			
		}
		innerExtract(curi,cs,returnValue);
	}
	protected void innerExtract(CrawlURI curi, CharSequence cs, String jsonResults){
		if( jsonResults!=null && jsonResults.length()>0 ){

			boolean framesAsEmbeds=getTreatFramesAsEmbedLinks();
			boolean extractValueAttributes = getExtractValueAttributes();
			boolean extractOnlyFormGets = getExtractOnlyFormGets();
			boolean XHRAsEmbeds=getTreatXHRAsEmbedLinks();
			
			Scanner scanner = new Scanner(jsonResults);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				JSONObject json;
				try {
					json = new JSONObject(line);
				}
				catch(JSONException e) {
					logger.log(Level.WARNING, "Error parsing JSON line - Skipping: "+line, e);
					continue;
				}
				try {
					String tag = json.getString("tagName");
					if(json.has("href")){ //must come first for base tag
						if(tag.equalsIgnoreCase("base")){
		                    try {
		                        UURI base = UURIFactory.getInstance(json.getString("href"));
		                        curi.setBaseURI(base);
		                    } catch (URIException e) {
		                        logUriError(e, curi.getUURI(), json.getString("href"));
		                    }
						}
						
						if(tag.equalsIgnoreCase("link"))
							processEmbed(curi,json.getString("href"),tag+"/@href",Hop.EMBED);
						else
							processLink(curi,json.getString("href"),tag+"/@href".toString());
					}
					
					for(String attr : new String[]{"lowsrc","background","cite","longdesc","usemap","profile","datasrc" }){
						if(json.has(attr))
							processEmbed(curi,json.getString(attr),tag+"/@"+attr,Hop.EMBED);
					}
					for(String attr : new String[]{"classid", "data", "archive", "code"}){ //all relative to codebase
						if(json.has(attr)){
							UURI codebaseURI =null;
							String[] resources=null;
							String codebase="";
							try{
								if("archive".equalsIgnoreCase(attr)) //handle special case for 'archive' attribute
									resources = TextUtils.split(WHITESPACEORCOMMA, json.getString("archive"));
								else if("code".equalsIgnoreCase(attr)){ //handle special case for 'code' attribute
									String value = json.getString("code");
									// If element is applet and code value does not end with
									// '.class' then append '.class' to the code value.
									if (tag.equalsIgnoreCase(APPLET) &&
											!value.toString().toLowerCase().endsWith(CLASSEXT)) {
										resources = new String[] {value + CLASSEXT};
									} else {
										resources = new String[] {value};
									}
								}
								else
									resources = new String[] {(String)TextUtils.unescapeHtml(json.getString(attr)) };

								if(json.has("codebase")){
									codebase = json.getString("codebase");
									processEmbed(curi,codebase,tag+"/@codebase",Hop.EMBED);
									codebaseURI = UURIFactory.getInstance(curi.getUURI(), codebase);
								}

								for(String res : resources){
									if(res.trim().length()>0){
										if (codebaseURI != null) {
											res = codebaseURI.resolve(res).toString();
										}
										processEmbed(curi, res, tag+"/@"+attr,Hop.EMBED);
									}
								}

							} catch (URIException e) {
								curi.getNonFatalFailures().add(e);
							} catch (IllegalArgumentException e) {
								DevUtils.logger.log(Level.WARNING, "innerExtract()\n" +
										"codebase=" + codebase + " "+attr+"=" + attr + "\n" +
										DevUtils.extraInfo(), e);
							}
						}
					}
					
					if ("XMLHttpRequest".equalsIgnoreCase(tag) && json.has("url")){
						if(XHRAsEmbeds)
							processEmbed(curi,json.getString("url"),"=BROWSER_MISC",Hop.EMBED);
						else
							processLink(curi,json.getString("url"),"=BROWSER_MISC".toString());
					}
					if("#comment".equalsIgnoreCase(tag) && json.has("value")){ // extracted comments get parsed via regex since they are not in the dom
						String commentValue = json.getString("value");
						super.extract(curi, commentValue);
					}
					else if(json.has("src")){ 
						if(!framesAsEmbeds && tag.toLowerCase().endsWith("frame"))
							processLink(curi,json.getString("src"),tag+"/@src");
						else
							processEmbed(curi,json.getString("src"),tag+"/@src",Hop.EMBED);
					}
					else if(json.has("style")){
						numberOfLinksExtracted.addAndGet(ExtractorCSS.processStyleCode(this, curi, json.getString("style"))); 
					}
					else if(tag.equalsIgnoreCase("style") && json.has("innerText")){
						numberOfLinksExtracted.addAndGet(ExtractorCSS.processStyleCode(this, curi, json.getString("innerText"))); 
					}
					else if(json.has("value") && extractValueAttributes){
						String value = json.getString("value");
			            if ("PARAM".equalsIgnoreCase(tag) && json.has("name") && json.getString("name").equalsIgnoreCase("flashvars")){
			                // special handling for <PARAM NAME='flashvars" VALUE="">
			                String queryStringLike = value;
			                // treat value as query-string-like "key=value[&key=value]*" pairings
			                considerQueryStringValues(curi, queryStringLike, tag+"/@value",Hop.SPECULATIVE);
			            } else {
			                // regular VALUE handling
		                    considerIfLikelyUri(curi,value,tag+"/@value",Hop.NAVLINK);
			            }
					}
					else if(tag.equalsIgnoreCase("form") && json.has("action")){
						String method = json.has("method")?json.getString("method"):null;
						if(method==null || "GET".equalsIgnoreCase(method) || !extractOnlyFormGets)
							processLink(curi, json.getString("action"), "form/@action");
					}
					
				} catch (JSONException e) {
					logger.log(Level.WARNING, "Error parsing JSON line - Skipping: "+line, e);
				}

			}

		}
	}
    
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if(this.getExtractJavascript()==false)
			throw new UnsupportedOperationException("ExtractJavascript is not valid in this context");
	}
	protected String generateExecutionString(CrawlURI curi, CharSequence cs, String tempInputFilePath, String tempOutputFilePath, boolean shouldReload){
		String userAgent = getUserAgent();
		if(userAgent==null || userAgent.trim().length()==0)
			userAgent=curi.getUserAgent();
		String execString = getExecutionString();

		String fullExecString=execString.replace("_URI_","\""+curi.getUURI().getEscapedURI().replace("\"", "\\\"")+"\"");
			
		String escapedUserAgent=null;
		if(userAgent!=null && userAgent.trim().length()>0)
			escapedUserAgent=userAgent.replace("\"", "\\\"");
		else if(curi.getUserAgent()!=null)
			escapedUserAgent=curi.getUserAgent().replace("\"", "\\\"");
		else 
			escapedUserAgent=getUserAgentProvider().getUserAgent().replace("\"", "\\\"");
		
		fullExecString = fullExecString.replace("_USERAGENT_","\""+escapedUserAgent+"\"");
		fullExecString = fullExecString.replace("_OUTPUTFILEPATH_","\""+tempOutputFilePath.replace("\"", "\\\"")+"\"");
		
		if(!shouldReload){ // if we want to preload the content
			JSONObject preloadObject = new JSONObject();
			try {
				preloadObject.put("Content-Type",curi.getContentType());
				//this should ideally include all of the response headers
				preloadObject.put("body",tempInputFilePath);
				String escapedJSON = preloadObject.toString().replaceAll("([^a-zA-Z0-9])", "\\\\$1");
				fullExecString = fullExecString.replace("_PRELOADJSON_",escapedJSON);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error occured while trying to set pre-loaded content.", e);
			}
		}	
		
		
		return fullExecString;
	}
    protected String executeCommand(String executionString, File tempOutputFile){
		String response = "";
		
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Executing command line: "+executionString);
        }
		ProcessBuilder pb = new ProcessBuilder("bash","-c",executionString);
		pb.redirectErrorStream(true);
		int shellExitStatus=0;
		try {
			final Process shell = pb.start();
			
			Callable<Integer> call = new Callable<Integer>(){
				public Integer call() throws Exception {
					shell.waitFor();
					return shell.exitValue();
				}
			};
			ExecutorService service = Executors.newSingleThreadExecutor();
			try{
				Future<Integer> ft = service.submit(call);
				try{
					shellExitStatus = ft.get(getCommandTimeout(),TimeUnit.MILLISECONDS);
				} catch (TimeoutException ex) {
					shell.destroy();
					shellExitStatus=-5;
				} catch (ExecutionException e) {
					shell.destroy();
					shellExitStatus=-5;
				}
				
			} finally{
				service.shutdown();
			}
			
	        InputStream inStream = null;
	        
	        try {
	        	inStream = org.apache.commons.io.FileUtils.openInputStream(tempOutputFile);
	        	response = IOUtils.toString(inStream);
	        } 
	        catch (IOException e) {
    			logger.log(Level.WARNING, "Error occured while reading command response.", e);
	        }
	        finally {
	            IOUtils.closeQuietly(inStream); 
	        }
	        
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Error occured while executing command. ExecStr: "+ executionString, e);
		}

		catch (InterruptedException e) {
			logger.log(Level.WARNING, "Error occured while executing command. ExecStr: "+ executionString, e);
		}

		if(shellExitStatus!=0){
			if(shellExitStatus==-5){
				logger.log(Level.WARNING, "Command exceeded timeout: "+shellExitStatus, executionString);
			}
			else{
				logger.log(Level.WARNING, "Command returned status code: "+shellExitStatus, executionString);
			}
		}
		return response;
    }
	private String convertStreamToStr(InputStream is) throws IOException {

		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is,
						"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		}
		else {
			return "";
		}
	}
	protected void cleanupTempFiles(){
	}
}
