/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 *
 */
public class SimpleSessionProposalChecker extends PCEPSessionProposalChecker {

	@Override
	public Boolean checkSessionCharacteristics(SessionPreferences openObj) {
		return true;
	}

	@Override
	public PCEPSessionPreferences getNewProposal(SessionPreferences open) {
		return new PCEPSessionPreferences(new PCEPOpenObject(1, 1, 0, null));
	}

}
