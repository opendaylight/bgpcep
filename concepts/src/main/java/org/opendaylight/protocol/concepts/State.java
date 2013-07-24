/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.io.Serializable;

/**
 * Interface identifying a piece of state information. Implementations of this interface are required to be immutable.
 * It is also recommended for the projects to be eligible (and carry) the @Immutable JSR-305 annotation.
 */
public interface State extends Immutable, Serializable {
	/**
	 * Report a string representation of the state. The interface contract of this method, unlike its normal Object
	 * ancestor, its return value must be consistent with the equals() method, such that the following always holds:
	 * 
	 * o1.toString().equals(o2.toString()) == o1.equals(o2)
	 * 
	 * @return String representation of the state information
	 */
	@Override
	public String toString();
}
