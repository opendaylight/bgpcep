/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPConnectionImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalCheckerImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposalChecker;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.framework.DispatcherImpl;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starter class for testing.
 */
public class Main {

	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static String usage = "DESCRIPTION:\n" + "\tCreates a server with given parameters. As long as it runs, it accepts connections "
			+ "from PCCs.\n" + "USAGE:\n" + "\t-a, --address\n" + "\t\tthe ip address to which is this server bound.\n"
			+ "\t\tFormat: x.x.x.x:y where y is port number.\n\n"
			+ "\t\tThis IP address will appear in BGP Open message as BGP Identifier of the server.\n" +

			"\t-as\n" + "\t\t value of AS in the initial open message\n\n" +

			"\t-h, --holdtimer\n" + "\t\tin seconds, value of the desired holdtimer\n"
			+ "\t\tAccording to RFC4271, recommended value for deadtimer is 90 seconds.\n"
			+ "\t\tIf not set, this will be the default value.\n\n" +

			"\t--help\n" + "\t\tdisplay this help and exits\n\n" +

			"With no parameters, this help is printed.";

	BGPDispatcherImpl dispatcher;

	public Main() throws IOException {
		this.dispatcher = new BGPDispatcherImpl(new DispatcherImpl(new BGPMessageFactory()));
	}

	public static void main(final String[] args) throws NumberFormatException, IOException {
		if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("--help"))) {
			System.out.println(Main.usage);
			return;
		}

		InetSocketAddress address = null;
		short holdTimerValue = 90;
		ASNumber as = null;

		int i = 0;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--address")) {
				final String[] ip = args[i + 1].split(":");
				address = new InetSocketAddress(InetAddress.getByName(ip[0]), Integer.valueOf(ip[1]));
				i++;
			} else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--holdtimer")) {
				holdTimerValue = Short.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equalsIgnoreCase("-as")) {
				as = new ASNumber(Long.valueOf(args[i + 1]));
				i++;
			} else {
				System.out.println("WARNING: Unrecognized argument: " + args[i]);
			}
			i++;
		}

		final Main m = new Main();

		final BGPSessionListener sessionListener = new TestingListener((DispatcherImpl) m.dispatcher.getDispatcher());

		final BGPSessionProposalImpl prop = new BGPSessionProposalImpl(holdTimerValue, as, new IPv4Address(InetAddress.getByName("25.25.25.2")));

		final BGPSessionPreferences proposal = prop.getProposal();

		prop.close();

		final BGPSessionProposalChecker checker = new BGPSessionProposalCheckerImpl();

		final ProtocolMessageFactory parser = new BGPMessageFactory();

		logger.debug(address + " " + sessionListener + " " + proposal + " " + checker);

		final InetSocketAddress addr = address;
		m.dispatcher.createClient(new BGPConnectionImpl(addr, sessionListener, proposal, checker), parser,
				new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
	}
}
