package com.red404;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ServletComponentScan
public class Red404Application {
    public static void main(String[] args) {

        System.setProperty("tomcat.util.http.parser.HttpParser.requestTargetAllow", "}{|\\^`<>[\"]");
        SpringApplication.run(Red404Application.class, args);
    }
}
