/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.Serializable;

/**
 * Marker interface. Serves as a general concept of protocol message, smallest unit of
 * information exchanged in a protocol. Classes in common-protocol work only with this
 * type of message, so that the rest of the module can be used without knowing specifics
 * of underlying protocol. Each implemented protocol either has some abstract class in
 * its API that represents abstract protocol specific message and implements this interface
 * or has only specific protocol messages and uses this interface directly.
 *
 * Example:
 *
 * public abstract SpecificProtocolMessage implements ProtocolMessage { .. }
 *
 * public class SpecificOpenMessage extends SpecificProtocolMessage { .. }
 */
public interface ProtocolMessage extends Serializable {

}
