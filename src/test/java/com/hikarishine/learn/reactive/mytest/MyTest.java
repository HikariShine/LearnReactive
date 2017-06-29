package com.hikarishine.learn.reactive.mytest;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class MyTest {

	@Test
	public void testCombineLatest() throws InterruptedException {
		// combineLatest是在每个publisher发布一个数据时都把几个发布者的最后发布的数据拿出来并combine。
		// 下面执行的有个疑问，为什么不是从0-0-0开始？应该是subscribe的时候已经有些数据，那么问题来了，如何保证都收到？
		// interval如果只给一个Duration，则两个Duration都是同一个值。
		Flux.combineLatest(os -> os[0] + "-" + os[1] + "-" + os[2], 12,
				Flux.interval(Duration.ofMillis(500), Duration.ofMillis(100)).awaitOnSubscribe(),
				Flux.interval(Duration.ofMillis(1000), Duration.ofMillis(200)).awaitOnSubscribe(),
				Flux.interval(Duration.ofMillis(1500), Duration.ofMillis(300)).awaitOnSubscribe())
				.awaitOnSubscribe()
				.subscribeOn(Schedulers.elastic())
				.subscribe(System.out::println);
		Thread.sleep(15000);
	}

	public void testCombineLatest1() throws InterruptedException {
		Thread.sleep(3000);
	}

}
