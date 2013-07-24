/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.net.InetAddress;

import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

/**
 *
 */
public class TestingSessionListenerFactory extends PCEPSessionListenerFactory {

	private final String autoResponseMessagesSrc;
	private final String periodicallySendMessagesSrc;
	private final String sendNowMessageSrc;
	private final int period;

	public TestingSessionListenerFactory(String autoResponseMessagesSrc, String periodicallySendMessagesSrc, int period, String sendNowMessageSrc) {
		this.autoResponseMessagesSrc = autoResponseMessagesSrc;
		this.periodicallySendMessagesSrc = periodicallySendMessagesSrc;
		this.period = period;
		this.sendNowMessageSrc = sendNowMessageSrc;
	}

	@Override
	public PCEPSessionListener getSessionListener(InetAddress address) {
		return new TestingSessionListener(this.autoResponseMessagesSrc, this.periodicallySendMessagesSrc, this.period, this.sendNowMessageSrc);
	}

}
