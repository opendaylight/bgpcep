/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

public interface PCEPExtensionConsumerContext {

	LabelRegistry getLabelHandlerRegistry();

	MessageHandlerRegistry getMessageHandlerRegistry();

	ObjectRegistry getObjectHandlerRegistry();

	EROSubobjectRegistry getEROSubobjectHandlerRegistry();

	RROSubobjectRegistry getRROSubobjectHandlerRegistry();

	XROSubobjectRegistry getXROSubobjectHandlerRegistry();

	TlvRegistry getTlvHandlerRegistry();
}
