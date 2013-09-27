/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.concepts.Bandwidth;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.concepts.UnnumberedInterfaceIdentifier;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.CompositePathObject;
import org.opendaylight.protocol.pcep.object.CompositeReplySvecObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestSvecObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.protocol.pcep.object.CompositeRptPathObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdPathObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.protocol.pcep.object.PCEPBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPClassTypeObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPoints;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExcludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPExistingPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPGlobalConstraintsObject;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPP2MPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.RROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject;
import org.opendaylight.protocol.pcep.subobject.RROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSRLGSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSubobjectAttribute;
import org.opendaylight.protocol.pcep.subobject.XROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.tlv.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.collect.Lists;

/**
 *
 */
public class ObjectsTest {

	private final AsNumber as = new AsNumber((long) 2555);

	@Test
	public void compositeErrorObjectTest() {

		final List<PCEPErrorObject> errorObjects = new ArrayList<PCEPErrorObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPErrorObject(PCEPErrors.ATTEMPT_2ND_SESSION));
			}
		};

		final List<PCEPRequestParameterObject> errorParams = new ArrayList<PCEPRequestParameterObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true, true));
			}
		};
		final CompositeErrorObject m = new CompositeErrorObject(errorParams, errorObjects);
		final CompositeErrorObject m2 = new CompositeErrorObject(errorParams, errorObjects);
		final CompositeErrorObject m3 = new CompositeErrorObject(errorObjects);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getErrors(), m2.getErrors());
		assertEquals(m.getRequestParameters(), m2.getRequestParameters());
		assertEquals(m, CompositeErrorObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeNotifyObjectTest() {

		final List<PCEPNotificationObject> notifications = new ArrayList<PCEPNotificationObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPNotificationObject((short) 2, (short) 3));
			}
		};

		final List<PCEPRequestParameterObject> reqParams = new ArrayList<PCEPRequestParameterObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true, true));
			}
		};
		final CompositeNotifyObject m = new CompositeNotifyObject(reqParams, notifications);
		final CompositeNotifyObject m2 = new CompositeNotifyObject(reqParams, notifications);
		final CompositeNotifyObject m3 = new CompositeNotifyObject(notifications);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getNotificationObjects(), m2.getNotificationObjects());
		assertEquals(m.getRequestParameters(), m2.getRequestParameters());
		assertEquals(m, CompositeNotifyObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositePathObjectTest() {

		final List<ExplicitRouteSubobject> subobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final CompositePathObject m = new CompositePathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPIncludeRouteObject(subobjects, true, true));
		final CompositePathObject m2 = new CompositePathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPIncludeRouteObject(subobjects, true, true));
		final CompositePathObject m3 = new CompositePathObject(new PCEPExplicitRouteObject(subobjects, false));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getBandwidth(), m2.getBandwidth());
		assertEquals(m.getExcludedRoute(), m2.getExcludedRoute());
		assertEquals(m.getIncludeRoute(), m2.getIncludeRoute());
		assertEquals(m.getLspa(), m2.getLspa());
		assertEquals(m.getMetrics(), m2.getMetrics());
		assertEquals(m, CompositePathObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeRptPathObjectTest() {

		final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new UnnumberedInterfaceIdentifier(2), true));
				this.add(new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 12, (byte) 122,
						(byte) 125, (byte) 2 }), 22), false));
			}
		};

		final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new RROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new UnnumberedInterfaceIdentifier(2)));
				this.add(new RROIPAddressSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 12, (byte) 122,
						(byte) 125, (byte) 2 }), 22), true, false));
			}
		};

		final CompositeRptPathObject m = new CompositeRptPathObject(new PCEPExplicitRouteObject(eroSubobjects, false), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPExistingPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new PCEPReportedRouteObject(rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		});
		final CompositeRptPathObject m2 = new CompositeRptPathObject(new PCEPExplicitRouteObject(eroSubobjects, false), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPExistingPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new PCEPReportedRouteObject(rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		});
		final CompositePathObject m3 = new CompositePathObject(new PCEPExplicitRouteObject(eroSubobjects, false));
		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getBandwidth(), m2.getBandwidth());
		assertEquals(m.getExcludedRoute(), m2.getExcludedRoute());
		assertEquals(m.getLspa(), m2.getLspa());
		assertEquals(m.getMetrics(), m2.getMetrics());
		assertEquals(m, CompositeRptPathObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeUpdPathObjectTest() {

		final List<ExplicitRouteSubobject> subobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final CompositeUpdPathObject m = new CompositeUpdPathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		});
		final CompositeUpdPathObject m2 = new CompositeUpdPathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		});
		final CompositeUpdPathObject m3 = new CompositeUpdPathObject(new PCEPExplicitRouteObject(subobjects, false));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getBandwidth(), m2.getBandwidth());
		assertEquals(m.getExcludedRoute(), m2.getExcludedRoute());
		assertEquals(m.getLspa(), m2.getLspa());
		assertEquals(m.getMetrics(), m2.getMetrics());
		assertEquals(m, CompositeUpdPathObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeRequestedObjectTest() {

		final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new RROAsNumberSubobject(ObjectsTest.this.as));
			}
		};

		final CompositeRequestObject m = new CompositeRequestObject(new PCEPRequestParameterObject(true, true, true, true, true, true, false, false, false, (short) 1, 1, new ArrayList<PCEPTlv>() {
			private static final long serialVersionUID = 1L;
			{
				this.add(new OrderTlv(1L, 2L));
			}
		}, true, true), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new IPv4Address(new byte[] {
				(byte) 127, (byte) 0, (byte) 0, (byte) 1 })), new PCEPClassTypeObject((short) 2), new PCEPLspObject((short) 1, true, true, true, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPReportedRouteObject(rroSubobjects, true), new PCEPExistingPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new PCEPIncludeRouteObject(eroSubobjects, true, true), new PCEPLoadBalancingObject(2, new Bandwidth((float) 0.2), true));
		final CompositeRequestObject m2 = new CompositeRequestObject(new PCEPRequestParameterObject(true, true, true, true, true, true, false, false, false, (short) 1, 1, new ArrayList<PCEPTlv>() {
			private static final long serialVersionUID = 1L;
			{
				this.add(new OrderTlv(1L, 2L));
			}
		}, true, true), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new IPv4Address(new byte[] {
				(byte) 127, (byte) 0, (byte) 0, (byte) 1 })), new PCEPClassTypeObject((short) 2), new PCEPLspObject((short) 1, true, true, true, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPReportedRouteObject(rroSubobjects, true), new PCEPExistingPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new PCEPIncludeRouteObject(eroSubobjects, true, true), new PCEPLoadBalancingObject(2, new Bandwidth((float) 0.2), true));
		final CompositeRequestObject m3 = new CompositeRequestObject(new PCEPRequestParameterObject(true, true, true, true, true, true, false, false, false, (short) 1, 1, new ArrayList<PCEPTlv>() {
			private static final long serialVersionUID = 1L;
			{
				this.add(new OrderTlv(1L, 2L));
			}
		}, true, true), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new IPv4Address(new byte[] {
				(byte) 127, (byte) 0, (byte) 0, (byte) 1 })));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getBandwidth(), m2.getBandwidth());
		assertEquals(m.getLspa(), m2.getLspa());
		assertEquals(m.getMetrics(), m2.getMetrics());
		assertEquals(m.getClassType(), m2.getClassType());
		assertEquals(m.getEndPoints(), m2.getEndPoints());
		assertEquals(m.getIncludeRoute(), m2.getIncludeRoute());
		assertEquals(m.getLsp(), m2.getLsp());
		assertEquals(m.getRroBandwidth(), m2.getRroBandwidth());
		assertEquals(m, CompositeRequestObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeResponseObjectTest() {

		final List<ExplicitRouteSubobject> subobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final List<CompositePathObject> paths = new ArrayList<CompositePathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositePathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
					}
				}, new PCEPIncludeRouteObject(subobjects, true, true)));
			}
		};

		final CompositeResponseObject m = new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true, true), new PCEPNoPathObject((short) 2, true, false), new PCEPLspObject((short) 1, true, true, true, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPIncludeRouteObject(subobjects, true, true), paths);
		final CompositeResponseObject m2 = new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true, true), new PCEPNoPathObject((short) 2, true, false), new PCEPLspObject((short) 1, true, true, true, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
			}
		}, new PCEPIncludeRouteObject(subobjects, true, true), paths);
		final CompositeResponseObject m3 = new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true, true));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getBandwidth(), m2.getBandwidth());
		assertEquals(m.getLspa(), m2.getLspa());
		assertEquals(m.getMetrics(), m2.getMetrics());
		assertEquals(m.getIncludeRoute(), m2.getIncludeRoute());
		assertEquals(m.getLsp(), m2.getLsp());
		assertEquals(m.getPaths(), m2.getPaths());
		assertEquals(m.getRequestParameter(), m2.getRequestParameter());
		assertEquals(m, CompositeResponseObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeStateReportObjectTest() {

		final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new RROAsNumberSubobject(ObjectsTest.this.as));
			}
		};

		final List<CompositeRptPathObject> paths = new ArrayList<CompositeRptPathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(eroSubobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPExistingPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new PCEPReportedRouteObject(rroSubobjects, true), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
					}
				}));
			}
		};

		final CompositeStateReportObject m = new CompositeStateReportObject(new PCEPLspObject((short) 1, true, true, true, true), paths);
		final CompositeStateReportObject m2 = new CompositeStateReportObject(new PCEPLspObject((short) 1, true, true, true, true), paths);
		final CompositeStateReportObject m3 = new CompositeStateReportObject(new PCEPLspObject((short) 1, true, true, true, true));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getLsp(), m2.getLsp());
		assertEquals(m.getPaths(), m2.getPaths());
		assertEquals(m, CompositeStateReportObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeUpdateRequestObjectTest() {

		final List<ExplicitRouteSubobject> subobjects = new ArrayList<ExplicitRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new EROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final List<CompositeUpdPathObject> paths = new ArrayList<CompositeUpdPathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(subobjects, true), new PCEPLspaObject(2, 2, 2, (short) 2, (short) 2, true, true, true, true), new PCEPRequestedPathBandwidthObject(new Bandwidth((float) 0.222), true, true), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new TEMetric(255), true, false));
					}
				}));
			}
		};

		final CompositeUpdateRequestObject m = new CompositeUpdateRequestObject(new PCEPLspObject((short) 1, true, true, true, true), paths);
		final CompositeUpdateRequestObject m2 = new CompositeUpdateRequestObject(new PCEPLspObject((short) 1, true, true, true, true), paths);
		final CompositeUpdateRequestObject m3 = new CompositeUpdateRequestObject(new PCEPLspObject((short) 1, true, true, true, true));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getLsp(), m2.getLsp());
		assertEquals(m.getPaths(), m2.getPaths());
		assertEquals(m, CompositeUpdateRequestObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeRequestSVECObjectTest() {

		final List<ExcludeRouteSubobject> subobjects = new ArrayList<ExcludeRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new XROAsNumberSubobject(ObjectsTest.this.as, false));
			}
		};

		final List<Long> requestIds = new ArrayList<Long>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(0x12345678L);
			}
		};

		final CompositeRequestSvecObject m = new CompositeRequestSvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true), new PCEPObjectiveFunctionObject(PCEPOFCodes.MBC, true, false), new PCEPGlobalConstraintsObject((short) 2, (short) 2, (short) 2, (short) 2, true, false), new PCEPExcludeRouteObject(subobjects, true, true, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(0x123456), true, false));
			}
		});
		final CompositeRequestSvecObject m2 = new CompositeRequestSvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true), new PCEPObjectiveFunctionObject(PCEPOFCodes.MBC, true, false), new PCEPGlobalConstraintsObject((short) 2, (short) 2, (short) 2, (short) 2, true, false), new PCEPExcludeRouteObject(subobjects, true, true, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(0x123456), true, false));
			}
		});
		final CompositeRequestSvecObject m3 = new CompositeRequestSvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getExcludeRoute(), m2.getExcludeRoute());
		assertEquals(m.getGlobalConstraints(), m2.getGlobalConstraints());
		assertEquals(m.getObjectiveFunction(), m2.getObjectiveFunction());
		assertEquals(m.getSvec(), m2.getSvec());
		assertEquals(m, CompositeRequestSvecObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void compositeRplySVECObjectTest() {

		final List<Long> requestIds = new ArrayList<Long>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(0x12345678L);
			}
		};

		final CompositeReplySvecObject m = new CompositeReplySvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true), new PCEPObjectiveFunctionObject(PCEPOFCodes.MBC, true, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(0x123456), true, false));
			}
		});
		final CompositeReplySvecObject m2 = new CompositeReplySvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true), new PCEPObjectiveFunctionObject(PCEPOFCodes.MBC, true, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(0x123456), true, false));
			}
		});
		final CompositeReplySvecObject m3 = new CompositeReplySvecObject(new PCEPSvecObject(true, true, true, false, false, requestIds, true));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCompositeAsList(), m2.getCompositeAsList());
		assertEquals(m.getObjectiveFunction(), m2.getObjectiveFunction());
		assertEquals(m.getSvec(), m2.getSvec());
		assertEquals(m, CompositeReplySvecObject.getCompositeFromList(m.getCompositeAsList()));
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void subobjectsTest() {
		final EROAsNumberSubobject xas = new EROAsNumberSubobject(this.as, false);
		final EROUnnumberedInterfaceSubobject uis = new EROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 127, (byte) 0,
				(byte) 0, (byte) 2 }), new UnnumberedInterfaceIdentifier(2), true);
		final EROIPPrefixSubobject<IPv4Prefix> ips = new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] {
				(byte) 12, (byte) 122, (byte) 125, (byte) 2 }), 22), false);

		assertEquals(xas.getASNumber().getValue().longValue(), 2555);
		assertEquals(xas.isLoose(), false);
		assertFalse(xas.equals(null));
		assertFalse(xas.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(xas.equals(xas));

		assertEquals(uis.getInterfaceID().getInterfaceId(), 2);
		assertEquals(uis.getRouterID(), new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }));
		assertEquals(uis.isLoose(), true);
		assertFalse(uis.equals(null));
		assertFalse(uis.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(uis.equals(uis));

		assertEquals(ips.getPrefix(), new IPv4Prefix(new IPv4Address(new byte[] { (byte) 12, (byte) 122, (byte) 125, (byte) 2 }), 22));
		assertEquals(ips.isLoose(), false);
		assertFalse(ips.equals(null));
		assertFalse(ips.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(ips.equals(ips));
	}

	@Test
	public void xroSubobjectsTest() {
		final XROAsNumberSubobject xas = new XROAsNumberSubobject(this.as, false);
		final XROUnnumberedInterfaceSubobject uis = new XROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 127, (byte) 0,
				(byte) 0, (byte) 2 }), new UnnumberedInterfaceIdentifier(2), true, XROSubobjectAttribute.SRLG);
		final XROIPPrefixSubobject<IPv4Prefix> ips = new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] {
				(byte) 12, (byte) 122, (byte) 125, (byte) 2 }), 22), false, XROSubobjectAttribute.SRLG);
		final XROSRLGSubobject srlg = new XROSRLGSubobject(new SharedRiskLinkGroup(0x1234L), true);

		assertEquals(xas.getASNumber().getValue().longValue(), 2555);
		assertEquals(xas.isMandatory(), false);
		assertFalse(xas.equals(null));
		assertFalse(xas.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(xas.equals(xas));

		// FIXME BUG-89
		// assertEquals(xas.toString(), "XROAsNumberSubobject [asnumber=AsNumber [_value=2555], mandatory=false]");

		assertEquals(uis.getInterfaceID().getInterfaceId(), 2);
		assertEquals(uis.getRouterID(), new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }));
		assertEquals(uis.isMandatory(), true);
		assertFalse(uis.equals(null));
		assertFalse(uis.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(uis.equals(uis));
		assertEquals(uis.getAttribute(), XROSubobjectAttribute.SRLG);
		assertEquals(
				uis.toString(),
				"XROUnnumberedInterfaceSubobject [attribute=SRLG, interfaceID=UnnumberedInterfaceIdentifier [interfaceId=2], routerID=127.0.0.2, mandatory=true]");

		assertEquals(ips.getPrefix(), new IPv4Prefix(new IPv4Address(new byte[] { (byte) 12, (byte) 122, (byte) 125, (byte) 2 }), 22));
		assertEquals(ips.isMandatory(), false);
		assertFalse(ips.equals(null));
		assertFalse(ips.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(ips.equals(ips));
		assertEquals(ips.getAttribute(), XROSubobjectAttribute.SRLG);
		assertEquals(ips.toString(), "XROIPPrefixSubobject [attribute=SRLG, prefix=12.122.124.0/22, mandatory=false]");

		assertEquals(srlg.getSrlgId().getValue(), 0x1234L);
		assertEquals(srlg.getAttribute(), XROSubobjectAttribute.SRLG);
		assertFalse(srlg.equals(null));
		assertFalse(srlg.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(srlg.equals(srlg));
		assertEquals(srlg.toString(), "XROSRLGSubobject [attribute=SRLG, srlgId=4660, mandatory=true]");
	}

	@Test
	public void testToString() {
		final List<PCEPObject> objects = Lists.newArrayList();
		objects.add(new PCEPExistingPathBandwidthObject(new Bandwidth(22.0), true, false));
		objects.add(new PCEPRequestedPathBandwidthObject(new Bandwidth(22.0), true, false));
		objects.add(new PCEPEndPointsObject<IPv4Address>(IPv4.FAMILY.addressForBytes(new byte[] { 1, 1, 1, 1 }), IPv4.FAMILY.addressForBytes(new byte[] {
				1, 1, 1, 2 })));
		objects.add(new PCEPP2MPEndPointsObject<IPv4Address>(0, IPv4.FAMILY.addressForBytes(new byte[] { 1, 1, 1, 1 }), Lists.newArrayList(IPv4.FAMILY.addressForBytes(new byte[] {
				1, 1, 1, 1 })), true, false));

		for (final PCEPObject o : objects) {
			assertThat(o.toString(), containsString("processed=true"));
			assertThat(o.toString(), containsString("ignored=false"));
			if (o instanceof PCEPBandwidthObject)
				assertThat(o.toString(), containsString("bandwidth=Bandwidth [bytesPerSecond=22.0]"));
			if (o instanceof PCEPEndPoints)
				assertThat(o.toString(), containsString("sourceAddress=1.1.1.1"));
			if (o instanceof PCEPP2MPEndPointsObject)
				assertThat(o.toString(), containsString("destinationAddresses=[1.1.1.1]"));
		}
	}

}
