package br.edu.ifnmg.pagtesouro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "br.edu.ifnmg.pagtesouro.repository")
public class TccServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TccServerApplication.class, args);
	}

}
