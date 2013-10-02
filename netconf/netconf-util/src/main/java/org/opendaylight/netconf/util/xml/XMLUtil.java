/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util.xml;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class XMLUtil {

	public static final String RFC4741_TARGET_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
	public static final String URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0 = "urn:ietf:params:xml:ns:netconf:base:1.0";
	public static final String URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING = "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring";
	// TODO where to store namespace of config ?
	public static final String URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG = "urn:opendaylight:params:xml:ns:yang:controller:config";

	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setCoalescing(true);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		try {
			return factory.newDocumentBuilder();
		} catch (final Exception e) {
			throw new RuntimeException("Initialization error", e);
		}
	}

	public static Document newDocument() {

		return getDocumentBuilder().newDocument();
	}

	public static Document parse(InputStream inputStream) throws IOException, SAXException {
		final Document doc = getDocumentBuilder().parse(inputStream);
		Preconditions.checkState(doc.getInputEncoding().equals("UTF-8") == true);
		return doc;
	}

	// for debugging
	public static String xmlToString(Document doc) {
		// create String from the underlying Document
		final StringWriter stw = new StringWriter();
		Transformer serializer;
		try {
			serializer = TransformerFactory.newInstance().newTransformer();
			serializer.transform(new DOMSource(doc), new StreamResult(stw));
			return stw.toString();
		} catch (TransformerException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Document stringToXml(String stringXml) {
		try {
			return getDocumentBuilder().parse(new InputSource(new StringReader(stringXml)));
		} catch (SAXException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Schema loadSchema(InputStream... fromStreams) {
		// final InputStream xmlSchema = NetconfSessionListenerImpl.class.getResourceAsStream("xml.xsd");
		// Preconditions.checkNotNull(xmlSchema, "Cannot find xml.xsd");
		//
		// final InputStream rfc4714Schema = NetconfSessionListenerImpl.class.getResourceAsStream("rfc4741.xsd");
		// Preconditions.checkNotNull(rfc4714Schema, "Cannot find rfc4741.xsd");

		Source[] sources = new Source[fromStreams.length];
		int i = 0;
		for (InputStream stream : fromStreams) {
			sources[i++] = new StreamSource(stream);
		}

		final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			return schemaFactory.newSchema(sources);
		} catch (SAXException e) {
			throw new IllegalStateException("Failed to instantiate XML schema", e);
		}
	}

	public static Object evaluateXPath(XPathExpression expr, Object rootNode, QName returnType) {
		try {
			return expr.evaluate(rootNode, returnType);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Error while evaluating xpath expression " + expr, e);
		}
	}

	public static XPathExpression compileXPath(String xPath) {
		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		xpath.setNamespaceContext(new HardcodedNamespaceResolver("netconf", RFC4741_TARGET_NAMESPACE));
		try {
			return xpath.compile(xPath);
		} catch (final XPathExpressionException e) {
			throw new IllegalStateException("Error while compiling xpath expression " + xPath, e);
		}
	}

	public static Document createDocumentCopy(Document original) {
		final Document copiedDocument = XMLUtil.newDocument();
		final Node copiedRoot = copiedDocument.importNode(original.getDocumentElement(), true);
		copiedDocument.appendChild(copiedRoot);
		return copiedDocument;
	}

}
