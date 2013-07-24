/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.framework.SessionPreferencesChecker;

/**
 * Interface to work with session characteristics. They need to be
 * checked during the PCEP establishment phase. If they are not
 * acceptable a new proposal needs to be requested.
 */
public abstract class PCEPSessionProposalChecker implements SessionPreferencesChecker {

	/**
	 * Checks session characteristics, if they are acceptable.
	 *
	 * @param openObj
	 *            storage for session characteristics
	 * @return true = acceptable, false = negotiable, null = unacceptable
	 */
	@Override
	public abstract Boolean checkSessionCharacteristics(SessionPreferences openObj);

	/**
	 * In case of negotiable session characteristics, new ones are requested
	 * through this method.
	 *
	 * @param open old open object with unacceptable session characteristics
	 * @return
	 * 	<li> new session characteristics wrapped in Open Object
	 * 	<li> null if there are not available any different acceptable
	 * session characteristics
	 */
	public abstract PCEPSessionPreferences getNewProposal(SessionPreferences open);

}
