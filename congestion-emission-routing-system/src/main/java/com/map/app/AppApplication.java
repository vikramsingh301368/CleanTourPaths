package com.map.app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import com.map.app.service.TrafficAndRoutingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping; 
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, XADataSourceAutoConfiguration.class })
public class AppApplication {

    @Autowired
    TrafficAndRoutingService ts;    

    private static void insertCMDProperties(String[] args) {
        try {
            for (String arg : args) {
                if (arg.length() <= 2 || arg.charAt(0) != '-' && arg.charAt(1) != '-') {
                    throw new IOException();
                }
                String[] map = arg.substring(2).split("=");
                if (map.length != 2) {
                    throw new IOException();
                }
                Properties prop = new Properties();
                try (FileInputStream ip = new FileInputStream("config.properties")) {
                    prop.load(ip);
                    if (prop.getProperty(map[0]) == null) {
                        throw new IOException();
                    } else {
                        prop.setProperty(map[0], map[1]);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Config properties are not valid. Aborting ...");
                }
                prop.store(new FileOutputStream("config.properties"), null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Invalid argument. Please follow proper syntax. Aborting...");
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            throw new RuntimeException("Enter arguments in comma-separated fashion");
        }
        if (args.length == 1) {
            String[] CmdArgs = args[0].split(",");
            insertCMDProperties(CmdArgs);
        }
        SpringApplication.run(AppApplication.class, args);
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    void jobInitializer() {
        ts.start();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")  
                    .allowedOrigins("http://localhost:3001/", "http://localhost:300/","https://tsaas.iitr.ac.in/") 
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }

    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations(ResourceUtils.CLASSPATH_URL_PREFIX + "/static/");
    }
}

@Configuration
@EnableScheduling
class SchedulingConfiguration {

}

@RestController
@RequestMapping("/api")
class DummyController {

    @PostMapping("/process")
    public String processInput(@RequestBody String input) {
        return "Processed: IIT R" + input;
    }

    @GetMapping("/test")
    public String test() {
        return "Test endpoint done!";
    }
}
