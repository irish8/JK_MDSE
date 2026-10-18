package com.jkmdse.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class PosApplication {
    public static void main(String[] args) { SpringApplication.run(PosApplication.class, args); }
    @Bean ApplicationRunner seedAdmin(JdbcTemplate db) { return args -> {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count == 0) db.update("INSERT INTO users(username,password_hash,full_name,role) VALUES(?,?,?,?)", "admin", PasswordUtil.hash("Admin999!"), "Store Administrator", "ADMIN");
    }; }
}
