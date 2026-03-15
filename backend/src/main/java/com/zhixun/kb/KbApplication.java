package com.zhixun.kb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhixun.kb.mapper")
public class KbApplication {
    public static void main(String[] args) {
        SpringApplication.run(KbApplication.class, args);
    }
}
