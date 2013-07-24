/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

/**
 * Enumerable for attributes of subobjects. Defined in 5521.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1">Exclude Route
 *      Object definition</a>
 */
public enum XROSubobjectAttribute {
	/**
	 * The subobject is to be interpreted as an interface or set of interfaces.
	 */
	INTERFACE,
	/**
	 * The subobject is to be interpreted as a node or set of nodes.
	 */
	NODE,
	/**
	 * The subobject identifies an SRLG explicitly or indicates all of the SRLGs
	 * associated with the resource or resources identified by the subobject.
	 */
	SRLG;
}
