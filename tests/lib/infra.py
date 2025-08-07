#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from contextlib import contextmanager
import datetime
import paramiko
import subprocess
import time
import logging

log = logging.getLogger(__name__)

def shell(command: str | list | tuple, joiner="; ", cwd: str | None = None, capture_output=True, run_in_background=False):
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        if run_in_background:
            process = subprocess.Popen(f"exec {exec_command}", shell=True, text=True, cwd=cwd)
            return process
        else:
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

    shell("sed -ie 's/memory-mapped = true/memory-mapped = false/g' system/org/opendaylight/controller/sal-clustering-config/*/sal-clustering-config-*-factorypekkoconf.xml", cwd="opendaylight")

    # start ODL
    shell("JAVA_OPTS=-Xmx8g ./bin/start", cwd="opendaylight")

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