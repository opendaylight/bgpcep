import logging
import pytest
import signal

from lib import bgp
from lib import infra
from lib import ip_topology


PREFIXES_COUNT = 600_000

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
class TestSinglePeer300KRoutes:
    bgp_speaker_process = None

    def test_topology_is_empty(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."