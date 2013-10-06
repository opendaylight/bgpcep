/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * 
 * BGP Update Message contains two sets of Objects, ones that have to be removed from the topology and ones that need to
 * be added. Although it is restricted to have the same object in both sets, the implementation needs to handle this
 * kind of situation. Therefore, first step is to remove objects, then add the other set.
 * 
 */
public interface BGPUpdateMessage extends Notification {
	/**
	 * Objects that are identified with Identifiers in this set, need to be removed from topology.
	 * 
	 * @return set of identifiers of objects to be removed
	 */
	public Set<?> getRemovedObjects();

	/**
	 * Set of objects that need to be added to the topology wrapped in BGPObject that includes the identifier and its
	 * attributes.
	 * 
	 * @return set of BGPObjects to be added
	 */
	public Set<BGPObject> getAddedObjects();
}
