/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.identity;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.integration.pcep.AbstractPcepOsgiTest;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;

@RunWith(PaxExam.class)
@ExamReactorStrategy(org.ops4j.pax.exam.spi.reactors.PerClass.class)
public class IdentityRefNetconfTest extends AbstractIdentityTest {

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 18383);


    @Test
	public void testIdRef() throws Exception {
        NioEventLoopGroup nettyThreadgroup = new NioEventLoopGroup();
        Thread.sleep(20 * 1000);
        NetconfClientDispatcher clientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup);


        NetconfMessage edit = xmlFileToNetconfMessage("netconfMessages/editConfig_identities.xml");
        NetconfMessage commit = xmlFileToNetconfMessage("netconfMessages/commit.xml");

        try (NetconfClient netconfClient = new NetconfClient("client", tcpAddress, 4000, clientDispatcher)) {
            NetconfMessage response = netconfClient.sendMessage(edit);
            response = netconfClient.sendMessage(commit);

            // Error message is misleading, actual error is DependencyResolverImpl(line: 179) getting null from IdentityCodec
            Assert.assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<ok/>"));
        }

        clientDispatcher.close();
	}


    public static NetconfMessage xmlFileToNetconfMessage(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        return new NetconfMessage(xmlFileToDocument(fileName));
    }

    public static Document xmlFileToDocument(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        try (InputStream resourceAsStream = IdentityRefNetconfTest.class.getClassLoader().getResourceAsStream(fileName)) {
            Preconditions.checkNotNull(resourceAsStream);
            final Document doc = XmlUtil.readXmlToDocument(resourceAsStream);
            return doc;
        }
    }
}
