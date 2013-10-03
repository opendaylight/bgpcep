/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Iterator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;

/**
 * In order for a model-driven RIB implementation to work correctly, it has
 * to know how to handle individual NLRI fields, whose encoding is specific
 * to a AFI/SAFI pair. The specific methods needed are encapsulated in this
 * interface and the entity providing the model extension is expected to
 * register an implementation of this interface for each AFI/SAFI pair it is
 * capable of handling.
 * 
 * @param <KEY> Destination key type
 */
public interface NLRIHandler<KEY> {
	public Iterator<KEY> getKeys(MpReachNlri nlri);
	public Iterator<KEY> getKeys(MpUnreachNlri nlri);
}
