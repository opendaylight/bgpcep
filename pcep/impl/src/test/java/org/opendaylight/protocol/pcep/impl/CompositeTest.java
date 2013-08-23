/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.Bandwidth;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.CompositePathObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.protocol.pcep.object.CompositeRptPathObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdPathObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.protocol.pcep.object.PCEPClassTypeObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExistingPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.RROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.pcep.tlv.LSPCleanupTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;

public class CompositeTest {

	public PCEPExplicitRouteObject ero;
	public PCEPClassTypeObject ct;
	public PCEPLspaObject lspa;
	public List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
	public PCEPIncludeRouteObject iro = new PCEPIncludeRouteObject(new ArrayList<ExplicitRouteSubobject>() {
		private static final long serialVersionUID = 1L;

		{
			this.add(new EROAsNumberSubobject(new ASNumber(0L), true));
		}
	}, false, false);
	public PCEPRequestParameterObject requestParameter;
	public PCEPNoPathObject noPath;
	public PCEPRequestedPathBandwidthObject bandwidth;

	public List<PCEPRequestParameterObject> requestParameters = new ArrayList<PCEPRequestParameterObject>();
	public PCEPErrorObject error;
	public List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();

	public PCEPNotificationObject notification;
	public List<PCEPNotificationObject> notifications = new ArrayList<PCEPNotificationObject>();

	private PCEPReportedRouteObject reportedRoute;
	private PCEPExistingPathBandwidthObject rroBandwidth;
	private PCEPIncludeRouteObject includeRoute;
	private PCEPLoadBalancingObject loadBalancing;
	private PCEPEndPointsObject<?> endPoints;

	private PCEPLspObject lsp;
	private final List<CompositePathObject> compositePaths = new ArrayList<CompositePathObject>();
	private final List<CompositeRptPathObject> compositeRptPaths = new ArrayList<CompositeRptPathObject>();
	private final List<CompositeUpdPathObject> compositeUpdPaths = new ArrayList<CompositeUpdPathObject>();
	public PCEPReportedRouteObject rro = new PCEPReportedRouteObject(new ArrayList<ReportedRouteSubobject>() {
		private static final long serialVersionUID = 1L;

		{
			this.add(new RROAsNumberSubobject(new ASNumber(0L)));
		}
	}, false);

