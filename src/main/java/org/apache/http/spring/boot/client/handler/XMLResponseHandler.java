 package org.apache.http.spring.boot.client.handler;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.http.spring.boot.client.exception.HttpResponseException;

/**
 * {@link ResponseHandler} implementation that parses the response body into a DOM
 * {@link Document}.
 * <p>A {@link HttpResponseException} is thrown when the status code is outside the 2xx range
 * or the XML is malformed.</p>
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class XMLResponseHandler implements ResponseHandler<Document> {

	/** Factory used to create the {@link DocumentBuilder} that parses the response body. */
	private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	/**
	 * <p>Handle client.</p>
	 * @param httpclient the httpclient
	 */
	
	@Override
	public void handleClient(HttpClient httpclient) {

	}
	
	/**
	 * Parse the response body into a DOM {@link Document}.
	 * @param httpMethod the executed HTTP method
	 * @return the parsed {@link Document}
	 * @throws IOException if the status is not 2xx or the XML is malformed
	 */
	@Override
    public Document handleResponse(HttpMethodBase httpMethod) throws IOException {
		StatusLine statusLine = httpMethod.getStatusLine();
		int status = statusLine.getStatusCode();
		if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
	        try {
	            DocumentBuilder docBuilder = factory.newDocumentBuilder();
	            return docBuilder.parse(new InputSource(new StringReader(httpMethod.getResponseBodyAsString())));
	        } catch (ParserConfigurationException ex) {
	            throw new IllegalStateException(ex);
	        } catch (SAXException ex) {
	            throw new HttpResponseException("Malformed XML document", ex);
	        } finally {
			}
		} else {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}
    }
}

