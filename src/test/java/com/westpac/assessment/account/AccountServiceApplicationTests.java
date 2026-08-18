package com.westpac.assessment.account;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test requires database and Redis - skipped for unit testing")
class AccountServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
