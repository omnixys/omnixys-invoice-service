package com.omnixys.invoice;

import com.omnixys.invoice.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InvoiceApplicationTests {

  @BeforeAll
	protected static void setup() {
		new Env();
	}

    @Test
    void contextLoads() {
    }

}
