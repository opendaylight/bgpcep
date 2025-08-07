#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 iBGP peer, data change counter
# is used. This suite uses play.py as single iBGP peer. The suite
# only looks at example-ipv4-topology, so RIB is not examined.
# This suite requires odl-bgpcep-data-change-counter to be installed
# so make sure it is added to "install-features" of any jobs
# that are going to invoke it.The suite consists of two halves,
# differing on which side initiates BGP connection.
# Data change counter is a lightweight way to detect
# "work is being done". change_counter.py provide a nice function
# to wait for stability, but it needs initial value, that is why
# "TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()"
# appears just before work-inducing action. The time for
# test_wait_for_stable_* cases to finish is the main performance
# metric.After waiting for stability is done, full check on
# number of prefixes present is performed.
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering


import logging
import pytest
import signal
import time

from lib import bgp
from lib import change_counter
from lib import infra
from lib import ip_topology
from lib import utils


PREFIXES_COUNT = 600_000

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=3)
class TestSinglePeerChangeCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    def test_for_empty_topology_before_talking(self):
        topology_count = utils.retry_function_and_expect_value(120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count)
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."
        #assert False, "This test case should fail"


    @pytest.mark.skip_if_fails("test_for_empty_topology_before_talking")
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbour_user(ip="127.0.0.1", passive_mode=True)

    def test_recofigure_data_change_counter(self):
        change_counter.set_change_counter(topology_name="example-ipv4-topology")

    @pytest.mark.skip_if_fails("test_recofigure_data_change_counter")
    def test_data_change_counter_ready(self):
        utils.retry_function(5, 1, change_counter.get_change_count)


    @pytest.mark.skip_if_fails(["test_reconfigure_odl_to_accept_connection",
                                "test_data_change_counter_ready"])
    def test_start_talking_bgp_speaker(self):
        TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()
        TestSinglePeerChangeCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=PREFIXES_COUNT,
                          insert=1,
                          withdraw=0,
                          prefill=0,
                          update="single",
                          listen=False,
                          info=True)

    @pytest.mark.skip_if_fails(["test_reconfigure_odl_to_accept_connection",
                        "test_start_talking_bgp_speaker",
                        "test_data_change_counter_ready"])
    def test_wait_for_stable_talking_ip_topology(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count_single+1)

    @pytest.mark.skip_if_fails(["test_reconfigure_odl_to_accept_connection",
                                "test_start_talking_bgp_speaker"])
    def test_talking_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert topology_count == PREFIXES_COUNT, f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.skip_if_fails("test_start_talking_bgp_speaker")
    def test_kill_talking_bgp_speaker(self):
        TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()
        bgp.stop_bgp_speaker(self.bgp_speaker_process)

    @pytest.mark.skip_if_fails("test_start_talking_bgp_speaker")
    def test_store_result_for_talking_bgp_speaker(self):
        infra.shell("mv totals-bgp.csv results/changecount-talking-totals-bgp.csv")
        infra.shell("mv performance-bgp.csv results/changecount-talking-performance-bgp.csv")

    @pytest.mark.skip_if_fails(["test_kill_talking_bgp_speaker",
                                "test_data_change_counter_ready"])
    def test_wait_for_stable_ip_topology_after_talking(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count_single+1)

    def test_for_empty_ip_topology_after_talking(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    @pytest.mark.skip_if_fails("test_for_empty_ip_topology_after_talking")
    def test_start_listening_bgp_speaker(self):
        TestSinglePeerChangeCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=PREFIXES_COUNT,
                          insert=1,
                          withdraw=0,
                          prefill=0,
                          update="single",
                          listen=True,
                          info=True)

    @pytest.mark.skip_if_fails("test_start_listening_bgp_speaker")
    def test_reconfigure_odl_to_initiate_connection(self):
        TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()
        bgp.set_bgp_neighbour_user(ip="127.0.0.1", passive_mode=False)

    @pytest.mark.skip_if_fails(["test_start_listening_bgp_speaker",
                    "test_reconfigure_odl_to_initiate_connection",
                    "test_data_change_counter_ready"])
    def test_wait_for_stable_listening_ip_topology(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count_single+1)

    @pytest.mark.skip_if_fails(["test_start_listening_bgp_speaker",
                                "test_reconfigure_odl_to_initiate_connection"])
    def test_listening_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert topology_count == PREFIXES_COUNT, f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.skip_if_fails("test_start_listening_bgp_speaker")
    def test_kill_listening_bgp_speaker(self):
        TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()
        bgp.stop_bgp_speaker(self.bgp_speaker_process)

    @pytest.mark.skip_if_fails("test_start_listening_bgp_speaker")
    def test_store_result_for_listening_bgp_speaker(self):
        infra.shell("mv totals-bgp.csv results/changecount-listening-totals-bgp.csv")
        infra.shell("mv performance-bgp.csv results/changecount-listening-performance-bgp.csv")

    @pytest.mark.skip_if_fails(["test_kill_listening_bgp_speaker",
                    "test_data_change_counter_ready"])
    def test_wait_for_stable_ip_topology_after_listening(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count_single+1)

    def test_for_empty_ip_topology_after_listening(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_restore_data_change_counter_configuration(self):
        change_counter.set_change_counter(topology_name="example-linkstate-topology")

    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbour(ip="127.0.0.1")