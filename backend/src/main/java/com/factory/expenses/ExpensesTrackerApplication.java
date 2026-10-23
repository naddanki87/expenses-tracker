package com.factory.expenses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExpensesTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExpensesTrackerApplication.class, args);
  }
}
