package com.hikarishine.learn.reactive.handon;

import com.hikarishine.learn.reactive.handon.domain.User;
import com.hikarishine.learn.reactive.handon.repository.BlockingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Learn how to call blocking code from Reactive one with adapted concurrency strategy for
 * blocking code that produces or receives data.
 *
 * For those who know RxJava:
 *  - RxJava subscribeOn = Reactor subscribeOn
 *  - RxJava observeOn = Reactor publishOn
 *  - RxJava Schedulers.io <==> Reactor Schedulers.elastic
 *
 * @author Sebastien Deleuze
 * @see Flux#subscribeOn(Scheduler)
 * @see Flux#publishOn(Scheduler)
 * @see Schedulers
 */
public class Part11BlockingToReactive {

//========================================================================================

	// TODO Create a Flux for reading all users from the blocking repository deferred until the flux is subscribed, and run it with an elastic scheduler
	Flux<User> blockingRepositoryToFlux(BlockingRepository<User> repository) {
//		之前写错了，这题理解成延迟获取了，其实是想要延迟执行。repository延迟执行，直到有subscribe时才开始执行。
//		要做到这点，需要Flux从延迟执行的目标中获取，一般情况下延迟执行时通过Functional实现的，即defer。
//		return Flux.fromIterable(repository.findAll()).awaitOnSubscribe();
		return Flux.defer(() -> Flux.fromIterable(repository.findAll())).subscribeOn(Schedulers.elastic());
	}

//========================================================================================

	// TODO Insert users contained in the Flux parameter in the blocking repository using an elastic scheduler and return a Mono<Void> that signal the end of the operation
	Mono<Void> fluxToBlockingRepository(Flux<User> flux, BlockingRepository<User> repository) {
//		return Mono.empty(flux.subscribeOn(Schedulers.elastic()).doOnNext(u -> repository.save(u)));
//		标准答案
		return flux.publishOn(Schedulers.elastic()).doOnNext(repository::save).then();
	}

}
