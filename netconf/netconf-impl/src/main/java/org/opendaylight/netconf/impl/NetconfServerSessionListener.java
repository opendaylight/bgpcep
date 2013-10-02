/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import static com.google.common.base.Preconditions.checkState;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfTerminationReason;
import org.opendaylight.netconf.impl.osgi.NetconfOperationRouterImpl;
import org.opendaylight.netconf.util.SendErrorExceptionUtil;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class NetconfServerSessionListener implements SessionListener<NetconfMessage, NetconfServerSession, NetconfTerminationReason> {

	static final Logger logger = LoggerFactory.getLogger(NetconfServerSessionListener.class);
	public static final String MESSAGE_ID = "message-id";

	private NetconfOperationRouterImpl operationRouter;

	public NetconfServerSessionListener(NetconfOperationRouterImpl operationRouter) {
		this.operationRouter = operationRouter;
	}

	@Override
	public void onSessionUp(NetconfServerSession netconfNetconfServerSession) {

	}

	@Override
	public void onSessionDown(NetconfServerSession netconfNetconfServerSession, Exception e) {
		logger.debug("Session {} down, reason: {}", netconfNetconfServerSession, e.getMessage());

		operationRouter.close();
	}

	@Override
	public void onSessionTerminated(NetconfServerSession netconfNetconfServerSession, NetconfTerminationReason netconfTerminationReason) {
		logger.debug("Session {} terminated, reason: {}", netconfNetconfServerSession, netconfTerminationReason.getErrorMessage());

		operationRouter.close();
	}

	@Override
	public void onMessage(NetconfServerSession session, NetconfMessage netconfMessage) {
		try {

			Preconditions.checkState(operationRouter != null, "Cannot handle message, session up was not yet received");
			// FIXME: there is no validation since the document may contain yang schemas
			final NetconfMessage message = processDocument(netconfMessage);
			logger.debug("Respondign with message {}", Xml.toString(message.getDocument()));
			session.sendMessage(message);

			if (isCloseSession(netconfMessage)) {
				closeNetconfSession(session);
			}

		} catch (final RuntimeException e) {
			logger.error("Unexpected exception", e);
			// TODO: should send generic error or close session?
			throw new RuntimeException("Unable to process incoming message " + netconfMessage, e);
		} catch (NetconfDocumentedException e) {
			SendErrorExceptionUtil.sendErrorMessage(session, e, netconfMessage);
		}
	}

	private void closeNetconfSession(NetconfServerSession session) {
		// destroy NetconfOperationService
		session.close();
		logger.info("Session {} closed successfully", session.getSessionId());
	}

	private NetconfMessage processDocument(final NetconfMessage netconfMessage) throws NetconfDocumentedException {

		final Document incommingDocument = netconfMessage.getDocument();
		final Node rootNode = incommingDocument.getDocumentElement();

		if (rootNode.getNodeName().equals(Xml.RPC_KEY)) {
			final String messageId = rootNode.getAttributes().getNamedItem(MESSAGE_ID).getTextContent();
			checkState(messageId != null);
			final Document responseDocument = XMLUtil.newDocument();
			Document rpcReply = operationRouter.onNetconfMessage(incommingDocument);
			responseDocument.appendChild(responseDocument.importNode(rpcReply.getDocumentElement(), true));
			return new NetconfMessage(responseDocument);
		} else {
			// unknown command, send RFC 4741 p.70 unknown-element
			/*
			Tag:         unknown-element
			Error-type:  rpc, protocol, application
			Severity:    error
			Error-info:  <bad-element> : name of the unexpected element
			Description: An unexpected element is present.
			*/
			// TODO add message to error info
			throw new NetconfDocumentedException("Unknown tag " + rootNode.getNodeName(), NetconfDocumentedException.ErrorType.protocol, NetconfDocumentedException.ErrorTag.unknown_element, NetconfDocumentedException.ErrorSeverity.error, ImmutableMap.of(
					"bad-element", rootNode.getNodeName()));
		}
	}

	private static boolean isCloseSession(final NetconfMessage incommingDocument) {
		final Document document = incommingDocument.getDocument();
		XmlElement rpcElement = XmlElement.fromDomDocument(document);
		if (rpcElement.getOnlyChildElementOptionally("close-session").isPresent())
			return true;

		return false;
	}
}
