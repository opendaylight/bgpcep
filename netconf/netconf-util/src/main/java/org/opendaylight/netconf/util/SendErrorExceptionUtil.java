/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util;

import java.io.InputStream;
import java.util.Map.Entry;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;

public class SendErrorExceptionUtil {
	private static final Logger logger = LoggerFactory.getLogger(SendErrorExceptionUtil.class);

	public static void sendErrorMessage(final NetconfSession session, final NetconfDocumentedException sendErrorException) {
		logger.info("Sending error {}", sendErrorException.getMessage(), sendErrorException);
		final Document errorDocument = createDocument(sendErrorException);
		session.sendMessage(new NetconfMessage(errorDocument));
	}

	public static void sendErrorMessage(Channel channel, NetconfDocumentedException sendErrorException) {
		logger.info("Sending error {}", sendErrorException.getMessage(), sendErrorException);
		final Document errorDocument = createDocument(sendErrorException);
		channel.writeAndFlush(new NetconfMessage(errorDocument));
	}

	public static void sendErrorMessage(NetconfSession session, NetconfDocumentedException sendErrorException,
			NetconfMessage incommingMessage) {
		final Document errorDocument = createDocument(sendErrorException);
		logger.info("Sending error {}", Xml.toString(errorDocument));
		tryToCopyAttributes(incommingMessage.getDocument(), errorDocument, sendErrorException);
		session.sendMessage(new NetconfMessage(errorDocument));
	}

	private static void tryToCopyAttributes(final Document incommingDocument, final Document errorDocument,
			final NetconfDocumentedException sendErrorException) {
		try {
			final Element incommingRpc = incommingDocument.getDocumentElement();
			Preconditions.checkState(incommingRpc.getTagName().equals(Xml.RPC_KEY), "Missing " + Xml.RPC_KEY + " " + "element");

			final Element rpcReply = errorDocument.getDocumentElement();
			Preconditions.checkState(rpcReply.getTagName().equals(Xml.RPC_REPLY_KEY), "Missing " + Xml.RPC_REPLY_KEY + " element");

			final NamedNodeMap incomingAttributes = incommingRpc.getAttributes();
			for (int i = 0; i < incomingAttributes.getLength(); i++) {
				final Attr attr = (Attr) incomingAttributes.item(i);
				// skip namespace
				if (attr.getNodeName().equals(Xml.XMLNS_ATTRIBUTE_KEY))
					continue;
				rpcReply.setAttributeNode((Attr) errorDocument.importNode(attr, true));
			}
		} catch (final Exception e) {
			logger.warn("Unable to copy incomming attributes to {}, returned rpc-error might be invalid for client", sendErrorException, e);
		}
	}

	private static XPathExpression rpcErrorExpression = XMLUtil.compileXPath("/netconf:rpc-reply/netconf:rpc-error");
	private static XPathExpression errorTypeExpression = XMLUtil.compileXPath("netconf:error-type");
	private static XPathExpression errorTagExpression = XMLUtil.compileXPath("netconf:error-tag");
	private static XPathExpression errorSeverityExpression = XMLUtil.compileXPath("netconf:error-severity");

	private static Document createDocument(final NetconfDocumentedException sendErrorException) {

		final InputStream errIS = SendErrorExceptionUtil.class.getResourceAsStream("server_error.xml");
		Document originalErrorDocument;
		try {
			originalErrorDocument = XMLUtil.parse(errIS);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}

		final Document errorDocument = XMLUtil.createDocumentCopy(originalErrorDocument);
		final Node rootNode = errorDocument.getFirstChild();

		final Node rpcErrorNode = (Node) XMLUtil.evaluateXPath(rpcErrorExpression, rootNode, XPathConstants.NODE);

		final Node errorTypeNode = (Node) XMLUtil.evaluateXPath(errorTypeExpression, rpcErrorNode, XPathConstants.NODE);
		errorTypeNode.setTextContent(sendErrorException.getErrorType().getTagValue());

		final Node errorTagNode = (Node) XMLUtil.evaluateXPath(errorTagExpression, rpcErrorNode, XPathConstants.NODE);
		errorTagNode.setTextContent(sendErrorException.getErrorTag().getTagValue());

		final Node errorSeverityNode = (Node) XMLUtil.evaluateXPath(errorSeverityExpression, rpcErrorNode, XPathConstants.NODE);
		errorSeverityNode.setTextContent(sendErrorException.getErrorSeverity().getTagValue());

		if (sendErrorException.getErrorInfo() != null && sendErrorException.getErrorInfo().isEmpty() == false) {
			/*
			<error-info>
				<bad-attribute>message-id</bad-attribute>
				<bad-element>rpc</bad-element>
			</error-info>
			 */
			final Node errorInfoNode = errorDocument.createElementNS(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, "error-info");

			errorInfoNode.setPrefix(rootNode.getPrefix());
			rpcErrorNode.appendChild(errorInfoNode);
			for (final Entry<String, String> errorInfoEntry : sendErrorException.getErrorInfo().entrySet()) {
				final Node node = errorDocument.createElementNS(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, errorInfoEntry.getKey());
				node.setTextContent(errorInfoEntry.getValue());
				errorInfoNode.appendChild(node);
			}

		}
		return errorDocument;
	}

}
