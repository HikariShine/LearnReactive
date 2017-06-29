package com.hikarishine.learn.reactive.mytest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * FastBootWeixin  SpringReactor
 *
 * @author Guangshan
 * @summary FastBootWeixin  SpringReactor
 * @Copyright (c) 2017, Guangshan Group All Rights Reserved
 * @since 2017/6/25 22:31
 */
@SpringBootApplication
@RestController
public class SpringReactor {
    public static void main(String[] args) {
        SpringApplication.run(SpringReactor.class);
    }


    @GetMapping("hello/{who}")
    public Mono<String> hello(@PathVariable String who) {
        return Mono.just(who)
                .map(w -> "Hello " + w + "!");
    }

    @PostMapping("heyMister")
    public Flux<String> hey(@RequestBody Mono<String> body) {
        return Mono.just("Hey mister ")
                .concatWith(body
                        .flatMapMany(sir -> Flux.fromArray(sir.split("")))
                        .map(String::toUpperCase)
                        .take(1)
                ).concatWith(Mono.just(". how are you?"));
    }

}
