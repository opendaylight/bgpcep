/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.client;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.opendaylight.netconf.api.NetconfSessionPreferences;
import org.opendaylight.netconf.util.NetconfSessionNegotiator;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.opendaylight.protocol.framework.SessionListener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfClientSessionNegotiator extends NetconfSessionNegotiator<NetconfSessionPreferences, NetconfClientSession> {

	protected NetconfClientSessionNegotiator(NetconfSessionPreferences sessionPreferences, Promise<NetconfClientSession> promise,
			Channel channel, Timer timer, SessionListener sessionListener) {
		super(sessionPreferences, promise, channel, timer, sessionListener);
	}

	private static Collection<String> getCapabilities(Document doc) {
		XmlElement responseElement = XmlElement.fromDomDocument(doc);
		XmlElement capabilitiesElement = responseElement.getOnlyChildElementWithSameNamespace(Xml.CAPABILITIES);
		List<XmlElement> caps = capabilitiesElement.getChildElements(Xml.CAPABILITY);
		return Collections2.transform(caps, new Function<XmlElement, String>() {

			@Nullable
			@Override
			public String apply(@Nullable XmlElement input) {
				return input.getTextContent();
			}
		});
	}

	private static final XPathExpression sessionIdXPath = XMLUtil.compileXPath("/netconf:hello/netconf:session-id");

	private long extractSessionId(Document doc) {
		final Node sessionIdNode = (Node) XMLUtil.evaluateXPath(sessionIdXPath, doc, XPathConstants.NODE);
		String textContent = sessionIdNode.getTextContent();
		if (textContent == null || textContent.equals("")) {
			throw new IllegalStateException("Session id not received from server");
		}

		return Long.valueOf(textContent);
	}

	@Override
	protected NetconfClientSession getSession(SessionListener sessionListener, Channel channel, Document doc) {
		return new NetconfClientSession(sessionListener, channel, extractSessionId(doc), getCapabilities(doc));
	}
}
