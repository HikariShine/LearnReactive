package com.hikarishine.learn.reactive.mytest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@RunWith(JUnit4.class)
public class ReactorTest {

    private static List<String> words = Arrays.asList(
            "the",
            "quick",
            "brown",
            "fox",
            "jumped",
            "over",
            "the",
            "lazy",
            "dog"
    );

    @Test
    public void testReactor2() {
        Flux<String> manyLetters =
                Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .groupBy(Function.identity())
                .sort(Comparator.comparing(GroupedFlux::key))
                .map(a -> String.format("%s:%d", a.key(), a.count().block()));
        manyLetters.subscribe(System.out::println);
    }
    @Test
    public void testReactor1() {
        Flux<String> manyLetters = Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .distinct()
                .sort()
                .zipWith(Flux.range(1, Integer.MAX_VALUE),
                        (string, count) -> String.format("%2d. %s", count, string));
        manyLetters.subscribe(System.out::println);
    }
    @Test
    public void shortCircuit() {
        Flux<String> helloPauseWorld =
                Mono.just("Hello")
                        .concatWith(Mono.just("world")
                                .delaySubscription(Duration.ofMillis(2000l)));
        helloPauseWorld.subscribe(System.out::println);
    }

    @Test
    public void firstEmitting() {
        Mono<String> a = Mono.just("oops I'm late")
                .delaySubscription(Duration.ofMillis(450l));
        Flux<String> b = Flux.just("let's get", "the party", "started")
                .delayElements(Duration.ofMillis(400l));

        Flux.firstEmitting(a, b)
                .toIterable()
                .forEach(System.out::println);
    }

    @Test
    public void testMono() throws InterruptedException {
        Mono<String> a = Mono.fromCallable(() -> "oops I'm mono").delayElement(Duration.ofMillis(1200l));
        Thread.sleep(1000l);
        a.subscribe(System.out::println);
        // 后面如果不加sleep还是打印不出来，因为上面加过delay之后就会使用非主线程来运行subscribe，而主线程结束
        // 没来得及运行subscribe中的过程就结束了，所以打印不出来。以及如果Mono在subscribe时还没有元素被producer
        // 之后就算生产了也不会再被这个订阅执行了。所以Mono的subscribe其实是告诉producer我可以接受你的生产内容了
        // 此时生产者把生产好的结果交给subscribe执行。
        Thread.sleep(1000l);
        // 而Flux不同，Flux是直接生成元素并开始发送，如果subscribe没有收到，并没有什么影响。subscribe只会接受在订阅之后发送的值
        Flux<String> b = Flux.just("oops I'm flux", "oops I'm flux too").delayElements(Duration.ofMillis(600l));
        // 纠正一下上面的结论，Flux貌似也是从第一个订阅开始才发送值，但是还有另外一个结论，delayElements其实是在subscribe第一个值后delay时间。
        Thread.sleep(1000l);
        b.subscribe(System.out::println);
        Thread.sleep(1300l);
    }

    @Test
    public void testSubscribe() throws InterruptedException {
        Mono<String> helloPauseWorld =
                Mono.fromCallable(() -> {
                    System.out.println("call");
                    Thread.sleep(1000);
                    return "haha";
                });
        System.out.println("start");
        Thread.sleep(2000);
        System.out.println("sleep over");
        helloPauseWorld.subscribe(System.out::println);
        Thread.sleep(1500);
        // 上面可以看出来，fromCallable中的lambda是在订阅前就执行的。
    }

    @Test
    public void testInterval() throws InterruptedException {
        Flux.interval(Duration.ofMillis(500), Duration.ofMillis(300)).subscribe(System.out::println);
        Thread.sleep(5000);
    }

    @Test
    public void testMergeWith() throws InterruptedException {
        Flux.interval(Duration.ofMillis(200))
                .mergeWith(Flux.interval(Duration.ofMillis(200)))
                        .subscribe(System.out::println);
        Thread.sleep(5000);
    }

    @Test
    public void testMonoWhen() throws InterruptedException {
        Tuple2<Long, Long> nowAndLater =
                Mono.when(
                        Mono.just(System.currentTimeMillis()),
                        Mono.just(1l).delayElement(Duration.ofMillis(10)).map(i -> System.currentTimeMillis()))
                        .block();
        System.out.println(nowAndLater.toString());
    }

    @Test
    public void testMonoParallel() throws InterruptedException {
        Mono.fromCallable( () -> System.currentTimeMillis() )
                .repeat()
                .parallel(8) //parallelism
                .runOn(Schedulers.parallel())
                .doOnNext( d -> System.out.println("I'm on thread "+Thread.currentThread()) )
                .sequential()
                .subscribe(System.out::println);
        Thread.sleep(1000);
    }


}
