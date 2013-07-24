/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;

import org.opendaylight.protocol.concepts.Stateful;

/**
 * 
 * A common interface for objects that can be added to a topology. It can be either BGPRoute, BGPLink, BGPNode or
 * BGPPrefix.
 * 
 */
public interface BGPObject extends Serializable, Stateful<BaseBGPObjectState> {
}
