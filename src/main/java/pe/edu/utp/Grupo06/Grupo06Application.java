package pe.edu.utp.Grupo06;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pe.edu.utp.Grupo06.view.JavaFxApplication;

@SpringBootApplication
public class Grupo06Application {

	public static void main(String[] args) {
		// Lanza la aplicación de escritorio JavaFX integrada con Spring Boot
		Application.launch(JavaFxApplication.class, args);
	}

}