	@Before
	public void setUp() {
		this.ero = new PCEPExplicitRouteObject(new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(new ASNumber(0L), true));
			}
		}, false);
		this.ct = new PCEPClassTypeObject((short) 5);
		this.lspa = new PCEPLspaObject(0, 0, 0, (short) 0, (short) 0, false, false, false, false);
		this.metrics.add(new PCEPMetricObject(false, false, new TEMetric(1000), false, false));
		this.metrics.add(new PCEPMetricObject(false, false, new TEMetric(1000), false, false));

		this.requestParameter = new PCEPRequestParameterObject(false, false, false, false, false, false, false, false, (short) 0, 0, false, false);
		this.noPath = new PCEPNoPathObject((short) 2, false, false);
		this.bandwidth = new PCEPRequestedPathBandwidthObject(new Bandwidth(0), false, false);

		this.requestParameters.add(this.requestParameter);
		this.requestParameters.add(this.requestParameter);

		this.error = new PCEPErrorObject(PCEPErrors.BANDWIDTH_MISSING);

		this.errors.add(this.error);
		this.errors.add(this.error);
		this.errors.add(this.error);

		this.notification = new PCEPNotificationObject((short) 1, (short) 1);

		this.notifications.add(this.notification);
		this.notifications.add(this.notification);

		final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>();
		eroSubobjects.add(new EROAsNumberSubobject(new ASNumber(0x0L), false));
		eroSubobjects.add(new EROAsNumberSubobject(new ASNumber(0x0L), false));
		eroSubobjects.add(new EROAsNumberSubobject(new ASNumber(0x0L), false));

		final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>();
		rroSubobjects.add(new RROAsNumberSubobject(new ASNumber(0x0L)));
		rroSubobjects.add(new RROAsNumberSubobject(new ASNumber(0x0L)));
		rroSubobjects.add(new RROAsNumberSubobject(new ASNumber(0x0L)));

		this.reportedRoute = new PCEPReportedRouteObject(rroSubobjects, true);
		this.rroBandwidth = new PCEPExistingPathBandwidthObject(new Bandwidth(Float.intBitsToFloat(0xFF)), true, false);
		this.includeRoute = new PCEPIncludeRouteObject(eroSubobjects, true, false);
		this.loadBalancing = new PCEPLoadBalancingObject(0x0, new Bandwidth(Float.intBitsToFloat(0x0)), false);
		final byte[] ipbytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		this.endPoints = new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipbytes), new IPv4Address(ipbytes));

		this.lsp = new PCEPLspObject(0, false, false, true, true, null);
		this.compositePaths.add(new CompositePathObject(new PCEPExplicitRouteObject(eroSubobjects, true), this.lspa, this.bandwidth, this.metrics,
				this.includeRoute));
		this.compositePaths.add(new CompositePathObject(new PCEPExplicitRouteObject(eroSubobjects, true)));

		this.compositeUpdPaths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(eroSubobjects, true), this.lspa, this.bandwidth, this.metrics));
		this.compositeUpdPaths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(eroSubobjects, true)));

	}

	@Test
	public void testCompositePathObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		objects.add(this.ero);
		objects.add(this.lspa);
		objects.add(this.metrics.get(0));
		objects.add(this.metrics.get(1));
		objects.add(this.iro);
		objects.add(new PCEPMetricObject(false, false, new TEMetric(1000), false, false));
		final CompositePathObject path = CompositePathObject.getCompositeFromList(objects);
		assertEquals(path.getExcludedRoute(), this.ero);
		assertEquals(path.getLspa(), this.lspa);
		assertNull(path.getBandwidth());
		assertEquals(path.getMetrics().get(0), this.metrics.get(0));
		assertEquals(path.getMetrics().get(1), this.metrics.get(1));
		assertEquals(path.getIncludeRoute(), this.iro);
	}

	@Test
	public void testCompositeRptPathObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		objects.add(this.ero);
		objects.add(this.lspa);
		objects.add(this.rro);
		objects.add(this.metrics.get(0));
		objects.add(this.metrics.get(1));
		objects.add(new PCEPMetricObject(false, false, new TEMetric(1000), false, false));
		final CompositeRptPathObject path = CompositeRptPathObject.getCompositeFromList(objects);
		assertEquals(path.getExcludedRoute(), this.ero);
		assertEquals(path.getLspa(), this.lspa);
		assertNull(path.getBandwidth());
		assertEquals(path.getMetrics().get(0), this.metrics.get(0));
		assertEquals(path.getMetrics().get(1), this.metrics.get(1));
		assertEquals(path.getReportedRoute(), this.rro);
	}

	@Test
	public void testCompositeResponseObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		objects.add(this.requestParameter);
		objects.add(this.noPath);
		objects.add(this.bandwidth);
		objects.add(this.metrics.get(0));
		objects.add(this.metrics.get(1));
		objects.add(this.iro);
		// add one path
		objects.add(this.ero);
		objects.add(this.lspa);
		objects.add(this.metrics.get(0));
		objects.add(this.metrics.get(1));
		objects.add(this.iro);
		// add another path
		objects.add(this.ero);
		objects.add(this.lspa);
		objects.add(this.metrics.get(0));
		objects.add(new PCEPMetricObject(false, false, new TEMetric(1000), false, false));
		objects.add(this.iro);
		//
		objects.add(this.requestParameter);
		final List<CompositeResponseObject> list = new ArrayList<CompositeResponseObject>();
		while (!objects.isEmpty()) {
			list.add(CompositeResponseObject.getCompositeFromList(objects));
		}
		assertEquals(2, list.size());
		final CompositeResponseObject response = list.get(0);

		assertEquals(response.getRequestParameter(), this.requestParameter);
		assertEquals(response.getNoPath(), this.noPath);
		assertNull(response.getLspa());
		assertEquals(response.getBandwidth(), this.bandwidth);
		assertEquals(response.getMetrics().get(0), this.metrics.get(0));
		assertEquals(response.getMetrics().get(1), this.metrics.get(1));
		assertEquals(response.getIncludeRoute(), this.iro);
		// check path
		CompositePathObject path = response.getPaths().get(0);
		assertEquals(path.getExcludedRoute(), this.ero);
		assertEquals(path.getLspa(), this.lspa);
		assertNull(path.getBandwidth());
		assertEquals(path.getMetrics().get(0), this.metrics.get(0));
		assertEquals(path.getMetrics().get(1), this.metrics.get(1));
		assertEquals(path.getIncludeRoute(), this.iro);
		// check path
		path = response.getPaths().get(1);
		assertEquals(path.getExcludedRoute(), this.ero);
		assertEquals(path.getLspa(), this.lspa);
		assertNull(path.getBandwidth());
		assertEquals(path.getMetrics().get(0), this.metrics.get(0));
		assertEquals(path.getMetrics().get(1), new PCEPMetricObject(false, false, new TEMetric(1000), false, false));
		assertEquals(path.getIncludeRoute(), this.iro);
	}

	@Test
	public void testCompositeErrorObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		CompositeErrorObject compositeErrors;

		objects.addAll(this.requestParameters);
		objects.addAll(this.errors);
		compositeErrors = new CompositeErrorObject(this.requestParameters.subList(0, this.requestParameters.size()), this.errors.subList(0, this.errors.size()));
		assertEquals(compositeErrors, CompositeErrorObject.getCompositeFromList(objects));

		objects.clear();
		objects.addAll(this.errors);
		compositeErrors = new CompositeErrorObject(null, this.errors.subList(0, this.errors.size()));
		assertEquals(compositeErrors, CompositeErrorObject.getCompositeFromList(objects));

	}

	@Test
	public void testCompositeNotifyObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		CompositeNotifyObject compositeNotifications;

		objects.addAll(this.requestParameters);
		objects.addAll(this.notifications);
		compositeNotifications = new CompositeNotifyObject(this.requestParameters.subList(0, this.requestParameters.size()), this.notifications.subList(0,
				this.notifications.size()));
		assertEquals(compositeNotifications, CompositeNotifyObject.getCompositeFromList(objects));

		objects.clear();
		// first
		objects.addAll(this.requestParameters);
		objects.addAll(this.notifications);
		// second
		objects.addAll(this.requestParameters);
		objects.addAll(this.notifications);
		while (!objects.isEmpty()) {
			assertEquals(compositeNotifications, CompositeNotifyObject.getCompositeFromList(objects));
		}

		objects.clear();
		objects.addAll(this.notifications);
		compositeNotifications = new CompositeNotifyObject(null, this.notifications.subList(0, this.notifications.size()));
		assertEquals(compositeNotifications, CompositeNotifyObject.getCompositeFromList(objects));

	}

	@Test
	public void testCompositeRequestObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		CompositeRequestObject compositeRequest;

		objects.add(this.requestParameter);
		objects.add(this.endPoints);
		objects.add(this.ct);
		objects.add(this.lsp);
		objects.add(this.lspa);
		objects.add(this.bandwidth);
		objects.addAll(this.metrics);
		objects.add(this.reportedRoute);
		objects.add(this.rroBandwidth);
		objects.add(this.includeRoute);
		objects.add(this.loadBalancing);

		compositeRequest = new CompositeRequestObject(this.requestParameter, this.endPoints, this.ct, this.lsp, this.lspa, this.bandwidth,
				this.metrics.subList(0, this.metrics.size()), this.reportedRoute, this.rroBandwidth, this.includeRoute, this.loadBalancing);
		assertEquals(compositeRequest, CompositeRequestObject.getCompositeFromList(objects));

		objects.clear();
		// first
		objects.add(this.requestParameter);
		objects.add(this.endPoints);
		objects.add(this.ct);
		objects.add(this.lsp);
		objects.add(this.lspa);
		objects.add(this.bandwidth);
		objects.addAll(this.metrics);
		objects.add(this.reportedRoute);
		objects.add(this.rroBandwidth);
		objects.add(this.includeRoute);
		objects.add(this.loadBalancing);
		// second
		objects.add(this.requestParameter);
		objects.add(this.endPoints);
		objects.add(this.ct);
		objects.add(this.lsp);
		objects.add(this.lspa);
		objects.add(this.bandwidth);
		objects.addAll(this.metrics);
		objects.add(this.reportedRoute);
		objects.add(this.rroBandwidth);
		objects.add(this.includeRoute);
		objects.add(this.loadBalancing);
		while (!objects.isEmpty()) {
			assertEquals(compositeRequest, CompositeRequestObject.getCompositeFromList(objects));
		}

		objects.clear();
		objects.add(this.requestParameter);
		objects.add(this.endPoints);
		compositeRequest = new CompositeRequestObject(this.requestParameter, this.endPoints);
		assertEquals(compositeRequest, CompositeRequestObject.getCompositeFromList(objects));

	}

	@Test
	public void testCompositeStateReportObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		CompositeStateReportObject compositeStateReport;

		objects.add(this.lsp);
		for (final CompositeRptPathObject compositeRptPath : this.compositeRptPaths) {
			objects.addAll(compositeRptPath.getCompositeAsList());
		}

		compositeStateReport = new CompositeStateReportObject(this.lsp, this.compositeRptPaths);
		assertEquals(compositeStateReport, CompositeStateReportObject.getCompositeFromList(objects));

		objects.clear();
		// first
		objects.add(this.lsp);
		for (final CompositeRptPathObject compositeRptPath : this.compositeRptPaths) {
			objects.addAll(compositeRptPath.getCompositeAsList());
		}
		// second
		objects.add(this.lsp);
		for (final CompositeRptPathObject compositeRptPath : this.compositeRptPaths) {
			objects.addAll(compositeRptPath.getCompositeAsList());
		}
		while (!objects.isEmpty()) {
			assertEquals(compositeStateReport, CompositeStateReportObject.getCompositeFromList(objects));
		}

		objects.clear();
		objects.add(this.lsp);
		for (final CompositeRptPathObject compositeRptPath : this.compositeRptPaths) {
			objects.addAll(compositeRptPath.getCompositeAsList());
		}
		compositeStateReport = new CompositeStateReportObject(this.lsp, this.compositeRptPaths);
		assertEquals(compositeStateReport, CompositeStateReportObject.getCompositeFromList(objects));

	}

	@Test
	public void testCompositeUpdateRequestObject() {
		final List<PCEPObject> objects = new ArrayList<PCEPObject>();
		CompositeUpdateRequestObject compositeStateReport;

		objects.add(this.lsp);
		for (final CompositeUpdPathObject compositePath : this.compositeUpdPaths) {
			objects.addAll(compositePath.getCompositeAsList());
		}

		compositeStateReport = new CompositeUpdateRequestObject(this.lsp, this.compositeUpdPaths);
		assertEquals(compositeStateReport, CompositeUpdateRequestObject.getCompositeFromList(objects));

		objects.clear();
		// first
		objects.add(this.lsp);
		for (final CompositeUpdPathObject compositePath : this.compositeUpdPaths) {
			objects.addAll(compositePath.getCompositeAsList());
		}
		// second
		objects.add(this.lsp);
		for (final CompositeUpdPathObject compositePath : this.compositeUpdPaths) {
			objects.addAll(compositePath.getCompositeAsList());
		}
		while (!objects.isEmpty()) {
			assertEquals(compositeStateReport, CompositeUpdateRequestObject.getCompositeFromList(objects));
		}

		objects.clear();
		objects.add(this.lsp);
		for (final CompositeUpdPathObject compositePath : this.compositeUpdPaths) {
			objects.addAll(compositePath.getCompositeAsList());
		}
		compositeStateReport = new CompositeUpdateRequestObject(this.lsp, this.compositeUpdPaths);
		assertEquals(compositeStateReport, CompositeUpdateRequestObject.getCompositeFromList(objects));

	}

	@Test
	public void testSessionProposalFactory() throws IOException {
		final PCEPSessionProposalFactoryImpl spf = new PCEPSessionProposalFactoryImpl(10, 2, true, false, true, true, 5);
		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
		tlvs.add(new PCEStatefulCapabilityTlv(true, false, true));
		tlvs.add(new LSPCleanupTlv(5));
		assertEquals(new PCEPOpenObject(2, 10, 0, tlvs), spf.getSessionProposal(null, 0));
	}
}
