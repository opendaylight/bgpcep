#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import paramiko


class RemoteSSHSessionHandler:

    def __init__(self, host, username, password, port=22):
        ssh_client = paramiko.SSHClient()
        ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh_client.connect(
            hostname=host,
            port=port,
            username=username,
            password=password,
            look_for_keys=False,
            allow_agent=False,
        )
        self.ssh_client = ssh_client
        self.stdin, self.stdout, self.stderr = None, None, None

    def start_command(self, command):
        self.stdin, self.stdout, self.stderr = self.ssh_client.exec_command(
            command, get_pty=True
        )

    def stop_command(self):
        if self.stdin:
            self.stdin.write("\x03")
            self.stdin.flush()

        self.stdin, self.stdout, self.stderr = None, None, None
