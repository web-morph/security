package com.github.webmorph.spel;

import lombok.experimental.Delegate;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class ReactiveOpAnd extends Operator {
    @Delegate(excludes = OperatorExcludes.class)
    private final OpAnd delegate;

    public ReactiveOpAnd(int startPos, int endPos, SpelNodeImpl... operands) {
        super("and", startPos, endPos, operands);
        this.delegate = new OpAnd(startPos, endPos, operands);
    }

    @NonNull
    @Override
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public TypedValue getValueInternal(@NonNull ExpressionState state) throws EvaluationException {
        if (!ReactiveSpelUtils.isMethodSecurityExpressionContext(state)) this.delegate.getValueInternal(state);
        Mono<Boolean> resultMono = ReactiveSpelUtils.toBooleanMono(this.getLeftOperand().getValue(state))
                .zipWith(ReactiveSpelUtils.toBooleanMono(this.getRightOperand().getValue(state)), Boolean::logicalAnd);
        return new TypedValue(resultMono);
    }
}
