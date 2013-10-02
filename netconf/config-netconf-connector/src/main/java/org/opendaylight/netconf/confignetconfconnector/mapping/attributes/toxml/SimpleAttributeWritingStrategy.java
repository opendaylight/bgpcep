/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.mapping.attributes.toxml;

import org.opendaylight.netconf.confignetconfconnector.util.Util;
import org.opendaylight.netconf.util.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleAttributeWritingStrategy implements AttributeWritingStrategy {

	private final Document document;
	private final String key;

	/**
	 * @param document
	 * @param key
	 */
	public SimpleAttributeWritingStrategy(Document document, String key) {
		this.document = document;
		this.key = key;
	}

	@Override
	public void writeElement(Element parentElement, String namespace, Object value) {
		Util.checkType(value, String.class);
		Element innerNode = Xml.createTextElement(document, key, (String) value);
		Xml.addNamespaceAttr(innerNode, namespace);
		parentElement.appendChild(innerNode);
	}

}
