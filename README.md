ExternalBrowserExtractorHTML
============================

External Browser Extractor Processor for heritrix3. Execute an external browser via command line and parse JSON results

Build: mvn -Dmaven.test.skip=false clean install
Result: target/ExternalBrowserExtractorHTML-0.1.jar


Configuration crawler-beans.cxml
================================

Define your extractor bean (You can put this near the settings for the other extractors: extractorHTML, extractorCSS, etc):

       <bean id="browserExtractorHtml" class="org.archive.modules.extractor.ExternalBrowserExtractorHtml" >
       	     <property name="executionString" value="/usr/local/bin/node /Users/accounts/Downloads/zombieBrowserExtractor/lib/zombieBrowserExtractor/zombieBrowserExtractor.js --userAgent _USERAGENT_ --url _URL_ > _OUTPUTFILEPATH_ 2>/dev/null"/>
	     <property name="shouldBrowserReload" value="True" />
 	     <property name="commandTimeout" value="240000" />
	     <!-- <property name="treatFramesAsEmbedLinks" value="true" /> -->
	     <!-- <property name="treatXHRAsEmbedLinks" value="true" /> -->
      </bean>

Add it to the processor chain: inside  <bean id="fetchProcessors" class="org.archive.modules.FetchChain"><property name="processors"><list>
Add it after the extractorHTML processor:

    <!-- ...extract outlinks from HTML content via external browser process... -->
    <ref bean="browserExtractorHtml"/>

