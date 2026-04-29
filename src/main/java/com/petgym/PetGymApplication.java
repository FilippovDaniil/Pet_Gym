package com.petgym; // объявляем пакет (папку), в котором находится этот класс

import org.springframework.boot.SpringApplication; // класс для запуска Spring Boot приложения
import org.springframework.boot.autoconfigure.SpringBootApplication; // аннотация, включающая авто-конфигурацию Spring Boot
import org.springframework.scheduling.annotation.EnableScheduling; // аннотация, включающая планировщик задач (cron-задачи)

@SpringBootApplication // говорим Spring Boot: "это главный класс приложения, сканируй всё вокруг и настраивай сам"
@EnableScheduling      // включаем поддержку @Scheduled-методов (запуск по расписанию)
public class PetGymApplication {
    public static void main(String[] args) { // точка входа: с этого метода JVM запускает приложение
        SpringApplication.run(PetGymApplication.class, args); // запускаем Spring Boot, передаём главный класс и аргументы командной строки
    }
}
