/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util.mapping;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfOperationRouter;
import org.opendaylight.netconf.mapping.api.HandlingPriority;
import org.opendaylight.netconf.mapping.api.NetconfOperation;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Map;

public abstract class AbstractNetconfOperation implements NetconfOperation {
	private final String netconfSessionIdForReporting;

	protected AbstractNetconfOperation(String netconfSessionIdForReporting) {
		this.netconfSessionIdForReporting = netconfSessionIdForReporting;
	}

	public String getNetconfSessionIdForReporting() {
		return netconfSessionIdForReporting;
	}

	@Override
	public HandlingPriority canHandle(Document message) {
		OperationNameAndNamespace operationNameAndNamespace = new OperationNameAndNamespace(message);
		return canHandle(operationNameAndNamespace.getOperationName(), operationNameAndNamespace.getNamespace());
	}

	public static class OperationNameAndNamespace {
		private final String operationName, namespace;

		public OperationNameAndNamespace(Document message) {
			XmlElement requestElement = getRequestElementWithCheck(message);

			XmlElement operationElement = requestElement.getOnlyChildElement();
			operationName = operationElement.getName();
			namespace = operationElement.getNamespace();
		}

		public String getOperationName() {
			return operationName;
		}

		public String getNamespace() {
			return namespace;
		}
	}

	protected static XmlElement getRequestElementWithCheck(Document message) {
		return XmlElement.fromDomElementWithExpected(message.getDocumentElement(), Xml.RPC_KEY,
				XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
	}

	protected abstract HandlingPriority canHandle(String operationName, String netconfOperationNamespace);

	@Override
	public Document handle(Document message, NetconfOperationRouter opRouter) throws NetconfDocumentedException {

		XmlElement requestElement = getRequestElementWithCheck(message);

		Document document = XMLUtil.newDocument();

		XmlElement operationElement = requestElement.getOnlyChildElement();
		Map<String, Attr> attributes = requestElement.getAttributes();

		Element response = handle(document, operationElement, opRouter);

		Element rpcReply = document.createElementNS(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, Xml.RPC_REPLY_KEY);
		rpcReply.appendChild(response);

		for (String attrName : attributes.keySet()) {
			rpcReply.setAttribute(attrName, attributes.get(attrName).getNodeValue());
		}

		document.appendChild(rpcReply);
		return document;
	}

	protected abstract Element handle(Document document, XmlElement operationElement, NetconfOperationRouter opRouter)
			throws NetconfDocumentedException;

	@Override
	public String toString() {
		return getClass() + "{" + netconfSessionIdForReporting + '}';
	}
}
