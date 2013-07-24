/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

/**
 * Enumerable representing ObjectiveFunction codes. Defined in RFC5541.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5541#section-4">Objective
 *      Functions Definition</a>
 * @see <a href="http://tools.ietf.org/html/rfc6006#section-3.6.1">New Objective
 *      Functions [RFC6006]</a>
 */
public enum PCEPOFCodes {
	/**
	 * Minimum Cost Path
	 */
	MCP,
	/**
	 * Minimum Load Path
	 */
	MLP,
	/**
	 * Maximum residual Bandwidth Path
	 */
	MBP,
	/**
	 * Minimize aggregate Bandwidth Consumption
	 */
	MBC,
	/**
	 * Minimize the load of the Most Loaded Link
	 */
	MLL,
	/**
	 * Minimize Cumulative Cost of a set of paths
	 */
	MCC,
	/**
	 * Name: Shortest Path Tree (SPT)
	 * 
	 * Description: Minimize the maximum source-to-leaf cost with respect to a
	 * specific metric or to the TE metric used as the default metric when the
	 * metric is not specified (e.g., TE or IGP metric).
	 */
	SPT,
	/**
	 * Name: Minimum Cost Tree (MCT)
	 * 
	 * Description: Minimize the total cost of the tree, that is the sum of the
	 * costs of tree links, with respect to a specific metric or to the TE
	 * metric used as the default metric when the metric is not specified.
	 */
	MCT;
}
