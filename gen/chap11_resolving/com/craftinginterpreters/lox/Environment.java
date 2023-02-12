package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class Environment
{
    final Environment enclosing;
    //private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> values = new TreeMap<>();
    private List<Object> var_list = new ArrayList<>();
    int bsize = 0;
    Environment() { enclosing = null; }

    Environment(Environment enclosing) { this.enclosing = enclosing; }
    Environment(Environment enclosing, int size) {
        this.enclosing = enclosing;
        this.bsize = size;
        this.var_list = new ArrayList<>(size);
    }

    Object get(Token name, int idx)
    {
        if (bsize != 0) {
            return var_list.get(idx);
        }
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null)
            return enclosing.get(name, idx);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, int idx, Object value)
    {
        if (bsize != 0) {
            var_list.set(idx, value);
            return;
        }
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, idx, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    void define(String name, Object value) { values.put(name, value); }
    Environment ancestor(int distance)
    {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing; // [coupled]
        }

        return environment;
    }
    Object getAt(int distance, String name, int idx) {
        Environment env = ancestor(distance);
        if (env.bsize != 0) return env.var_list.get(idx);
        else return env.values.get(name);
    }
    void assignAt(int distance, Token name, int idx, Object value) { ancestor(distance).assign(name, idx, value); }
    @Override public String toString()
    {
        String result = values.toString();
        if (enclosing != null) {
            result += " -> " + enclosing.toString();
        }

        return result;
    }
}
