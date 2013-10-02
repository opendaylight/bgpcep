/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl.mapping.operations;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfOperationRouter;
import org.opendaylight.netconf.mapping.api.HandlingPriority;
import org.opendaylight.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCloseSession extends AbstractNetconfOperation {
	public static final String CLOSE_SESSION = "close-session";

	public DefaultCloseSession(String netconfSessionIdForReporting) {
		super(netconfSessionIdForReporting);
	}

	@Override
	protected HandlingPriority canHandle(String operationName, String netconfOperationNamespace) {
		if (operationName.equals(CLOSE_SESSION) == false)
			return HandlingPriority.CANNOT_HANDLE;
		if (netconfOperationNamespace.equals(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0) == false)
			return HandlingPriority.CANNOT_HANDLE;

		return HandlingPriority.HANDLE_WITH_MAX_PRIORITY;
	}

	/**
	 * Close netconf operation router associated to this session, which in turn closes NetconfOperationServiceSnapshot
	 * with all NetconfOperationService instances
	 */
	@Override
	protected Element handle(Document document, XmlElement operationElement, NetconfOperationRouter opRouter)
			throws NetconfDocumentedException {
		opRouter.close();
		return document.createElement(Xml.OK);
	}
}
