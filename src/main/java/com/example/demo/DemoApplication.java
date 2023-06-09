package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Instant;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	record Greeting(String name){
	}

	@RestController
	class GreedingHttpController {
		@Autowired
		GreetingClient gc;
		@GetMapping("/greetings/{name}")
		ResponseEntity<Greeting> greet(@PathVariable String name, @RequestHeader Map<String, String> headers){
			var good = StringUtils.hasText(name) && Character.isUpperCase(name.charAt(0));
			if(!good)
				throw new IllegalArgumentException("the name must start with an upper case");

			HttpHeaders responseHeaders = new HttpHeaders();
			headers.entrySet().stream().forEach(e->responseHeaders.set(e.getKey(),e.getValue()));

			return ResponseEntity.ok()
					.headers(responseHeaders)
					.body(new Greeting("Hello, " + name + "!"));
		}
	}

	@ControllerAdvice
	class ProblemDetailErrorHandelingControllerAdvice{
		@ExceptionHandler(IllegalArgumentException.class)
		ProblemDetail onException(Exception ex){
			ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "the name is invalid");
			problemDetail.setTitle("Bookmark Not Found");
			problemDetail.setProperty("errorCategory", "Generic");
			problemDetail.setProperty("timestamp", Instant.now());
			return problemDetail;
		}
	}

	interface GreetingClient {
		@GetExchange("/greetings/{name}")
		ResponseEntity<Greeting> greet(@PathVariable String name, @RequestHeader Map<String, String> headers);

	}

	@Bean
	HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder builder){
		var wc = builder.baseUrl("http://localhost:8080");
		return HttpServiceProxyFactory
				.builder(WebClientAdapter.forClient(wc.build()))
				.build();
	}
	@Bean
	GreetingClient greetingClient(HttpServiceProxyFactory httpServiceProxyFactory){
		return httpServiceProxyFactory.createClient(GreetingClient.class);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(GreetingClient gc){
		return event -> {
			try {
				var response = gc.greet("cdm team", Map.of("CustomHeader", "MiuHeader"));
				System.out.println(response.getHeaders());
				System.out.println("result: "+response.getBody());
			}catch (Exception ex){
				System.out.println(ex);
			}

		};
	}
}
