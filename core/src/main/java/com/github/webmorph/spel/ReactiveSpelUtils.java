package com.github.webmorph.spel;

import lombok.experimental.UtilityClass;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import reactor.core.publisher.Mono;

@UtilityClass
public class ReactiveSpelUtils {
    public boolean isMethodSecurityExpressionContext(ExpressionState state) {
        //noinspection ConstantConditions
        return MethodSecurityExpressionOperations.class.isAssignableFrom(state.getRootContextObject().getTypeDescriptor().getType());
    }

    public Mono<?> toMono(Object value) throws EvaluationException {
        if (value instanceof Mono<?> mono) return Mono.from(mono);
        else return Mono.justOrEmpty(value);
    }

    public Mono<Boolean> toBooleanMono(Object value) throws EvaluationException {
        if (value == null)
            throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", Boolean.class);
        if (value instanceof Boolean bool) return Mono.just(bool);
        if (value instanceof Mono<?> mono) {
            return Mono.from(mono)
                    .flatMap(o -> {
                        if (o instanceof Boolean bool) return Mono.just(bool);
                        else return Mono.error(new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, o.getClass(), Boolean.class));
                    });
        }
        throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, value.getClass(), Boolean.class);
    }
}
