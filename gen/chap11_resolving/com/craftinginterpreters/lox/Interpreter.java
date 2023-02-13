package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    private final Map<Expr, Integer> index = new HashMap<>();
    private final Map<Stmt, Integer> bsize = new HashMap<>();

    Interpreter()
    {
        globals.define("clock", -1, new LoxCallable() {
            @Override public int arity() { return 0; }

            @Override public Object call(Interpreter interpreter, List<Object> arguments)
            {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override public String toString() { return "<native fn>"; }
            });
    }

    void interpret(List<Stmt> statements)
    {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }
    private Object evaluate(Expr expr) { return expr.accept(this); }
    private void execute(Stmt stmt) { stmt.accept(this); }
    void resolveBlock(Stmt.Block blockStmt, int size) {
        bsize.put(blockStmt, size);
    }
    void resolveFunction(Stmt.Function functionStmt, int size) {
        bsize.put(functionStmt, size);
    }
    void resolve(Expr expr, int depth, int offset) {
        locals.put(expr, depth);
        index.put(expr, offset);
    }
    void executeBlock(List<Stmt> statements, Environment environment)
    {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
    @Override public Void visitBlockStmt(Stmt.Block stmt)
    {
        executeBlock(stmt.statements, new Environment(environment, bsize.get(stmt)));
        return null;
    }
    @Override public Void visitExpressionStmt(Stmt.Expression stmt)
    {
        evaluate(stmt.expression);
        return null;
    }
    @Override public Void visitFunctionStmt(Stmt.Function stmt)
    {
        LoxFunction function = new LoxFunction(stmt, environment, bsize.get(stmt));
        System.out.println("func: " + stmt.name.lexeme + " bsize: " + bsize.get(stmt));
        environment.define(stmt.name.lexeme, bsize.get(stmt), function);
        return null;
    }
    @Override public Void visitIfStmt(Stmt.If stmt)
    {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override public Void visitPrintStmt(Stmt.Print stmt)
    {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }
    @Override public Void visitReturnStmt(Stmt.Return stmt)
    {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }
    @Override public Void visitVarStmt(Stmt.Var stmt)
    {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        int idx = bsize.get(stmt) == null ? -1 : bsize.get(stmt);
        environment.define(stmt.name.lexeme, idx, value);
        return null;
    }
    @Override public Void visitWhileStmt(Stmt.While stmt)
    {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    @Override public Object visitAssignExpr(Expr.Assign expr)
    {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, index.get(expr), value);
        } else {
            globals.assign(expr.name, -1, value);
        }

        return value;
    }
    @Override public Object visitBinaryExpr(Expr.Binary expr)
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); // [left]

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } // [plus]

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }

        // Unreachable.
        return null;
    }
    @Override public Object visitCallExpr(Expr.Call expr)
    {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { // [in-order]
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }
    @Override public Object visitGroupingExpr(Expr.Grouping expr) { return evaluate(expr.expression); }
    @Override public Object visitLiteralExpr(Expr.Literal expr) { return expr.value; }
    @Override public Object visitLogicalExpr(Expr.Logical expr)
    {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }
    @Override public Object visitUnaryExpr(Expr.Unary expr)
    {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }

        // Unreachable.
        return null;
    }
    @Override public Object visitVariableExpr(Expr.Variable expr) { return lookUpVariable(expr.name, expr); }
    private Object lookUpVariable(Token name, Expr expr)
    {
        Integer distance = locals.get(expr);
        if (distance != null) {
            System.out.println("name: " + name.lexeme + " expr: " + expr.toString() + " distance:" + distance);
            return environment.getAt(distance, name.lexeme, index.get(expr));
        } else {
            return globals.get(name, -1);
        }
    }
    private void checkNumberOperand(Token operator, Object operand)
    {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right)
    {
        if (left instanceof Double && right instanceof Double)
            return;
        // [operand]
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    private boolean isTruthy(Object object)
    {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }
    private boolean isEqual(Object a, Object b)
    {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }
    private String stringify(Object object)
    {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
