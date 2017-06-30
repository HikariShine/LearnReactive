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
	public void testMergeAndMergeSequential() {
		
	}

}
