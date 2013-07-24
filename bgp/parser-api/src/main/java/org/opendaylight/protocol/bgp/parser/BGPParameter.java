/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

/**
 * Marker interface of each BGP Parameter. Currently we have only Capabilities parameter.
 */
public interface BGPParameter {

	/**
	 * Returns fixed type of the parameter.
	 * 
	 * @return type of the parameter
	 */
	public int getType();
}
