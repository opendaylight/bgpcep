/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.io.IOException;

public interface PCEPSessionRuntimeMXBean {
	//TODO remove once operations are generated

	Integer getDeadTimerValue();

	Integer getKeepAliveTimerValue();

	Integer getReceivedMsgCount();

	Integer getSentMsgCount();

	String getPeerAddress();

	String getNodeIdentifier();

	void tearDown() throws IOException;
}
