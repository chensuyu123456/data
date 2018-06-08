package com.shellshellfish.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class DataApplication {


    public static void main(String[] args) {

        SpringApplication.run(DataApplication.class, args);
       // System.out.println(template.getDb().getName());
    }
}
