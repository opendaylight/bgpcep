from contextlib import contextmanager
import datetime
import logging
import paramiko
import pytest
import subprocess
import time

ODL_FEATRUES = ["odl-integration-compatible-with-all",
                "odl-infrautils-ready, odl-restconf-all",
                "odl-restconf-nb-bierman02, odl-jolokia",
                "odl-bgpcep-data-change-counter",
                "odl-bgpcep-bgp",
                "odl-bgpcep-bgp-config-example"]

log = logging.getLogger(__name__)


def shell(command: str | list | tuple, joiner="; ", cwd: str | None = None):
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        result = subprocess.run(
            exec_command,
            shell=True,
            check=True,
            capture_output=True,
            text=True,
            cwd=cwd,
        )
        log.info(f"{result.returncode:3d} |--| {result.stdout}")
        return result.returncode, result.stdout
    except subprocess.CalledProcessError as e:
        log.error(
            f"ERROR while command execution '{exec_command}':\n{e.stderr.strip()}"
        )
        return e.returncode, e.stdout
    except FileNotFoundError:
        log.error(f"ERROR command not found: {exec_command}")
        return None


def start_odl_with_features(features: list[str], timeout: int = 60) -> int:
    # set config with the required features
    shell(f"sed -ie 's/\(featuresBoot=\|featuresBoot =\)/featuresBoot = {",".join(features)},/g' etc/org.apache.karaf.features.cfg", cwd="opendaylight")

    # start ODL
    shell(" ./bin/start", cwd="opendaylight")

    #wait for proper message with timeout
    start_time = datetime.datetime.now()
    while not shell("grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log", cwd="opendaylight"):
        time.sleep(1)
        if (datetime.datetime.now() - start_time).seconds >= timeout:
            raise TimeoutError(f"ODL controller did not start within {timeout} seconds")

@contextmanager
def open_ssh_connection(hostname: str, port: int, username: str, password: str) -> paramiko.SSHClient:
    ssh_client = paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh_client.connect(hostname=hostname, port=port, username=username, password=password)
    yield ssh_client
    ssh_client.close()


def execute_karaf_command(command: str) -> None:
    with open_ssh_connection("127.0.0.1", 8101, "admin", "admin") as karaf_connection:
        stdin, stdout, stderr = karaf_connection.exec_command(command)
        print(f"{stdout=}")
        print(f"{stderr=}")


@pytest.fixture(scope="class")
def preconditions():
    start_odl_with_features(ODL_FEATRUES)
    execute_karaf_command("log:set INFO")

@pytest.mark.usefixtures("preconditions")
class TestSinglePeer300KRoutes:

    def test_routes():
        pass