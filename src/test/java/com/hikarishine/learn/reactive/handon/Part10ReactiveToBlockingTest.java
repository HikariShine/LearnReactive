package com.hikarishine.learn.reactive.handon;

import java.util.Iterator;

import com.hikarishine.learn.reactive.handon.domain.User;
import com.hikarishine.learn.reactive.handon.repository.ReactiveRepository;
import com.hikarishine.learn.reactive.handon.repository.ReactiveUserRepository;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Learn how to turn Reactive API to blocking one.
 *
 * @author Sebastien Deleuze
 */
public class Part10ReactiveToBlockingTest {

	Part10ReactiveToBlocking workshop = new Part10ReactiveToBlocking();
	ReactiveRepository<User> repository = new ReactiveUserRepository();

//========================================================================================

	@Test
	public void mono() {
		Mono<User> mono = repository.findFirst();
		User user = workshop.monoToValue(mono);
		assertEquals(User.SKYLER, user);
	}

//========================================================================================

	@Test
	public void flux() {
		Flux<User> flux = repository.findAll();
		Iterable<User> users = workshop.fluxToValues(flux);
		Iterator<User> it = users.iterator();
		assertEquals(User.SKYLER, it.next());
		assertEquals(User.JESSE, it.next());
		assertEquals(User.WALTER, it.next());
		assertEquals(User.SAUL, it.next());
		assertFalse(it.hasNext());
	}

}
