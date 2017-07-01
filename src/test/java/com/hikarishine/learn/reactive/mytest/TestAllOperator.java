package com.hikarishine.learn.reactive.mytest;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class TestAllOperator {

	@Test
	public void testCombineLatest() throws InterruptedException {
		// combineLatest是在每个publisher发布一个数据时都把几个发布者的最后发布的数据拿出来并combine。
		// 下面执行的有个疑问，为什么不是从0-0-0开始？应该是subscribe的时候已经有些数据，那么问题来了，如何保证都收到？
		// interval如果只给一个Duration，则两个Duration都是同一个值。

		// 补充一个问题，如何保证从0-0-0开始显示？即不丢失任何发送的数据
		Flux.combineLatest(os -> os[0] + "-" + os[1] + "-" + os[2], 1,
				Flux.interval(Duration.ofMillis(500), Duration.ofMillis(100)).awaitOnSubscribe().onBackpressureBuffer(),
				Flux.interval(Duration.ofMillis(1000), Duration.ofMillis(200)).awaitOnSubscribe().onBackpressureBuffer(),
				Flux.interval(Duration.ofMillis(1500), Duration.ofMillis(300)).awaitOnSubscribe().onBackpressureBuffer())
				.awaitOnSubscribe()
				.onBackpressureBuffer()
				.subscribeOn(Schedulers.elastic())
				.toStream().forEach(System.out::println);
		Thread.sleep(5000);

		// 突然明白了，其实不是因为发送没收到才丢的数据，是因为第三个发布0的时候，第一个和第二个变成了10和2才这样的，丢了是正确的行为
		// 还有关于prefetch，我可能理解了，是预取的功能。对于cold Observable来说，是先主动Request(prefetch)拉prefetch个值
		// 等有需要的时候再从缓存中直接拿值，这个prefetch是默认就有的。其实和背压也是有点关系的。
		// 背压是自己主动去request，但是如果生产者生产数据较慢，则request可能会比较耗费时间，此时使用prefetch就提前去请求数据了
	}

	@Test
	public void testMergeAndMergeSequentialAndConcat() throws InterruptedException {
		Flux<Integer> flux1 = Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(200))
				.delayElements(Duration.ofMillis(100));
		Flux<Integer> flux2 = Flux.range(11, 10)
				.delaySubscription(Duration.ofMillis(100))
				.delayElements(Duration.ofMillis(300));
		// 这个就是普通的merge，每来一个元素都发布出去。
		System.out.println("merge");
		Flux.merge(flux1, flux2)
				.subscribe(System.out::println);
		Thread.sleep(5000);

		System.out.println("mergeSequential");
		flux1 = Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(4000))
				.delayElements(Duration.ofMillis(100));
		flux2 = Flux.range(11, 10)
				.delaySubscription(Duration.ofMillis(2000))
				.delayElements(Duration.ofMillis(300));
		// 顺序merge，最先到的元素所在的flux在全部发送出去之后，才发送第二个flux的全部，以此类推。
		// 有点类似于concat，这个sequential是指merge的sequential，就是参数的顺序。、
		// 与concat最终顺序也是一致的，唯一的不同就是mergeSequential是多个同时订阅，同时收取全部数据
		// 当一个flux全部发送出去之后，马上发送其他flux之前收集到的数据。相当于多个是同时订阅的。
		Flux.mergeSequential(flux1, flux2)
				.subscribe(System.out::println);
		Thread.sleep(8000);

		System.out.println("concat");
		flux1 = Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(4000))
				.delayElements(Duration.ofMillis(100));
		flux2 = Flux.range(11, 10)
				.delaySubscription(Duration.ofMillis(2000))
				.delayElements(Duration.ofMillis(300));
		// 这个与上面的不同是第一个订阅并且发布完成之后，才开始订阅第二个。
		Flux.concat(flux1, flux2)
				.subscribe(System.out::println);
		Thread.sleep(12000);
		// 还有两个delayError，请参考下面的testConcatDelayError
	}

	@Test
	public void testDefer() throws InterruptedException {
		// 延迟执行直到有订阅者，当有订阅者订阅时才执行defer内的supplier，返回publisher，开始获取数据。
		Flux flux = Flux.defer(() -> {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return Flux.range(0,10);
		}).delayElements(Duration.ofMillis(200));
		System.out.println("defer");
		Thread.sleep(3000);
		System.out.println("start");
		// 这之后又延迟了三秒，说明先执行了supplier，再订阅
		flux.subscribe(System.out::println);
		Thread.sleep(3000);
		flux = Flux.range(0,10).delayElements(Duration.ofMillis(200));
		System.out.println("no defer");
		Thread.sleep(3000);
		System.out.println("start");
		flux.subscribe(System.out::println);
		Thread.sleep(3000);
	}

	@Test
	public void testRangeAndInterval() throws InterruptedException {
		// 没啥说的，字面意思
		Flux.range(0 ,100).subscribe(System.out::println);
		// hot Observable，无限发送
		// 只有一个参数时是period，此时delay与period相同
		Flux.interval(Duration.ofMillis(1000)).subscribe(System.out::println);
		Thread.sleep(5000);
	}

	@Test
	public void testConcatDelayError() throws InterruptedException {
		// 与concat唯一的不同是如果某一个flux发送了一个error，这个concat会把error留着，直到其他正常的flux都发送完毕在发送这个error
		Flux.concatDelayError(Flux.error(new RuntimeException("试试")), Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(1000)).delayElements(Duration.ofMillis(200)))
				.subscribe(System.out::println, e -> System.out.println(e));
		Thread.sleep(5000);
		Flux.concat(Flux.error(new RuntimeException("试试")), Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(1000)).delayElements(Duration.ofMillis(200)))
				.subscribe(System.out::println, e -> System.out.println(e));
		Thread.sleep(5000);
	}

	@Test
	public void testSimpleFlux() {
		// 直接发送一个空的完成通知
		Flux.empty();
		// 直接发送一个错误
		Flux.error(new RuntimeException("只发送错误"));
		// 直接发送一个或多个数据
		Flux.just("1", "2");
	}

	@Test
	public void testFirstEmitting() throws InterruptedException {
		// 之前的amb操作符，选出第一个发送任何通知（包括error）的publisher，只发送他的全部值
		Flux.firstEmitting(Flux.range(1, 10)
				.delaySubscription(Duration.ofMillis(1500)).delayElements(Duration.ofMillis(200)),
				Flux.range(11, 10)
				.delaySubscription(Duration.ofMillis(1000)).delayElements(Duration.ofMillis(200)))
				.subscribe(System.out::println);
		Thread.sleep(5000);
	}

}
