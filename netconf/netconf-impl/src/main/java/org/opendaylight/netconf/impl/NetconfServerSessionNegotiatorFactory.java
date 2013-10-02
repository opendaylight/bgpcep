/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
import org.opendaylight.netconf.impl.util.NetconfUtil;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfServerSessionNegotiatorFactory implements SessionNegotiatorFactory {

	private final Timer timer;

	private static final Document helloMessageTemplate = loadHelloMessageTemplate();
	private final SessionIdProvider idProvider;
	private final NetconfOperationServiceFactoryListener factoriesListener;

	public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationServiceFactoryListener factoriesListener,
			SessionIdProvider idProvider) {
		this.timer = timer;
		this.factoriesListener = factoriesListener;
		this.idProvider = idProvider;
	}

	private static Document loadHelloMessageTemplate() {
		return NetconfUtil.createMessage(NetconfServerSessionNegotiatorFactory.class.getResourceAsStream("/server_hello.xml")).getDocument();
	}

	@Override
	public SessionNegotiator getSessionNegotiator(SessionListenerFactory sessionListenerFactory, Channel channel, Promise promise) {
		long sessionId = idProvider.getNextSessionId();

		NetconfServerSessionPreferences proposal = new NetconfServerSessionPreferences(createHelloMessage(sessionId), sessionId);
		return new NetconfServerSessionNegotiator(proposal, promise, channel, timer, sessionListenerFactory.getSessionListener());
	}

	private static final XPathExpression sessionIdXPath = XMLUtil.compileXPath("/netconf:hello/netconf:session-id");
	private static final XPathExpression capabilitiesXPath = XMLUtil.compileXPath("/netconf:hello/netconf:capabilities");

	private NetconfMessage createHelloMessage(long sessionId) {
		Document helloMessageTemplate = getHelloTemplateClone();

		// change session ID
		final Node sessionIdNode = (Node) XMLUtil.evaluateXPath(sessionIdXPath, helloMessageTemplate, XPathConstants.NODE);
		sessionIdNode.setTextContent(String.valueOf(sessionId));

		// add capabilities from yang store
		final Element capabilitiesElement = (Element) XMLUtil.evaluateXPath(capabilitiesXPath, helloMessageTemplate, XPathConstants.NODE);

		CapabilityProvider capabilityProvider = new CapabilityProviderImpl(factoriesListener.getSnapshot(sessionId));

		for (String capability : capabilityProvider.getCapabilities()) {
			final Element capabilityElement = helloMessageTemplate.createElement(Xml.CAPABILITY);
			capabilityElement.setTextContent(capability);
			capabilitiesElement.appendChild(capabilityElement);
		}
		return new NetconfMessage(helloMessageTemplate);
	}

	private synchronized Document getHelloTemplateClone() {
		return (Document) this.helloMessageTemplate.cloneNode(true);
	}
}
