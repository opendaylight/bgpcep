/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util.xml;

import org.opendaylight.netconf.api.NetconfMessage;
import org.w3c.dom.Document;

public class NetconfMessageUtil {

	public static boolean isOKMessage(NetconfMessage message) {
		return isOKMessage(message.getDocument());
	}

	public static boolean isOKMessage(Document document) {
		return isOKMessage(XmlElement.fromDomDocument(document));
	}

	public static boolean isOKMessage(XmlElement xmlElement) {
		return xmlElement.getOnlyChildElement().getName().equals(Xml.OK);
	}

	public static boolean isErrorMEssage(NetconfMessage message) {
		return isErrorMessage(message.getDocument());
	}

	public static boolean isErrorMessage(Document document) {
		return isErrorMessage(XmlElement.fromDomDocument(document));
	}

	public static boolean isErrorMessage(XmlElement xmlElement) {
		return xmlElement.getOnlyChildElement().getName().equals(Xml.RPC_ERROR);

	}
}
