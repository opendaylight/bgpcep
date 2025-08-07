import pytest

from lib import infra

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"
ODL_FEATRUES = ["odl-integration-compatible-with-all",
                "odl-infrautils-ready, odl-restconf-all",
                "odl-restconf-nb-rfc8040, odl-jolokia",
                "odl-bgpcep-data-change-counter",
                "odl-bgpcep-bgp",
                "odl-bgpcep-bgp-config-example"]

@pytest.fixture(scope="class")
def preconditions():
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.start_odl_with_features(ODL_FEATRUES, timeout=80)
    infra.execute_karaf_command("log:set INFO")
    #shell("pkill -9 -f org.apache.karaf.main.Main")