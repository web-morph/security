package com.github.webmorph.mixin;

import com.github.webmorph.spel.ReactiveOpAnd;
import com.github.webmorph.spel.ReactiveOpOr;
import dev.ckateptb.reflection.Reflect;
import dev.ckateptb.reflection.method.IReflectMethod;
import dev.ckateptb.reflection.type.IReflectClass;
import lombok.SneakyThrows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.lang.Nullable;

import java.util.function.Supplier;


@Mixin(targets = "org.springframework.expression.spel.standard.InternalSpelExpressionParser")
public abstract class MixinInternalSpelExpressionParser {
    private static final Object SYMBOLIC_OR = ((Supplier<Object>) () -> {
        try {
            return Reflect.on("org.springframework.expression.spel.standard.TokenKind")
                    .getFieldWithName("SYMBOLIC_OR")
                    .orElseThrow()
                    .getValue();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }).get();
    private static final Object SYMBOLIC_AND = ((Supplier<Object>) () -> {
        try {
            return Reflect.on("org.springframework.expression.spel.standard.TokenKind")
                    .getFieldWithName("SYMBOLIC_AND")
                    .orElseThrow()
                    .getValue();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }).get();
    private final IReflectMethod<Boolean> peekToken = ((Supplier<IReflectMethod<Boolean>>) () -> {
        try {
            return Reflect.on(this)
                    .getMethodWithNameAndParameters(
                            "peekToken",
                            Reflect.on("org.springframework.expression.spel.standard.TokenKind")
                    ).stream().findFirst().orElseThrow().cast(Boolean.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }).get();
    private final IReflectMethod<?> checkOperands = Reflect.on(this)
            .getMethodsWithName("checkOperands").stream().findFirst().orElseThrow();
    private final IReflectMethod<?> takeToken = Reflect.on(this)
            .getMethodWithNameAndParameters("takeToken").orElseThrow();

    @Shadow
    abstract boolean peekIdentifierToken(String identifierString);

    @Shadow
    abstract SpelNodeImpl eatRelationalExpression();

    @Overwrite
    @Nullable
    @SneakyThrows
    private SpelNodeImpl eatLogicalOrExpression() {
        SpelNodeImpl expr = eatLogicalAndExpression();
        while (peekIdentifierToken("or") || this.peekToken.invoke(SYMBOLIC_OR).getValue()) {
            IReflectClass<?> token = this.takeToken.invoke();
            Object t = token.getValue();  //consume OR
            SpelNodeImpl rhExpr = eatLogicalAndExpression();
            this.checkOperands.invoke(t, expr, rhExpr);
            expr = new ReactiveOpOr(
                    token.getFieldWithName("startPos").orElseThrow().cast(Integer.class).getValue(),
                    token.getFieldWithName("endPos").orElseThrow().cast(Integer.class).getValue(), expr, rhExpr);
        }
        return expr;
    }

    @Overwrite
    @Nullable
    @SneakyThrows
    private SpelNodeImpl eatLogicalAndExpression() {
        SpelNodeImpl expr = eatRelationalExpression();
        while (peekIdentifierToken("and") || this.peekToken.invoke(SYMBOLIC_AND).getValue()) {
            IReflectClass<?> token = this.takeToken.invoke();
            SpelNodeImpl rhExpr = eatRelationalExpression();
            this.checkOperands.invoke(token.getValue(), expr, rhExpr);
            expr = new ReactiveOpAnd(
                    token.getFieldWithName("startPos").orElseThrow().cast(Integer.class).getValue(),
                    token.getFieldWithName("endPos").orElseThrow().cast(Integer.class).getValue(), expr, rhExpr);
        }
        return expr;
    }
}
