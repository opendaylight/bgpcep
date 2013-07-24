/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import java.io.Serializable;

import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.protocol.concepts.NamedObject;
import org.opendaylight.protocol.concepts.Stateful;

/**
 * Class representing an generic object living in a network. Each such object
 * has a name which uniquely identifies it in a particular network. Further
 * generic attributes provide view into how a particular object has been tagged
 * in BGP world.
 * 
 * @param <T>
 */
public interface NetworkObject<T extends Identifier> extends NamedObject<T>, Serializable, Stateful<NetworkObjectState> {
}

