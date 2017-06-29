/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hikarishine.learn.reactive.handon;

import java.util.function.Function;

import com.hikarishine.learn.reactive.handon.domain.User;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/**
 * Learn how to deal with errors.
 *
 * @author Sebastien Deleuze
 * @see Exceptions#propagate(Throwable)
 * @see Hooks#onOperator(Function)
 */
public class Part07Errors {

//========================================================================================

	// TODO Return a Mono<User> containing User.SAUL when an error occurs in the input Mono, else do not change the input Mono.
	Mono<User> betterCallSaulForBogusMono(Mono<User> mono) {
		return mono.onErrorReturn(User.SAUL);
	}

//========================================================================================

	// TODO Return a Flux<User> containing User.SAUL and User.JESSE when an error occurs in the input Flux, else do not change the input Flux.
	Flux<User> betterCallSaulAndJesseForBogusFlux(Flux<User> flux) {
		return flux.onErrorResume(e -> Flux.just(User.SAUL, User.JESSE));
	}

//========================================================================================

	// TODO Implement a method that capitalizes each user of the incoming flux using the
	// #capitalizeUser method and emits an error containing a GetOutOfHereException error
	Flux<User> capitalizeMany(Flux<User> flux) {
		// 苦思冥想，查看方法，终于找到了这种方法，但是其实这个不是标准调用方法
		return flux.flatMap(u -> {
			try {
				return Flux.just(capitalizeUser(u));
			} catch (GetOutOfHereException e) {
				// 这里不catch有问题，因为是非运行时异常，不捕获不能在lambda中使用，如果是非运行时异常，直接调用即可，非运行时异常会被rx自动捕获并通过error方法抛出
				return Flux.error(e);
			}
		});
		// 下面是官网给的答案，参考一下：
//		return flux.map(user -> {
//			try {
//				return capitalizeUser(user);
//			}
//			catch (GetOutOfHereException e) {
//				throw Exceptions.propagate(e);
//			}
//		});
	}

	User capitalizeUser(User user) throws GetOutOfHereException {
		if (user.equals(User.SAUL)) {
			throw new GetOutOfHereException();
		}
		return new User(user.getUsername(), user.getFirstname(), user.getLastname());
	}

	protected final class GetOutOfHereException extends Exception {
	}

}
