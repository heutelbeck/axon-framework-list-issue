package com.heutelbeck;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import com.heutelbeck.QueryTestsuite.TestScenarioConfiguration;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestScenarioConfiguration.class)
public abstract class QueryTestsuite {

	private static final String LIST_RESPONSE_QUERY = "listResponseQuery";

	@Autowired
	QueryGateway queryGateway;

	@Autowired
	QueryUpdateEmitter emitter;

	@Test
	void when_listUpdatesAreEmitted_then_updatesContainTheLists() throws JsonProcessingException {
		var emitIntervallMs = 20L;
		var queryPayload    = "case1";
		var numberOfUpdates = 20L;
		var timeout         = Duration.ofMillis(emitIntervallMs * (numberOfUpdates + 2L));

		var subscriptionResult = queryGateway.subscriptionQuery(LIST_RESPONSE_QUERY, queryPayload,
				ResponseTypes.multipleInstancesOf(String.class), ResponseTypes.multipleInstancesOf(String.class));

		StepVerifier.create(subscriptionResult.initialResult())
				.expectNext(List.of("Ada", "Alan", "Alice", "Bob"))
				.verifyComplete();

		/*
		 * ATTENTION: This is the critical issue!
		 * 
		 * This List is the update to be emitted. In this case, the specific List
		 * implementation is LinkedList. This works fine in any case.
		 */
		var updateList = new LinkedList<String>();
		updateList.addAll(List.of("Gerald", "Tina"));

		assertThat(updateList.getClass().getSimpleName(), is("LinkedList"));

		Flux.interval(Duration.ofMillis(emitIntervallMs))
				.doOnNext(i -> emitter.emit(query -> query.getPayload().toString().equals(queryPayload),
						updateList))
				.take(Duration.ofMillis(emitIntervallMs * numberOfUpdates + emitIntervallMs / 2L)).subscribe();

		StepVerifier.create(subscriptionResult.updates().take(2).timeout(timeout))
				.expectNext(List.of("Gerald", "Tina"), List.of("Gerald", "Tina"))
				.verifyComplete();
	}

	@Test
	void when_listUpdatesAreEmittedAndListsAreList12_then_updatesContainTheLists() throws JsonProcessingException {
		var emitIntervallMs = 20L;
		var queryPayload    = "case2";
		var numberOfUpdates = 20L;
		var timeout         = Duration.ofMillis(emitIntervallMs * (numberOfUpdates + 2L));

		var subscriptionResult = queryGateway.subscriptionQuery(LIST_RESPONSE_QUERY, queryPayload,
				ResponseTypes.multipleInstancesOf(String.class), ResponseTypes.multipleInstancesOf(String.class));

		StepVerifier.create(subscriptionResult.initialResult())
				.expectNext(List.of("Ada", "Alan", "Alice", "Bob"))
				.verifyComplete();

		/*
		 * ATTENTION: This is the critical issue!
		 * 
		 * This List is the update to be emitted. In this case, the specific List
		 * implementation is an immutable List created the List.of(...) convenience
		 * method. Here the implementation type is a List12.
		 * 
		 * Expected: No difference and the updates are delivered.
		 * 
		 * Actual behavior: 
		 *   - InMemory local operation: works 
		 *   - With AxonServer: update is dropped.
		 */
		var updateList = List.of("Gerald", "Tina");
		assertThat(updateList.getClass().getSimpleName(), is("List12"));

		Flux.interval(Duration.ofMillis(emitIntervallMs))
				.doOnNext(i -> emitter.emit(query -> query.getPayload().toString().equals(queryPayload),
						updateList))
				.take(Duration.ofMillis(emitIntervallMs * numberOfUpdates + emitIntervallMs / 2L)).subscribe();

		StepVerifier.create(subscriptionResult.updates().take(2).timeout(timeout))
				.expectNext(List.of("Gerald", "Tina"), List.of("Gerald", "Tina"))
				.verifyComplete();
	}

	@Test
	void when_listUpdatesAreEmittedAndListsAreListN_then_updatesContainTheLists() throws JsonProcessingException {
		var emitIntervallMs = 20L;
		var queryPayload    = "case3";
		var numberOfUpdates = 20L;
		var timeout         = Duration.ofMillis(emitIntervallMs * (numberOfUpdates + 2L));

		var subscriptionResult = queryGateway.subscriptionQuery(LIST_RESPONSE_QUERY, queryPayload,
				ResponseTypes.multipleInstancesOf(String.class), ResponseTypes.multipleInstancesOf(String.class));

		StepVerifier.create(subscriptionResult.initialResult())
				.expectNext(List.of("Ada", "Alan", "Alice", "Bob"))
				.verifyComplete();

		/*
		 * ATTENTION: This is the critical issue!
		 * 
		 * This List is the update to be emitted. In this case, the specific List
		 * implementation is an immutable List created the .toList() convenience method
		 * of a steam. Here the implementation type is a ListN.
		 * 
		 * Expected: No difference and the updates are delivered.
		 * 
		 * Actual behavior: 
		 *   - InMemory local operation: works 
		 *   - With AxonServer: updateis dropped.
		 */
		var updateList = List.of("Gerald", "Tina").stream().toList();
		assertThat(updateList.getClass().getSimpleName(), is("ListN"));

		Flux.interval(Duration.ofMillis(emitIntervallMs))
				.doOnNext(i -> emitter.emit(query -> query.getPayload().toString().equals(queryPayload),
						updateList))
				.take(Duration.ofMillis(emitIntervallMs * numberOfUpdates + emitIntervallMs / 2L)).subscribe();

		StepVerifier.create(subscriptionResult.updates().take(2).timeout(timeout))
				.expectNext(List.of("Gerald", "Tina"), List.of("Gerald", "Tina"))
				.verifyComplete();
	}

	static class Projection {
		@QueryHandler(queryName = LIST_RESPONSE_QUERY)
		public List<String> handleListResponse(String query) {
			return List.of("Ada", "Alan", "Alice", "Bob");
		}
	}

	@Configuration
	static class TestScenarioConfiguration {
		@Bean
		Projection projection() {
			return new Projection();
		}
	}

}
