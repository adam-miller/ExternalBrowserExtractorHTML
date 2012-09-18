package org.archive.modules.deciderules;

import org.archive.modules.CrawlURI;

/**
 * Rule applies configured decision to any CrawlURIs whose 'hops-path'
 * (string like "LLXE" etc.) matches the supplied regex.
 *
 * @author adam-miller
 */
public class ViaContextMatchesRegexDecideRule extends MatchesRegexDecideRule {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
     * Usual constructor. 
     * @param name
     */
    public ViaContextMatchesRegexDecideRule() {
    }

    
    @Override
    protected String getString(CrawlURI uri) {
    	if(uri.getViaContext()!=null)
    		return uri.getViaContext().toString();
    	else
    		return "";
    }

}
