#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import textwrap

import allure
import pytest

from libraries import bgp
from libraries import cluster
from libraries import templated_requests
from libraries.variables import variables
from suites.suite_order import SuiteOrder

TOOLS_IP = variables.TOOLS_IP
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"
BGP_PEER_TEMPLATE = "variables/bgpuser/bgp_peer"

log = logging.getLogger(__name__)


@pytest.mark.bgp
@pytest.mark.cluster
@pytest.mark.functional
@pytest.mark.multi_device
@pytest.mark.usefixtures("odl_three_node_cluster")
@pytest.mark.run(order=SuiteOrder.CLUSTER_SETUP)
class TestClusterSetup:

    @allure.description(
        textwrap.dedent(
            """
            **Verifies the three node ODL cluster is usable from every member**

            Checks that RESTCONF answers on all members, then writes BGP peer \
            configuration through member 1 and reads it back from every other \
            member. A successful read on members 2 and 3 proves the config \
            datastore is genuinely replicated, which is only true if the pekko \
            cluster actually formed.
            """
        )
    )
    def test_cluster_is_formed_and_replicating(
        self, allure_step_with_separate_logging
    ):
        members = cluster.active_nodes()

        with allure_step_with_separate_logging("step_check_all_members_reported"):
            assert len(members) == 3, f"expected 3 cluster members, got {members}"
            log.info(f"Cluster members under test: {members}")

        with allure_step_with_separate_logging("step_check_restconf_on_each_member"):
            # Every member must serve RESTCONF on its own address.
            for host in members:
                templated_requests.get_from_uri(
                    uri=f"{variables.REST_API}/network-topology:network-topology"
                    "?content=nonconfig",
                    expected_code=200,
                    host=host,
                )

        with allure_step_with_separate_logging("step_configure_peer_via_member_1"):
            bgp.set_bgp_neighbour(
                ip=TOOLS_IP,
                holdtime=HOLDTIME,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
                host=members[0],
            )

        with allure_step_with_separate_logging("step_read_peer_back_from_each_member"):
            # The config datastore is replicated, so the peer written through
            # member 1 has to be readable from every member. The same template
            # the peer was written with is reused, so the two cannot drift
            # apart.
            mapping = {"IP": TOOLS_IP, "BGP_RIB_OPENCONFIG": RIB_INSTANCE}
            for host in members:
                response = templated_requests.get_templated_request(
                    BGP_PEER_TEMPLATE,
                    mapping,
                    json=False,
                    expected_code=200,
                    host=host,
                )
                assert TOOLS_IP in response.text, (
                    f"peer {TOOLS_IP} not visible from member {host}: "
                    f"{response.text}"
                )

        with allure_step_with_separate_logging("step_delete_peer_via_member_1"):
            bgp.delete_bgp_neighbour(
                ip=TOOLS_IP, rib_instance=RIB_INSTANCE, host=members[0]
            )
