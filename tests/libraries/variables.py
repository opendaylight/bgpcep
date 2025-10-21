from pydantic_settings import BaseSettings
from pydantic import computed_field


class Variables(BaseSettings):
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
        return f"{self.RESTCONF_ROOT}/data"

    TOOLS_IP: str = "127.0.1.0"
    TOOLS_USER: str = "admin"
    TOOLS_PASSWORD: str = "admin"
    KARAF_LOG_LEVEL: str = "INFO"
    TEST_DURATION_MULTIPLIER: int = 1


variables = Variables()
