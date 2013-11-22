/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Interface to expose BGP specific MessageFactory.
 */
public interface BGPMessageFactory extends ProtocolMessageFactory<Notification> {
	@Override
	public Notification parse(final byte[] bytes) throws BGPParsingException, BGPDocumentedException;
}
