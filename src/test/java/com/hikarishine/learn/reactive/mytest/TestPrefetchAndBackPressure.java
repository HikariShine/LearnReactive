package com.hikarishine.learn.reactive.mytest;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class TestPrefetchAndBackPressure {

	@Test
	public void testHotFluxPrefetch() throws InterruptedException {
		// 除了create之外还有个generate，不同之处在于create中的sink是FluxSink，支持pull和push，即支持cold和hot，也即同步和异步
		// on前缀是接收到pull请求，next是主动发送数据。
		// 而generate是SynchronousSink，只支持同步的发送，即next
		// 相同的还有Flux.push(FluxSink);他与create的唯一不同就是使用的是PUSH_ONLY模式
		// 他们最终调用的都是FluxCreate<>(emitter, OverflowStrategy.BUFFER, FluxCreate.CreateMode.PUSH_ONLY))
		// push的最后一个参数固定是PUSH_ONLY，而create是PUSH_PULL
		Flux<Integer> hotFlux = Flux.create(sink -> {
			// 这个是同步发送数据，属于hot的
			try {
				for (int i = 0; i < 10000; i++) {
					sink.next(i);
					System.out.println("send:" + i);
					Thread.sleep(1);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, FluxSink.OverflowStrategy.DROP);
//		Flux<Integer> newFlux = Flux.merge(1, hotFlux.onBackpressureDrop()).subscribeOn(Schedulers.parallel()).publishOn(Schedulers.elastic());
		hotFlux.subscribeOn(Schedulers.parallel()).publishOn(Schedulers.elastic()).subscribe(f -> {
			System.out.println("receive:" + f);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		// 上面测试hot，背压不会生效，但是默认有缓存，所以不会丢失
		// 但是奇怪的是，我设置背压为drop之后，还是能收到所有的数据，为何？发送方已经不管接收方是否能接收在一直不停的发了。
		Thread.sleep(1200000);
	}

	@Test
	public void testColdFluxPrefetch() throws InterruptedException {
		Flux<Integer> hotFlux = Flux.create(sink -> {
			// 第一次默认请求256个，当消费的只剩64个的时候会补充请求192个，填满256的空间。
			// 256和192是我调试观察到的，那么问题来了，他们的值是在哪里设置的呢？
			// @See PublishOnSubscriber类的构造方法中：
			// if (prefetch != Integer.MAX_VALUE) {
			// this.limit = prefetch - (prefetch >> 2);
			//} 果然是根据prefetch来的，这就串起来了。limit就是192。
			// 继续问：这个prefetch又是从哪里传递进来的？
			// 是从FluxPublishOn的构造方法来的，这个的构造方法调用时在执行publishOn方法中，这里能设置prefetch。
			// 如果不调用publishOn，默认的prefetch为Integer.MAX。
			sink.onRequest(c -> {
				System.out.println("send start");
				try {
					for (int i = 0; i < c; i++) {
						sink.next(i);
						System.out.println("send:" + i);
						Thread.sleep(1);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			// 这个是请求发送数据，属于cold的
		}, FluxSink.OverflowStrategy.BUFFER);
//		Flux<Integer> newFlux = Flux.merge(1, hotFlux.onBackpressureDrop()).subscribeOn(Schedulers.parallel()).publishOn(Schedulers.elastic());
		hotFlux.subscribeOn(Schedulers.parallel())
//				.publishOn(Schedulers.elastic())
				.subscribe(f -> {
					System.out.println("receive:" + f);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
		// 上面测试hot，背压不会生效，但是默认有缓存，所以不会丢失
		// 但是奇怪的是，我设置背压为drop之后，还是能收到所有的数据，为何？发送方已经不管接收方是否能接收在一直不停的发了。
		Thread.sleep(1200000);
	}

	@Test
	public void testHotFluxBackPressure() throws InterruptedException {
		Flux<Integer> hotFlux = Flux.create(sink -> {
			try {
				for (int i = 0; i < 1000; i++) {
					sink.next(i);
					System.out.println("send:" + i);
					Thread.sleep(1);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 这个是请求发送数据，属于hot的
		});
//		, FluxSink.OverflowStrategy.DROP);
		hotFlux.subscribeOn(Schedulers.parallel())
//				.publishOn(Schedulers.elastic(), 10)
				.publishOn(Schedulers.elastic())
				.subscribe(f -> {
					System.out.println("receive:" + f);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
		// 上面测试hot，背压不会生效，但是默认有缓存，所以不会丢失
		// 但是奇怪的是，我设置背压为drop之后，还是能收到所有的数据，为何？发送方已经不管接收方是否能接收在一直不停的发了。
		// 是因为create创建的observable使用了无界队列，所以可以把所有的数据保存起来。SpscLinkedArrayQueue这个类的offer方法
		// 如果检查到队列中当前offset已经有值了就会扩大队列。而且subscribe默认开启背压，这个队列会先放到subscriber的背压缓存中
		// 而不会令这个队列一直被生产出来的数据填充。
		// 这个队列的创建是在FluxCreate的createSink方法中生成的，如果OverflowStrategy是Buffer就会生成这个队列：
		// return new BufferAsyncSink<>(t, QueueSupplier.SMALL_BUFFER_SIZE);这里面创建了这个队列。默认大小256
		// 当我把策略改成Drop和publishOn的prefetch改成10之后，背压终于失效了！
		Thread.sleep(1200000);
	}

}
