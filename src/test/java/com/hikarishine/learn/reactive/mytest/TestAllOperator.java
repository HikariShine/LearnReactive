package com.hikarishine.learn.reactive.mytest;

import org.assertj.core.util.Lists;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		// 一个永远不发送complete的publisher
		Flux.never();
		// 从publisher创建
		Flux.from(Flux.just(1, 2));
		// 从迭代器创建
		Flux.fromIterable(Lists.newArrayList(1, 2, 3));
		// 从数组创建
		Flux.fromArray(new String[]{"1", "2", "3"});
		// 从Stream创建
		Flux.fromStream(Stream.of(1, 2, 3));
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

	@Test
	public void testZip() throws InterruptedException {
		// 把多个publisher发送的序列压缩在一起成为一个tuple，比如第一个是等待所有publisher发布一个之后才会发布
		// 所有publisher的第一个值放在一起。
		Flux.zip(Flux.range(1, 10), Flux.range(11, 10))
				.subscribe(tuple2 -> System.out.println(tuple2.getT1() + ":" + tuple2.getT2()));
		Thread.sleep(5000);
	}

	@Test
	public void testSwitchOnNext() throws InterruptedException {
		// switchOnNext接受的是一个publish publisher的publisher，当这个publisher发布后一个publisher时
		// 直接切断前一个的publish，开始发布后一个publisher publish的对象
		Flux.switchOnNext(Flux.just(Flux.range(1, 10).delaySubscription(Duration.ofMillis(500)).delayElements(Duration.ofMillis(100)),
				Flux.range(11, 10).delaySubscription(Duration.ofMillis(1500))).delayElements(Duration.ofMillis(800)), 1)
				.subscribe(System.out::println);
		Thread.sleep(8000);
		// 来解释下上面那一堆delay的作用
		// 首先第一个delaySubscription是为了延迟第一个range的订阅时间（其实不延迟订阅时间，延迟的是从订阅到第一个元素发送的时间）
		// 第二个delayElements延迟的是每一个元素的延迟时间。第三个只是延迟订阅时间。
		// 第四个延迟了just中每个元素的发送时间，所以最终的时间线是这样的：
		// 延迟0.8s后，发送第一个Publisher，此时第一个被订阅，延迟0.5s+0.1s发送第一个元素
		// 又过了0.3s，第二个Publisher被发送，此时立即出发第二个的订阅，第一个订阅无效，等待延迟1.5s后，开始发送所有元素
		// 最终元素序列应该及其时间线应该是:
		// 0	0.8	1.3	1.4	1.5	1.6	3.1
		// 		s1		1	2	s2	11-20
	}

	@Test
	public void testDelaySubscriptionAndDelayElements() throws InterruptedException {
		System.out.println("start");
		Flux.range(1, 10).delaySubscription(Duration.ofMillis(5000))
				.doOnSubscribe(s -> System.out.println("subscribe"))
				.subscribe(System.out::println);
		// dealySubscription不会延迟真实的被订阅时间，而是延迟订阅后第一个元素的发送时间，onSubscribe的调用还是很早
		Thread.sleep(10000);

		Flux.range(1, 10).delayElements(Duration.ofMillis(1000))
				.doOnSubscribe(s -> System.out.println("subscribe"))
				.subscribe(System.out::println);
		// delayElements延迟每一个元素发送时间，包括第一个

		Thread.sleep(10000);

		// 两个都使用，第一个元素发送时间等于两个时间之和
		Flux.range(1, 10).delayElements(Duration.ofMillis(2000)).delaySubscription(Duration.ofMillis(2000))
				.doOnSubscribe(s -> System.out.println("subscribe"))
				.subscribe(System.out::println);
		Thread.sleep(10000);
	}

	@Test
	public void using() {
		// 这个略微复杂，暂时不考虑吧
//		Flux.using()
	}

	@Test
	public void testCheckpoint() {
		// checkpoint打一个检查点，当检查点之前有error抛出时，这个error中的堆栈信息会包括当前这个检查点
		// 检查点之后的error不会包含这个堆栈信息，使用这种方式可以检查各种operate操作中是哪一步出的问题
		//Error has been observed by the following operator(s):
		//|_	Flux.checkpoint(TestAllOperator.java:269)
		Flux.concat(Flux.just(1, 2), Flux.just(3, 4))
				.checkpoint()
				.concatWith(Flux.error(new RuntimeException()))
				.checkpoint()
				.subscribe(e -> System.out.println(e), e -> e.printStackTrace());

		Flux.concat(Flux.just(1, 2), Flux.error(new RuntimeException()))
				.subscribe(e -> System.out.println(e), e -> e.printStackTrace());
	}

	@Test
	public void testMergeSelf() {
		// 竟然可以
		Flux flux = Flux.just(1, 2, 3).awaitOnSubscribe();
		flux.mergeWith(flux).subscribe(System.out::println);
	}

	@Test
	public void testSubscribeTwoTimesAndAwaitOnSubscribe() throws InterruptedException {
		// 竟然可以
		// 还是没有搞懂，注释的大致意思是防止onSubscribe的重入，这里方法名要这样分开理解await OnSubscribe
		// 即执行完一个onSubscribe才执行下一个onSubscribe
		// 确保这个onSubscribe方法的调用发生在子subscriber的该方法调用之后
		// 那么问题来了，什么是子subscriber？
		Flux<Integer> flux = Flux.just(1, 2, 3)
				.subscribeOn(Schedulers.single())
				.publishOn(Schedulers.elastic())
				.awaitOnSubscribe();
		flux.doOnSubscribe(a -> {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).subscribe(a -> {
			System.out.println(a);
			flux.subscribe(b -> System.out.println(b + 3));
		});
		Thread.sleep(10000);
	}

	@Test
	public void concatParent() throws InterruptedException {
		Flux<Integer> flux1 = Flux.just(1, 2, 3);
		Flux<Integer> flux2 = Flux.just(4, 5, 6);
		Flux.concat(Flux.just(flux1, flux2)
//				.awaitOnSubscribe()
				.doOnSubscribe(s -> System.out.println("parent subscribe")))
				.publishOn(Schedulers.elastic())
				.subscribeOn(Schedulers.parallel())
				.doOnSubscribe(s -> System.out.println("subscribe"))
				.subscribe(i -> System.out.println(i));
		Thread.sleep(5000);
	}


	@Test
	public void testOperate() {
		// 测试所有值，返回Mono<Boolean>
		Flux<Integer> flux = Flux.just(1, 2, 3, 4);
		flux.all(i -> i > 0);

		// 有一个值就返回true
		flux = Flux.just(1, 2, 3, 4);
		flux.any(i -> i > 0);

		// Flux转为T类型
		flux = Flux.just(1, 2, 3, 4);
		flux.as(f -> f.blockLast());

		// 等待第一个和最后一个，返回他们的值
		flux = Flux.just(1, 2, 3, 4);
		flux.blockFirst();
		flux = Flux.just(1, 2, 3, 4);
		flux.blockLast();

		// T转为List<T>
		flux = Flux.just(1, 2, 3, 4);
		flux.buffer();
		// 还有几个buffer系列方法，字面意思理解即可
		// 有个特殊的bufferWhen，提供一个open和close信号，这两个信号之间的元素被手机到buffer中。

		// 缓存几个，通过Flux<T>发出，与buffer相似，但是发送的是Flux。
		// 上面的理解错了，与buffer类似，发送的Flux<T>是window操作符
		// cache作用是把cold转为hot，并缓存param个值，下次再有订阅时从前几个缓存的第一个值开始发送
		flux = Flux.just(1, 2, 3, 4);
		flux.cache(3);

		flux = Flux.just(1, 2, 3, 4);
		flux.awaitOnSubscribe();

		// 类型转换
		flux = Flux.just(1, 2, 3, 4);
		flux.cast(Long.class);

		// 同Stream的collect，返回类型变为Mono<T>而已，还有几个类似的collect
		flux = Flux.just(1, 2, 3, 4);
		flux.collect(Collectors.toList());

		// 计数，返回Mono<Long>
		flux = Flux.just(1, 2, 3, 4);
		flux.count();

		// 如果为空，返回一个默认值
		flux = Flux.just(1, 2, 3, 4);
		flux.defaultIfEmpty(5);

		// 去重，以及可以自定义比较函数的去重方法（终于有了，其实是通过key来去重）
		flux = Flux.just(1, 2, 3, 4);
		flux.distinct(Function.identity());

		// 过滤没啥说的
		flux = Flux.just(1, 2, 3, 4);
		flux.filter(i -> i > 3);

		// 同stream的flatMap，展开为Flux，合并到一起
		flux = Flux.just(1, 2, 3, 4);
		flux.flatMap(i -> Flux.just(i, i + 1));

		// 选取某个元素
		flux = Flux.just(1, 2, 3, 4);
		flux.elementAt(3);

		// 同stream的groupBy，不同的是返回一个GroupedFlux，基本数据相同，多了一个key方法
		flux = Flux.just(1, 2, 3, 4);
		flux.groupBy(Integer::intValue);

		// 是否有某个元素
		flux = Flux.just(1, 2, 3, 4);
		flux.hasElement(3);


		// 类似于switchOnNext，不同之处在于onNext是自己提供了几个publisher，而这个的publisher来自于自己的map
		flux.switchMap(i -> Flux.just(1, 2));
		// 拿几个值
		flux.take(3);
		// 完成时执行一个动作，默认返回一个Mono<Void>，只发送完成事件
		flux.then();
		// 断路器，到一定时间还没有数据新数据被发送就使用后面的进行发送
		flux.timeout(Duration.ofMillis(1000), Flux.just(1, 2));
		// 转为带时间戳的tuple2
		flux.timestamp();
		// 如您所见
		flux.toIterable();
		// 如您所见
		flux.toStream();
		// 把一个Flux转为另一个Flux，同操作还有个compose
		Function<Flux<Integer>, Flux<Integer>> applySchedulers = f -> f.subscribeOn(Schedulers.elastic())
				.publishOn(Schedulers.parallel());
		flux.transform(applySchedulers).map(v -> v * v).subscribe();
		// compose与这个的不同之处在于transform是直接进行装配，compose则是生成defer
		// 假设参数中有较耗时的操作，则可以使用compose
		flux.compose(applySchedulers);
		// 类似于buffer，但是发送的是Flux而不是List
		flux.window(Duration.ofMillis(1000));
		// 忽略所有元素，返回只发出Complete的Mono
		flux.ignoreElements();
		// 取最后一个元素
		flux.last();
		// 记录日志，操作日志
		flux.log();
		// map映射
		flux.map(Function.identity());
		// 把Flux的第一个元素转为Mono<T>
		flux.next();
		// 筛选出Object类型的元素并转换
		flux.ofType(Object.class);
		// 把Error转为Exception
		flux.onErrorMap(e1 -> new RuntimeException(e1.getClass().getName()));
		// flux并行化
		flux.parallel();
		// 同stream的reduce
		flux.reduce((a, b) -> a + b);
		// 重复从开始到结束的序列
		flux.repeat();
		// 当出错是尝试从开始重新执行并发送，默认重试无限次
		flux.retry();
		// 取样，取这段时间内最后发送的值
		flux.sample(Duration.ofMillis(1000));
		// 取第一个发送的值
		flux.sampleFirst(Duration.ofMillis(1000));
		// 这个不能顾名思义了，类似于reduce，但是他是直接取前一个值和当前值做运算，返回为新值，比map多往前拿了一个值
		// 还有一点要注意的是，前一个值拿的是新flux的前一个值，而不是原始的值
		flux.scan((a, b) -> a * b);
		// 对于只有一个值的flux，返回Mono<T>，没有值的返回NoSuchElementException，多于一个值的返回IndexOutOfBoundsException
		flux.single();
		// 跳过
		flux.skip(3);
		// 排序
		flux.sort();
		// 在第一个值之前插入参数值
		flux.startWith(0);
		// 下面两个类似于序列化和反序列化,materialize物质化
		// 把Flux<T>转换为Signal<T>
		flux.materialize();
		// 反物质化，把Signal<T>转为Flux<T>
		flux.dematerialize();
		// 类似于timestamp，但是tuple的第一个时间是两个元素发射间隔，elapsed，流逝消逝
		flux.elapsed();

		// 限制背压的prefetch
		flux.limitRate(2);
	}

	@Test
	public void test() {
		Flux<Integer> flux = Flux.just(1, 2, 3, 4);
		// 未知或者不是太明白的方法：
		flux.awaitOnSubscribe();
//		flux.groupJoin()
//		flux.join()
		flux.hide();
//		flux.handle()
		flux.publish();
		flux.replay();
		// 不知道干啥的，同publish().refCount();
		flux.share();
//		flux.subscribeWith()
	}

}
