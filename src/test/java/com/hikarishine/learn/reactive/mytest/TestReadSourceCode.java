package com.hikarishine.learn.reactive.mytest;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 看源码专用
 */
public class TestReadSourceCode {

    @Test
    public void requestDelayed() throws InterruptedException {
        AtomicBoolean state = new AtomicBoolean();
        AtomicReference<Throwable> e = new AtomicReference<>();

        // awaitOnSubscribe作用是延迟在onSubscribe方法中的request请求，这个请求不会在onSubscribe方法的执行过程中执行
        // 会等待onSubscribe方法执行完再有request请求。所以最后的status是true。
        // 下面有一个不delay的测试，最终结果是false。因为在请求时就会进入onNext，执行完onNext才执行open = true
        // 主要作用就是防止方法循环嵌套，几个on方法单独执行，可以看下源码，双装饰者模式。
        Flux.just(1)
                .awaitOnSubscribe()
                .subscribe(new Subscriber<Integer>() {
                    boolean open;

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                        open = true;
                    }

                    @Override
                    public void onNext(Integer t) {
                        state.set(open);
                    }

                    @Override
                    public void onError(Throwable t) {
                        e.set(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Assert.assertNull("Error: " + e.get(), e.get());
        Assert.assertTrue("Not open!", state.get());
        Thread.sleep(10000);
    }


    @Test
    public void requestNotDelayed() {
        AtomicBoolean state = new AtomicBoolean();
        AtomicReference<Throwable> e = new AtomicReference<>();

        Flux.just(1)
                .subscribe(new Subscriber<Integer>() {
                    boolean open;

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                        open = true;
                    }

                    @Override
                    public void onNext(Integer t) {
                        state.set(open);
                    }

                    @Override
                    public void onError(Throwable t) {
                        e.set(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Assert.assertNull("Error: " + e.get(), e.get());

        Assert.assertFalse("Request delayed!", state.get());
    }

    @Test
    public void cancelNotDelayed() {
        AtomicBoolean state1 = new AtomicBoolean();
        AtomicBoolean state2 = new AtomicBoolean();
        AtomicReference<Throwable> e = new AtomicReference<>();

        Flux.just(1)
                .doOnCancel(() -> state2.set(state1.get()))
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.cancel();
                        state1.set(true);
                    }

                    @Override
                    public void onNext(Integer t) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        e.set(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Assert.assertNull("Error: " + e.get(), e.get());

        Assert.assertFalse("Cancel executed before onSubscribe finished", state2.get());

    }


//	@Test
//	public void cancelDelayed() {
//		AtomicBoolean state1 = new AtomicBoolean();
//		AtomicBoolean state2 = new AtomicBoolean();
//		AtomicReference<Throwable> e = new AtomicReference<>();
//
//		DirectProcessor<Integer> sp = new DirectProcessor<>();
//
//		sp.awaitOnSubscribe()
//				.doOnCancel(() -> state2.set(state1.get()))
//				.subscribe(new Subscriber<Integer>() {
//					@Override
//					public void onSubscribe(Subscription s) {
//						s.cancel();
//						state1.set(true);
//					}
//
//					@Override
//					public void onNext(Integer t) {
//					}
//
//					@Override
//					public void onError(Throwable t) {
//						e.set(t);
//					}
//
//					@Override
//					public void onComplete() {
//
//					}
//				});
//
//		Assert.assertNull("Error: " + e.get(), e.get());
//
//		Assert.assertFalse("Cancel executed before onSubscribe finished", state2.get());
//		Assert.assertFalse("Has subscribers?!", sp.hasDownstreams());
//	}

}
