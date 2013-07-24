/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * As in order to work with SSLSelectionKey, a channel needs to implement
 * this interface. It is used to determine SSL progress and update interestOps
 * of underlying channel.
 */
interface SSLSelectableChannel {
	/**
	 * Return the events which the underlying channel should be selected for,
	 * based on what the user would like to see from us.
	 * 
	 * @param ops user's interest ops
	 * @return Calculated interest ops, including internal needs
	 */
	public int computeInterestOps(final int ops);

	/**
	 * Return a freshly-calculated operations which the channel is ready to
	 * make progress on.
	 * 
	 * @return Calculated ready ops
	 */
	public int computeReadyOps();
}

