package com.github.webmorph.spel;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

interface OperatorExcludes {
    TypedValue getValueInternal(ExpressionState expressionState);

    String getOperatorName();

    Object getValue(ExpressionState expressionState);

    TypedValue getTypedValue(ExpressionState expressionState);
}
