package com.github.webmorph.mixin.spel;

import com.github.webmorph.util.ReactiveSpelUtils;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.classtransform.InjectionCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import reactor.core.publisher.Mono;

@Slf4j
@Mixin(OpAnd.class)
public abstract class MixinOpAnd extends Operator {

    public MixinOpAnd(String payload, int startPos, int endPos, SpelNodeImpl... operands) {
        super(payload, startPos, endPos, operands);
    }

    @Inject(method = "Lorg/springframework/expression/spel/ast/OpAnd;getValueInternal(Lorg/springframework/expression/spel/ExpressionState;)Lorg/springframework/expression/TypedValue;",
            at = @At("HEAD"),
            cancellable = true)
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public void getValueInternal(ExpressionState state, InjectionCallback callback) {
        if (!ReactiveSpelUtils.isMethodSecurityExpressionContext(state)) return;
        Mono<Boolean> resultMono = ReactiveSpelUtils.toBooleanMono(this.getLeftOperand().getValue(state))
                .zipWith(ReactiveSpelUtils.toBooleanMono(this.getRightOperand().getValue(state)), Boolean::logicalAnd);
        callback.setReturnValue(new TypedValue(resultMono));
    }
}
