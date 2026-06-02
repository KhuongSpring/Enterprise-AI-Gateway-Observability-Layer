package com.enterprise.aigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.r2dbc.url=r2dbc:h2:mem:///testdb", "spring.r2dbc.username=sa",
    "spring.r2dbc.password=", "spring.flyway.enabled=false",
    "spring.cloud.compatibility-verifier.enabled=false"})
class AigatewayApplicationTests {

  @Test
  void contextLoads() {}

}
