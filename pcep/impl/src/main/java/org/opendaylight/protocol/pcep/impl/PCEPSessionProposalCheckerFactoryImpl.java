/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.PCEPSessionProposalCheckerFactory;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

public class PCEPSessionProposalCheckerFactoryImpl extends
		PCEPSessionProposalCheckerFactory  implements Closeable {

	@Override
	public PCEPSessionProposalChecker getPreferencesChecker(
			final InetSocketAddress address) {
		return new PCEPSessionProposalChecker() {

			@Override
			public Boolean checkSessionCharacteristics(
					final SessionPreferences openObj) {
				return true;
			}

			@Override
			public PCEPSessionPreferences getNewProposal(
					final SessionPreferences open) {
				return new PCEPSessionPreferences(new PCEPOpenObject(30, 120, 0, null));
			}

		};
	}

	@Override
	public void close() throws IOException {
		// nothing to close
	}
}
