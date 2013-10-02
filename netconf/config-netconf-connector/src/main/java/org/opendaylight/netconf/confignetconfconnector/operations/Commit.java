/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations;

import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

public class Commit extends AbstractConfigNetconfOperation {

	private static final Logger logger = LoggerFactory.getLogger(Commit.class);

	private final TransactionProvider transactionProvider;

	public Commit(TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient, String netconfSessionIdForReporting) {
		super(configRegistryClient, netconfSessionIdForReporting);
		this.transactionProvider = transactionProvider;
	}

	private static void checkXml(XmlElement xml) {
		xml.checkName(Xml.COMMIT);
		xml.checkNamespace(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
	}

	@Override
	protected String getOperationName() {
		return Xml.COMMIT;
	}

	@Override
	protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {
		checkXml(xml);

		CommitStatus status;
		try {
			status = this.transactionProvider.commitTransaction();
		} catch (final IllegalStateException e) {
			logger.warn("Commit failed: ", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.operation_failed.name(),
					"Operation failed. Use 'get-config' or 'edit-config' before triggering 'commit' operation");
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
		} catch (final NetconfDocumentedException e) {
			throw new NetconfDocumentedException("Unable to retrieve config snapshot after commit for persister, details: "
					+ e.getMessage(), ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error, e.getErrorInfo());
		}
		logger.info("Datastore {} committed successfully: {}", Datastore.candidate, status);

		return document.createElement(Xml.OK);
	}

}
