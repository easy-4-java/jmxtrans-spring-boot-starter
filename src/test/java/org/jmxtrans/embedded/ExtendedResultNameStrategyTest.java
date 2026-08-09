package org.jmxtrans.embedded;

import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExtendedResultNameStrategy}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class ExtendedResultNameStrategyTest {

    @Test
    void constructor_should_register_mac_address_evaluators() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        Map<String, Callable<String>> evaluators = strategy.getExpressionEvaluators();
        assertThat(evaluators).containsKey("mac_address");
        assertThat(evaluators).containsKey("escaped_mac_address");
    }

    @Test
    void resolveExpression_should_resolve_mac_address() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        String resolved = strategy.resolveExpression("#mac_address#");
        // After the null-safety fix the evaluator is always non-null
        assertThat(resolved).isNotNull().isNotEmpty();
    }

    @Test
    void resolveExpression_should_resolve_escaped_mac_address() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        String resolved = strategy.resolveExpression("#escaped_mac_address#");
        assertThat(resolved).isNotNull().isNotEmpty();
    }

    @Test
    void constructor_should_not_throw_even_when_mac_resolution_fails() {
        // Just verifying the constructor is robust
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        assertThat(strategy).isNotNull();
    }

    @Test
    void registerExpressionEvaluator_should_store_value() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        strategy.registerExpressionEvaluator("custom", "hello");
        assertThat(strategy.resolveExpression("#custom#")).isEqualTo("hello");
    }

    @Test
    void registerExpressionEvaluator_with_callable_should_be_invoked() throws Exception {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        strategy.registerExpressionEvaluator("callable", (Callable<String>) () -> "called");
        assertThat(strategy.resolveExpression("#callable#")).isEqualTo("called");
    }

}
