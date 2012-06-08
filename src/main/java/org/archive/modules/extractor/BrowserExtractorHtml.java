package org.archive.modules.extractor;

public interface BrowserExtractorHtml {

	public String getUserAgent();
	public void setUserAgent(String userAgent);
	public boolean getTreatFramesAsEmbedLinks();
	public void setTreatFramesAsEmbedLinks(boolean asEmbeds);
	public boolean getTreatXHRAsEmbedLinks();
	public void setTreatXHRAsEmbedLinks(boolean asEmbeds);
}
