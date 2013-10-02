/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl.mapping.operations;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfOperationRouter;
import org.opendaylight.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.netconf.mapping.api.HandlingPriority;
import org.opendaylight.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public final class DefaultGetSchema extends AbstractNetconfOperation {

	private final CapabilityProvider cap;

	private static final Logger logger = LoggerFactory.getLogger(DefaultGetSchema.class);

	public DefaultGetSchema(CapabilityProvider cap, String netconfSessionIdForReporting) {
		super(netconfSessionIdForReporting);
		this.cap = cap;
	}

	public static final String GET_SCHEMA = "get-schema";
	public static final String IDENTIFIER = "identifier";
	public static final String VERSION = "version";

	@Override
	protected HandlingPriority canHandle(String netconfOperationName, String namespace) {
		if (netconfOperationName.equals("get-schema") == false)
			return HandlingPriority.CANNOT_HANDLE;
		if (namespace.equals(XMLUtil.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING) == false)
			return HandlingPriority.CANNOT_HANDLE;

		return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
	}

	@Override
	protected Element handle(Document document, XmlElement xml, NetconfOperationRouter router) throws NetconfDocumentedException {
		GetSchemaEntry entry;

		try {
			entry = new GetSchemaEntry(xml);
		} catch (final IllegalArgumentException e) {
			logger.warn("Error parsing xml", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(NetconfDocumentedException.ErrorTag.bad_attribute.name(), e.getMessage());
			throw new NetconfDocumentedException(e.getMessage(), e, NetconfDocumentedException.ErrorType.rpc, NetconfDocumentedException.ErrorTag.bad_attribute, NetconfDocumentedException.ErrorSeverity.error, errorInfo);
		} catch (final IllegalStateException e) {
			logger.warn("Error parsing xml", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(NetconfDocumentedException.ErrorTag.bad_attribute.name(), e.getMessage());
			throw new NetconfDocumentedException(e.getMessage(), e, NetconfDocumentedException.ErrorType.rpc, NetconfDocumentedException.ErrorTag.bad_attribute, NetconfDocumentedException.ErrorSeverity.error, errorInfo);
		}

		String schema;
		try {
			schema = cap.getSchemaForCapability(entry.identifier, entry.version);
		} catch (IllegalStateException e) {
			Map<String, String> errorInfo = Maps.newHashMap();
			errorInfo.put(entry.identifier, e.getMessage());
			logger.warn("Rpc error: {}", NetconfDocumentedException.ErrorTag.operation_failed, e);
			throw new NetconfDocumentedException(e.getMessage(), NetconfDocumentedException.ErrorType.application, NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorSeverity.error, errorInfo);
		}

		Element getSchemaResult;
		getSchemaResult = Xml.createTextElement(document, Xml.DATA_KEY, schema);
		Xml.addNamespaceAttr(getSchemaResult, XMLUtil.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING);

		logger.info("{} operation successful", GET_SCHEMA);

		return getSchemaResult;
	}

	private static final class GetSchemaEntry {
		private final String identifier;
		private final Optional<String> version;

		GetSchemaEntry(XmlElement getSchemaElement) {
			getSchemaElement.checkName(GET_SCHEMA);
			getSchemaElement.checkNamespace(XMLUtil.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING);

			XmlElement identifierElement = getSchemaElement.getOnlyChildElementWithSameNamespace(IDENTIFIER);
			identifier = identifierElement.getTextContent();
			Optional<XmlElement> versionElement = getSchemaElement.getOnlyChildElementWithSameNamespaceOptionally(VERSION);
			if (versionElement.isPresent()) {
				version = Optional.of(versionElement.get().getTextContent());
			} else {
				version = Optional.absent();
			}

		}
	}
}
