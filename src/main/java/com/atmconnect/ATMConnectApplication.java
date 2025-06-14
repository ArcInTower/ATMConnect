package com.atmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableTransactionManagement
public class ATMConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ATMConnectApplication.class, args);
	}

}