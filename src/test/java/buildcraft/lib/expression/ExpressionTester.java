package buildcraft.lib.expression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.node.value.NodeVariableDouble;

public class ExpressionTester {
    @Test
    void arithmeticPrecedenceAndMathFunctionsAreStable() throws InvalidExpressionException {
        Assertions.assertEquals(-96, GenericExpressionCompiler.compileExpressionLong("165 + 15 - 6 * 46").evaluate());
        Assertions.assertEquals(
            32.0,
            GenericExpressionCompiler.compileExpressionDouble("pow(2, 5)").evaluate(),
            0.000_001
        );
        Assertions.assertEquals(
            1.0,
            GenericExpressionCompiler.compileExpressionDouble("sin(radians(90))").evaluate(),
            0.000_001
        );
    }

    @Test
    void comparisonsBooleanOperatorsAndTernariesCompile() throws InvalidExpressionException {
        Assertions.assertTrue(GenericExpressionCompiler.compileExpressionBoolean("1 <= 2 && 4 != 5").evaluate());
        Assertions.assertFalse(GenericExpressionCompiler.compileExpressionBoolean("!true || 3 > 9").evaluate());
        Assertions.assertEquals(
            "larger",
            GenericExpressionCompiler.compileExpressionString("1 <= 5^2-1 ? 'larger' : 'smaller'").evaluate()
        );
    }

    @Test
    void compiledExpressionsObserveVariableChanges() throws InvalidExpressionException {
        FunctionContext context = new FunctionContext(DefaultContexts.createWithAll());
        NodeVariableDouble input = context.putVariableDouble("input");
        var expression = GenericExpressionCompiler.compileExpressionDouble("input * 2 + 1", context);

        input.value = 1;
        Assertions.assertEquals(3.0, expression.evaluate(), 0.000_001);
        input.value = 30;
        Assertions.assertEquals(61.0, expression.evaluate(), 0.000_001);
    }

    @Test
    void typeMismatchAndMalformedSyntaxAreRejected() {
        Assertions.assertThrows(
            InvalidExpressionException.class,
            () -> GenericExpressionCompiler.compileExpressionLong("'not a number'")
        );
        Assertions.assertThrows(
            InvalidExpressionException.class,
            () -> GenericExpressionCompiler.compileExpressionBoolean("1 +")
        );
    }
}
