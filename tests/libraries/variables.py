#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# These variables are considered global and immutable, so their names are in ALL_CAPS.
#

from pydantic_settings import BaseSettings
from pydantic import computed_field


class Variables(BaseSettings):
    """
    Defines all global test settings, which can be overridden by environment
    variables.
    """

    BGP_TOOL_PORT: int = 17900
    ODL_BGP_PORT: int = 1790
    ODL_IP: str = "127.0.0.1"
    ODL_USER: str = "admin"
    ODL_PASSWORD: str = "admin"
    RESTCONF_PORT: int = 8181
    RESTCONF_ROOT: str = "rests"

    @computed_field
    @property
    def REST_API(self) -> str:
        """Computes the RESTCONF data API root URI."""
        return f"{self.RESTCONF_ROOT}/data"

    TOOLS_IP: str = "127.0.1.0"
    TOOLS_USER: str = "admin"
    TOOLS_PASSWORD: str = "admin"
    KARAF_LOG_LEVEL: str = "INFO"
    TEST_DURATION_MULTIPLIER: int = 1
    ENABLE_TCP_TW_REUSE: bool = False


variables = Variables()
