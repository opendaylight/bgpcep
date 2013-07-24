/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Interface to work with session preferences. They need to be
 * checked during the establishment phase. If they are not
 * acceptable a new proposal needs to be requested.
 * This interface should be implemented by a protocol specific
 * abstract class, that is extended by a final class that implements
 * the methods.
 */
public interface SessionPreferencesChecker {

	/**
	 * Checks session characteristics, if they are acceptable.
	 *
	 * @param openObj
	 *            storage for session characteristics
	 * @return true = acceptable, false = negotiable, null = unacceptable
	 * @throws DocumentedException when there is specific protocol error
	 * for rejecting the session characteristics
	 */
	public Boolean checkSessionCharacteristics(final SessionPreferences openObj) throws DocumentedException;

	/**
	 * In case of negotiable session characteristics, new ones are requested
	 * through this method.
	 *
	 * @param oldOpen old open object with unacceptable session characteristics
	 * @return
	 * 	<li> new session characteristics wrapped in Open Object
	 * 	<li> null if there are not available any different acceptable
	 * session characteristics
	 */
	public SessionPreferences getNewProposal(final SessionPreferences oldOpen);
}
