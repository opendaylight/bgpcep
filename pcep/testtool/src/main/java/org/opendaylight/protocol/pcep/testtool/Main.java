/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.DispatcherImpl;
import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPConnectionFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.PCEPSessionProposalCheckerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.PCEPConnectionImpl;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPMessageFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionProposalCheckerFactoryImpl;
import org.opendaylight.protocol.pcep.impl.PCEPSessionProposalFactoryImpl;

public class Main {

	public static String usage = "DESCRIPTION:\n" + "\tCreates a server with given parameters. As long as it runs, it accepts connections "
			+ "from PCCs.\n" + "USAGE:\n" + "\t-a, --address\n" + "\t\tthe ip address to which is this server bound.\n"
			+ "\t\tFormat: x.x.x.x:y where y is port number.\n\n" +

			"\t-d, --deadtimer\n" + "\t\tin seconds, value of the desired deadtimer\n"
			+ "\t\tAccording to RFC5440, recommended value for deadtimer is 4 times the value\n"
			+ "\t\tof KeepAlive timer. If it's not, a warning is printed.\n"
			+ "\t\tIf not set, it's value will be derived from KeepAlive timer value.\n\n" +

			"\t-ka, --keepalive\n" + "\t\tin seconds, value of the desired KeepAlive timer.\n"
			+ "\t\tIf not present, KeepAlive timer will be set to recommended value (30s).\n\n" +

			"\t--stateful\n" + "\t\tpassive stateful\n\n" +

			"\t--active\n" + "\t\tactive stateful (implies --stateful)\n\n" +

			"\t--versioned\n" + "\t\tversioned stateful (implies --stateful)\n\n" +

			"\t--instant\n"
			+ "\t\tinstantiated stateful, <seconds> cleanup timeout (default value, if not included = 0) (implies --stateful)\n\n" +

			"\t-arm, --autoResponseMessages <path to file>\n"
			+ "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
			+ "\t\t Messages are used as auto response for every message received. Purely for testing puposes! \n\n" +

			"\t-psm, --periodicallySendMessages <path to file> <period>\n"
			+ "\t\t <path to file> with groovy script which implements MessageGeneratorService followed by <period> in seconds.\n"
			+ "\t\t Messages which are sent periodically. Purely for testing puposes! \n\n" +

			"\t-snm, --sendNowMessage <path to file>\n"
			+ "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
			+ "\t\t Messages are sent in defined states defined by programmer. Purely for testing puposes! \n\n" +

			"\t--help\n" + "\t\tdisplay this help and exits\n\n" +

			"With no parameters, this help is printed.";

	public static void main(final String[] args) throws IOException {
		if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("--help"))) {
			System.out.println(Main.usage);
			return;
		}

		InetSocketAddress address = null;
		int keepAliveValue = 30;
		int deadTimerValue = 0;
		boolean stateful = false;
		boolean active = false;
		boolean versioned = false;
		boolean instant = false;
		String autoResponseMessagesSrc = null;
		String sendNowMessageSrc = null;
		String periodicallySendMessagesSrc = null;
		int period = 0;
		int timeout = 0;

		int i = 0;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--address")) {
				final String[] ip = args[i + 1].split(":");
				address = new InetSocketAddress(InetAddress.getByName(ip[0]), Integer.valueOf(ip[1]));
				i++;
			} else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--deadtimer")) {
				deadTimerValue = Integer.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equalsIgnoreCase("-ka") || args[i].equalsIgnoreCase("--keepalive")) {
				keepAliveValue = Integer.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equalsIgnoreCase("--stateful")) {
				stateful = true;
			} else if (args[i].equalsIgnoreCase("--active")) {
				stateful = true;
				active = true;
			} else if (args[i].equalsIgnoreCase("--versioned")) {
				stateful = true;
				versioned = true;
			} else if (args[i].equalsIgnoreCase("--instant")) {
				stateful = true;
				instant = true;
				if (Integer.valueOf(args[i + 1]) > 0 && Integer.valueOf(args[i + 1]) < Integer.MAX_VALUE) {
					timeout = Integer.valueOf(args[i + 1]);
					i++;
				}
			} else if (args[i].equalsIgnoreCase("--autoResponseMessages") || args[i].equalsIgnoreCase("-arm")) {
				autoResponseMessagesSrc = args[i + 1];
				i++;
			} else if (args[i].equalsIgnoreCase("--periodicallySendMessages") || args[i].equalsIgnoreCase("-psm")) {
				periodicallySendMessagesSrc = args[i + 1];
				i++;
				period = Integer.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equalsIgnoreCase("--sendNowMessage") || args[i].equalsIgnoreCase("-snm")) {
				sendNowMessageSrc = args[i + 1];
				i++;
			} else {
				System.out.println("WARNING: Unrecognized argument: " + args[i]);
			}
			i++;
		}
		if (deadTimerValue != 0 && deadTimerValue != keepAliveValue * 4) {
			System.out.println("WARNING: The value of DeadTimer should be 4 times the value of KeepAlive.");
		}
		if (deadTimerValue == 0) {
			deadTimerValue = keepAliveValue * 4;
		}

		final PCEPSessionListenerFactory slf = new TestingSessionListenerFactory(autoResponseMessagesSrc, periodicallySendMessagesSrc, period, sendNowMessageSrc);
		final PCEPSessionProposalFactory spf = new PCEPSessionProposalFactoryImpl(deadTimerValue, keepAliveValue, stateful, active, versioned, instant, timeout);
		final PCEPSessionProposalCheckerFactory spcf = new PCEPSessionProposalCheckerFactoryImpl();

		final PCEPSessionProposal prefs = spf.getSessionProposal(address, 0);

		final DispatcherImpl d = new DispatcherImpl(new PCEPMessageFactory());
		final PCEPDispatcherImpl dispatcher = new PCEPDispatcherImpl(d, spf);

		try {
			dispatcher.createServer(address, new PCEPConnectionFactory() {
				@Override
				public PCEPConnection createProtocolConnection(final InetSocketAddress address) {
					final PCEPSessionProposalChecker checker = spcf.getPreferencesChecker(address);
					final PCEPSessionListener lsnr = slf.getSessionListener(address.getAddress());

					return new PCEPConnectionImpl(address, lsnr, prefs.getProposal(), checker);
				}

				@Override
				public void setProposal(final PCEPSessionProposalFactory proposals, final InetSocketAddress address, final int sessionId) {
				}
			});
			// final ProtocolServer s = dispatcher.createServer(address, slf, spf, spcf);

			// try {
			// Thread.sleep(10000);
			// } catch (final InterruptedException e) {
			// e.printStackTrace();
			// }
			//
			// s.close();

		} finally {
			((PCEPSessionProposalCheckerFactoryImpl) spcf).close();
			// d.stop();
		}
	}
}
