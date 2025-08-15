package com.shadowbot.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DashboardApplication {
    
    private static ConfigurableApplicationContext context;
    
    public static void startDashboard(String[] args) {
        System.setProperty("server.port", "3000");
        System.setProperty("spring.thymeleaf.cache", "false");
        System.setProperty("logging.level.org.springframework.security", "DEBUG");
        
        context = SpringApplication.run(DashboardApplication.class, args);
        System.out.println("🌐 Dashboard started on http://localhost:3000");
    }
    
    public static void stopDashboard() {
        if (context != null) {
            context.close();
        }
    }
    
    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}
