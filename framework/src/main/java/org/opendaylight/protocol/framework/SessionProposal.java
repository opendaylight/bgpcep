/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Interface that provides the initial acceptable session characteristics
 * with which the session should be started. This interface should be
 * implemented by a protocol specific abstract class, that is extended by
 * a final class that implements the methods.
 */
public interface SessionProposal {

	/**
	 * Returns specific Session Preferences object for this IP address.
	 * @return SessionPreferences DTO with acceptable session characteristics
	 */
	public SessionPreferences getProposal();
}
