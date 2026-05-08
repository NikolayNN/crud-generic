package by.nhorushko.crudgenerictest;

import by.nhorushko.crudgeneric.flex.config.EnableAbsGenericCrud;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@EnableAbsGenericCrud
public class CrudGenericTestApp {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CrudGenericTestApp.class, args);
    }
}
