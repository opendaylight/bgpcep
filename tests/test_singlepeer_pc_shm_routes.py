from contextlib import contextmanager
import datetime
import json
import logging
import paramiko
import pytest
import re
import requests
import string
import subprocess
import time

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"
ODL_FEATRUES = ["odl-integration-compatible-with-all",
                "odl-infrautils-ready, odl-restconf-all",
                "odl-restconf-nb-rfc8040, odl-jolokia",
                "odl-bgpcep-data-change-counter",
                "odl-bgpcep-bgp",
                "odl-bgpcep-bgp-config-example"]

log = logging.getLogger(__name__)


def shell(command: str | list | tuple, joiner="; ", cwd: str | None = None, capture_output=True):
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        result = subprocess.run(
            exec_command,
            shell=True,
            check=True,
            capture_output=capture_output,
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
        return None, None


def start_odl_with_features(features: list[str], timeout: int = 60) -> int:
    # set config with the required features
    shell(f"sed -ie 's/\(featuresBoot=\|featuresBoot =\)/featuresBoot = {",".join(features)},/g' etc/org.apache.karaf.features.cfg", cwd="opendaylight")

    # disable color output of karaf
    shell(f"echo 'karaf.ansi.console = false' >> etc/system.properties", cwd="opendaylight")

    shell("echo 'setopt disable-highlighter' >> etc/shell.init.script", cwd="opendaylight")

    # start ODL
    shell(" ./bin/start", cwd="opendaylight")

    #wait for proper message with timeout
    start_time = datetime.datetime.now()
    result = shell("grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log", cwd="opendaylight")
    log.error(result)
    # Need to use while True to handle exception which can be thrown by shell fucntion
    while True:
        try:
            rc, stdout =  shell("grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log", cwd="opendaylight")
            if rc == 0:
                break
        except Exception as e:
            pass
        time.sleep(1)
        if (datetime.datetime.now() - start_time).seconds >= timeout:
            raise TimeoutError(f"ODL controller did not start within {timeout} seconds")

@contextmanager
def open_ssh_connection(hostname: str, port: int, username: str, password: str) -> paramiko.SSHClient:
    ssh_client = paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh_client.connect(hostname=hostname, port=port, username=username, password=password, look_for_keys=False, allow_agent=False)
    yield ssh_client
    ssh_client.close()


def execute_karaf_command(command: str) -> None:
    with open_ssh_connection("127.0.0.1", 8101, "karaf", "karaf") as karaf_connection:
        stdin, stdout, stderr = karaf_connection.exec_command(command)
        stdout = stdout.read().decode()
        stderr = stderr.read().decode()

    log.info(f"{stdout=}")
    if stderr:
        log.warn(f"Karaf command {command} failed with {stderr}")

    return stdout, stderr

def get_request(url, expected_code=None):
    log.info(f"Sending request to get {url}")
    response = requests.get(url, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    #log.info(response.text)
    #log.info(response.status_code)

    return response

def put_request(url, data, expected_code=None):
    log.info(f"Sending to {url} this data: {data}")
    response = requests.put(url=url, data=data, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    log.info(response.text)
    log.info(response.status_code)

    return response

def resolve_templated_text(template, mapping):
    with open(template) as template_file:
        template = template_file.read()
    resolved_tempate = string.Template(template.rstrip()).safe_substitute(mapping)

    return resolved_tempate

def put_templated_request(temlate, mapping):
    url = resolve_templated_text(temlate + "/location.uri", mapping)
    data = resolve_templated_text(temlate + "/data.json", mapping)
    response = put_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}", data)

    return response

def get_ipv4_topology():
    response = get_request(url=f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/network-topology:network-topology/topology=example-ipv4-topology?content=nonconfig",)

    if response.status_code == 401:
        raise Exception("Anuthorized to get the topology")
    if response.status_code == 404:
        raise Exception("Topology not found")

    topology = response.json()

    return topology

def get_ipv4_topology_count():
    topology = get_ipv4_topology()
    #log.warn(topology)
    log.warn(str(topology))
    topology_count = len(re.findall("'prefix':'", str(topology)))

    return topology_count

def set_bgp_neighbour(ip, passive_mode=True):
    mapping = {"IP": ip, "HOLDTIME": 180, "PEER_PORT": 17900 , "PASSIVE_MODE": passive_mode, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = put_templated_request("templates/bgp_peer", mapping)

def start_bgp_speaker(ammount, insert, withdraw, prefill, update):
    rc, stdout = shell(f"python play.py --amount {ammount}\
            --myip=127.0.0.1 \
            --myport=17900 \
            --peerip=127.0.0.1 \
            --peerport=1790 \
            --insert={insert} \
            --withdraw={withdraw} \
            --prefill {prefill} \
            --update {update} \
            --info &",
            capture_output=False)

    return

def wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value, timeout=60, wait_period=5, consecutive_times_stable_value=2):
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 0
    while True:
        current_count = get_ipv4_topology_count()
        log.warn(f"{current_count=}, {last_count=}, {excluded_value=}, {stable_times=}")
        if current_count != excluded_value and current_count == last_count:
            stable_times += 1
            if stable_times >= consecutive_times_stable_value:
                # topology is stable, no need to continue
                return
        else:
            stable_times = 0
        last_count = current_count
        time.sleep(wait_period)
        if (datetime.datetime.now() - start_time).seconds >= timeout:
            raise Exception(f"Ipv4 topology is not stable after {timeout} seconds")



@pytest.fixture(scope="class")
def preconditions():
    start_odl_with_features(ODL_FEATRUES, timeout=80)
    execute_karaf_command("log:set INFO")
    #shell("pkill -9 -f org.apache.karaf.main.Main")

@pytest.mark.usefixtures("preconditions")
class TestSinglePeer300KRoutes:

    def test_topology_is_empty(self):
        timeout = 60
        start_time = datetime.datetime.now()
        while True:
            try:
                topology_count = get_ipv4_topology_count()
                if topology_count == 0:
                    break
            except Exception as e:
                log.error(e)
            if (datetime.datetime.now() - start_time).seconds >= timeout:
                raise TimeoutError(f"Unable to check topology within {timeout} seconds")
            time.sleep(1)

    def test_reconfigure_odl_to_accept_connection(self):
        set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

    def test_start_talking_bgp_speaker(self):
        start_bgp_speaker(ammount=300,
                          insert=1,
                          withdraw=0,
                          prefill=0,
                          update="single")

    def test_stable_ip_topology(self):
        wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=0)
