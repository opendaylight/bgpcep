from pydantic_settings import BaseSettings
from pydantic import computed_field


class Variables(BaseSettings):
    BGP_TOOL_PORT: int = 17900
    ODL_BGP_PORT: int = 1790
    ODL_IP: str = "127.0.0.1"
    TOOLS_IP: str = "127.0.0.2"
    RESTCONF_PORT: int = 8181
    RESTCONF_ROOT: str = "rests"

    @computed_field
    @property
    def REST_API(self) -> str:
        return f"{self.RESTCONF_ROOT}/data"

    KARAF_LOG_LEVEL: str = "INFO"
    TEST_DURATION_MULTIPLIER: int = 1


variables = Variables()
