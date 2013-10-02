/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util.xml;

import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// TODO merge with xml util
public class Xml {

	/**
	 *
	 */
	public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
	public static final String MOUNTPOINTS = "mountpoints";
	public static final String MOUNTPOINT = "mountpoint";
	public static final String ID = "id";
	public static final String CAPABILITY = "capability";
	public static final String CAPABILITIES = "capabilities";
	public static final String COMMIT = "commit";

	public static Element readXmlToElement(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
		Document doc = readXmlToDocument(xmlContent);
		return doc.getDocumentElement();
	}

	public static Element readXmlToElement(InputStream xmlContent) throws ParserConfigurationException, SAXException, IOException {
		Document doc = readXmlToDocument(xmlContent);
		return doc.getDocumentElement();
	}

	public static Document readXmlToDocument(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
		return readXmlToDocument(new ByteArrayInputStream(xmlContent.getBytes("utf-8")));
	}

	public static Document readXmlToDocument(InputStream xmlContent) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = getDocumentBuilderFactory();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Document doc = dBuilder.parse(new InputSource(xmlContent));

		doc.getDocumentElement().normalize();
		return doc;
	}

	public static Element readXmlToElement(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = getDocumentBuilderFactory();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);

		doc.getDocumentElement().normalize();

		return doc.getDocumentElement();
	}

	private static final DocumentBuilderFactory getDocumentBuilderFactory() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setCoalescing(true);
		// factory.setValidating(true);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		return factory;
	}

	public static Document getDocument() {
		DocumentBuilderFactory factory = getDocumentBuilderFactory();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			return document;
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static Element createTextElement(Document document, String name, String content) {
		Element typeElement = document.createElement(name);
		typeElement.appendChild(document.createTextNode(content));
		return typeElement;
	}

	public static void addNamespaceAttr(Element root, String namespace) {
		root.setAttribute(XMLNS_ATTRIBUTE_KEY, namespace);
	}

	public static void addPrefixedNamespaceAttr(Element root, String prefix, String namespace) {
		root.setAttribute(concat(XMLNS_ATTRIBUTE_KEY, prefix), namespace);
	}

	public static Element createPrefixedTextElement(Document document, String key, String prefix, String moduleName) {
		return createTextElement(document, key, concat(prefix, moduleName));
	}

	private static String concat(String prefix, String value) {
		return prefix + ":" + value;
	}

	public static String toString(Document document) {
		return toString(document.getDocumentElement());
	}

	public static String toString(Element xml) {
		return toString(xml, false);
	}

	public static String toString(XmlElement xmlElement) {
		return toString(xmlElement.getDomElement(), false);
	}

	public static String toString(Element xml, boolean addXmlDeclaration) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, addXmlDeclaration == true ? "no" : "yes");

			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(xml);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			return xmlString;
		} catch (IllegalArgumentException | TransformerFactoryConfigurationError | TransformerException e) {
			throw new RuntimeException("Unable to serialize xml element " + xml, e);
		}
	}

	// XML keys common
	//
	public static final String NAME_KEY = "name";
	public static final String TYPE_KEY = "type";

	public static final String MODULE_KEY = "module";
	public static final String INSTANCE_KEY = "instance";
	public static final String OPERATION_ATTR_KEY = "operation";
	public static final String SERVICES_KEY = "services";
	public static final String CONFIG_KEY = "config";
	public static final String MODULES_KEY = "modules";
	public static final String CONFIGURATION_KEY = "configuration";
	public static final String DATA_KEY = "data";
	public static final String OK = "ok";
	public static final String FILTER = "filter";
	public static final String SOURCE_KEY = "source";
	public static final String RPC_KEY = "rpc";
	public static final String RPC_REPLY_KEY = "rpc-reply";
	public static final String RPC_ERROR = "rpc-error";

	public static String toString(Document doc, boolean addXmlDeclaration) {
		return toString(doc.getDocumentElement(), addXmlDeclaration);
	}
}
