package br.edu.ifnmg.pagtesouro;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer banco de dados PostgreSQL ativo localmente para inicialização do DataSource e Flyway")
class TccServerApplicationTests {

	@Test
	void contextLoads() {
	}

}

