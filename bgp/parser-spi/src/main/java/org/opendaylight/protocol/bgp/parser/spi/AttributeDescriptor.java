/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

/**
 * Common interface for path attribute descriptors <attribute type, attribute length, attribute value> of
  */
public interface AttributeDescriptor {

    public int getType();
    public int getFlags();
    public int getLength();

}
