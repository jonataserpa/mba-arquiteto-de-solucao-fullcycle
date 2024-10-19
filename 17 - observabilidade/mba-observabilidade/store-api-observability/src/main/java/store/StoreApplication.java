package store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class StoreApplication {

	public static void main(String... args) {
		SpringApplication.run(StoreApplication.class, args);
	}
}
