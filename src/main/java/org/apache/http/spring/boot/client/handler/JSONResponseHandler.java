package org.apache.http.spring.boot.client.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.XMLConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.apache.http.spring.boot.client.ContentType;
import org.apache.http.spring.boot.client.exception.HttpResponseException;
import org.apache.http.spring.boot.client.utils.HttpResponeUtils;
import org.apache.http.spring.boot.client.utils.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.alibaba.fastjson.JSONObject;

/**
 * {@link ResponseHandler} implementation that converts the response body into a
 * {@link JSONObject}. XML responses ({@code application/xml}) are parsed into a JSON object
 * tree, JSON responses ({@code application/json}) are parsed directly, and any other content
 * type produces an {@link HttpResponseException}.
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class JSONResponseHandler implements ResponseHandler<JSONObject> {

	/** SAX reader used to parse XML response bodies. */
	protected SAXReader reader = secureReader();

	private static SAXReader secureReader() {
		SAXReader reader = new SAXReader();
		try {
			reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
			reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			reader.setIncludeExternalDTDDeclarations(false);
			reader.setEntityResolver((publicId, systemId) -> {
				throw new SAXException("External XML entities are disabled");
			});
		} catch (SAXException | IllegalArgumentException ex) {
			throw new IllegalStateException("Cannot configure a safe XML parser", ex);
		}
		return reader;
	}
	/**
	 * <p>Handle client.</p>
	 * @param httpclient the httpclient
	 */
	
	@Override
	public void handleClient(HttpClient httpclient) {
		
	}
	
	/**
	 * Convert the response body into a {@link JSONObject}. XML bodies are parsed and converted
	 * into a JSON object tree; JSON bodies are parsed directly.
	 * @param httpMethod the executed HTTP method
	 * @return the response parsed into a {@link JSONObject}
	 * @throws IOException if the status is not 2xx, the content type is unexpected, or parsing fails
	 */
	@Override
	public JSONObject handleResponse(HttpMethodBase httpMethod) throws IOException {
		StatusLine statusLine = httpMethod.getStatusLine();
		int status = statusLine.getStatusCode();
		if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
			String contentType = HttpResponeUtils.getContentType(httpMethod);
			// xml转换成JSON对象
			if (contentType.startsWith(ContentType.APPLICATION_XML)) {
				// 将解析结果存储在JSONObject中
				JSONObject resultXML = new JSONObject();
				// 从request中取得输入流
				InputStream input = null;
				try {
					input = httpMethod.getResponseBodyAsStream();
					Document document = reader.read(input);
					// 得到xml根元素
					Element root = document.getRootElement();
					// 得到根元素的所有子节点
					List<Element> elementList = root.elements();
					List<Element> childElements = null;
					// 遍历所有子节点
					for (Element e : elementList) {
						childElements = e.elements();
						if (childElements != null && !childElements.isEmpty()) {
							resultXML.put(e.getName(),parseJSONObject(childElements));
						} else {
							resultXML.put(e.getName(), e.getTextTrim());
						}
					}
				} catch (DocumentException ex) {
					throw new HttpResponseException("Malformed XML document",ex);
				} finally {
					// 释放资源
					IOUtils.closeQuietly(input);
				}
				return resultXML;
			} else if (contentType.startsWith(ContentType.APPLICATION_JSON)) {
				return JSONObject.parseObject(httpMethod.getResponseBodyAsString());
			} else {
				throw new HttpResponseException("Unexpected content type:" + contentType);
			}
		} else {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}
	}

	/**
	 * Recursively convert a list of XML {@link Element}s into a nested {@link JSONObject}.
	 * @param childElements the child elements to convert
	 * @return the resulting {@link JSONObject}
	 */
	private static JSONObject parseJSONObject(List<Element> childElements) {
		// 将解析结果存储在JSONObject中
		JSONObject resultXML = new JSONObject();
		if (childElements != null && !childElements.isEmpty()) {
			// 遍历所有子节点
			for (Element e2 : childElements) {
				// 得到根元素的所有子节点
				List<Element> childElements2 = e2.elements();
				if (childElements2 != null && !childElements2.isEmpty()) {
					resultXML.put(e2.getName(), parseJSONObject(childElements2));
				} else {
					resultXML.put(e2.getName(), e2.getTextTrim());
				}
			}
		}
		return resultXML;
	}
}
