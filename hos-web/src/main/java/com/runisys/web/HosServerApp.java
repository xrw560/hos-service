package com.runisys.web;

import com.runisys.mybatis.HosDataSourceConfig;
import com.runisys.web.security.SecurityInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.*;

@EnableWebMvc
@SuppressWarnings("deprecation")
@Configuration
@ComponentScan({"com.runisys.*"})
@SpringBootApplication
@Import({HosDataSourceConfig.class,HosServerBeanConfiguration.class})
@MapperScan("com.runisys")
public class HosServerApp {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    public static void main(String[] args) {
        SpringApplication.run(HosServerApp.class);
    }

    @Bean
    public WebMvcConfigurer configurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                super.addInterceptors(registry);
                registry.addInterceptor(securityInterceptor);
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }

}
