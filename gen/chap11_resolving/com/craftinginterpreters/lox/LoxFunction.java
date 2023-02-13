package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable
{
    private final Stmt.Function declaration;
    private final Environment closure;
    private int bsize;

    LoxFunction(Stmt.Function declaration, Environment closure, int bsize)
    {
        this.closure = closure;
        this.declaration = declaration;
        this.bsize = bsize;
    }
    @Override public String toString() { return "<fn " + declaration.name.lexeme + ">"; }
    @Override public int arity() { return declaration.params.size(); }
    @Override public Object call(Interpreter interpreter, List<Object> arguments)
    {
        Environment environment = new Environment(closure, bsize);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, i, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }
}
