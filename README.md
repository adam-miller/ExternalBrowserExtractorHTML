ExternalBrowserExtractorHTML
============================

External Browser Extractor Processor for heritrix3. Execute an external browser via command line and parse JSON results

Build: mvn -Dmaven.test.skip=false clean install
Result: target/ExternalBrowserExtractorHTML-0.1.jar


Configuration crawler-beans.cxml
================================

Define your extractor bean (You can put this near the settings for the other extractors: extractorHTML, extractorCSS, etc). The optional proxy mode configuration is shown below:

    <bean id="browserExtractorHtml" class="org.archive.modules.extractor.ExternalBrowserExtractorHtml" >
      <property name="executionString" value="nice -n4 /phantomjs/phantomjs-1.6.1-linux-i686-dynamic/bin/phantomjs --proxy=127.0.0.1:3128 /phantomjs/phantomBrowserExtractor/phantomBrowserExtractor.js --userAgent _USERAGENT_ --url _URI_ 2>/dev/null"/>
      <property name="shouldBrowserReload" value="True" />
      <property name="commandTimeout" value="30000" />
      <!-- <property name="treatFramesAsEmbedLinks" value="true" /> -->
      <!-- <property name="treatXHRAsEmbedLinks" value="true" /> -->
    </bean>


Add it to the processor chain: inside  <bean id="fetchProcessors" class="org.archive.modules.FetchChain"><property name="processors"><list>
Add it before the extractorHTML processor:

    <!-- ...extract outlinks from HTML content via external browser process... -->
    <ref bean="browserExtractorHtml"/>


When using a caching proxy like Squid with the browser, you will want to direct H3 to request the discovered items through the proxy. Create a sheet association with the configuration shown below. The ViaContextMatchesRegexDecide rule is included in this package:

    <bean class='org.archive.crawler.spring.DecideRuledSheetAssociation'>
      <property name='rules'>
        <bean class="org.archive.modules.deciderules.ViaContextMatchesRegexDecideRule">
          <property name="regex" value=".*BROWSER_MISC.*" />
        </bean>
      </property>
      <property name='targetSheetNames'>
       <list>
        <value>cacheProxy</value>
       </list>
      </property>
    </bean>
    <bean id='cacheProxy' class='org.archive.spring.Sheet'>
      <property name='map'>
        <map>
          <entry key='fetchHttp.httpProxyHost' value='127.0.0.1' />
          <entry key='fetchHttp.httpProxyPort' value='3128' />
        </map>
      </property>
    </bean>