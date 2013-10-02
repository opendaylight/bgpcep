/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations;

import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.w3c.dom.Element;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ValidateTest {

	public static final String NETCONF_SESSION_ID_FOR_REPORTING = "foo";

	@Test(expected = NetconfDocumentedException.class)
	public void test() throws Exception {
		final XmlElement xml = XmlElement.fromString("<abc></abc>");
		final Validate validate = new Validate(null, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test(expected = NetconfDocumentedException.class)
	public void testNoSource() throws Exception {
		final XmlElement xml = XmlElement.fromString("<validate xmlns=\"" + XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0 + "\"/>");
		final Validate validate = new Validate(null, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test(expected = NetconfDocumentedException.class)
	public void testNoNamespace() throws Exception {
		final XmlElement xml = XmlElement.fromString("<validate/>");
		final Validate validate = new Validate(null, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test(expected = NetconfDocumentedException.class)
	public void testRunningSource() throws Exception {

		final XmlElement xml = XmlElement.fromString("<validate xmlns=\"" + XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
				+ "\"><source><running></running></source></validate>");
		final Validate validate = new Validate(null, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test(expected = NetconfDocumentedException.class)
	public void testNoTransaction() throws Exception {
		final XmlElement xml = XmlElement.fromString("<validate xmlns=\"" + XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
				+ "\"><source><candidate/></source></validate>");
		final TransactionProvider transactionProvider = mock(TransactionProvider.class);
		doThrow(IllegalStateException.class).when(transactionProvider).validateTransaction();
		final Validate validate = new Validate(transactionProvider, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test(expected = NetconfDocumentedException.class)
	public void testValidationException() throws Exception {
		final XmlElement xml = XmlElement.fromString("<validate xmlns=\"" + XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
				+ "\">><source><candidate/></source></validate>");
		final TransactionProvider transactionProvider = mock(TransactionProvider.class);
		doThrow(ValidationException.class).when(transactionProvider).validateTransaction();
		final Validate validate = new Validate(transactionProvider, null, NETCONF_SESSION_ID_FOR_REPORTING);
		validate.handle(null, xml);
	}

	@Test
	public void testValidation() throws Exception {
		final XmlElement xml = XmlElement.fromString("<validate xmlns=\"" + XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
				+ "\"><source><candidate/></source></validate>");
		final TransactionProvider transactionProvider = mock(TransactionProvider.class);
		final Element okElement = Xml.readXmlToElement("<ok/>");
		doNothing().when(transactionProvider).validateTransaction();
		final Validate validate = new Validate(transactionProvider, null, NETCONF_SESSION_ID_FOR_REPORTING);
		Element ok = validate.handle(XMLUtil.newDocument(), xml);
		assertEquals(Xml.toString(okElement), Xml.toString(ok));
	}

}
