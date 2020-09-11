package com.severell.core.database.migrations;


import com.severell.core.database.grammar.Grammar;

public class Command {

    private Object[] params;
    private CommandType type;

    enum CommandType {
        CREATE{
            @Override
            public String compile(Blueprint table, Grammar grammar) {
                return grammar.create(table);
            }
        },
        DROP{
            @Override
            public String compile(Blueprint table,Grammar grammar) {
                return grammar.drop(table);
            }
        },
        ADD{
            @Override
            public String compile(Blueprint table,Grammar grammar) {
                return grammar.add(table);
            }
        };


        public abstract String compile(Blueprint table, Grammar grammar);
    }


    public Command(CommandType type, Object[] params) {
        this.type = type;
        this.params = params;
    }

    protected CommandType getType() {
        return this.type;
    }

    public String compile(Blueprint table, Grammar grammar) {
        return this.type.compile(table, grammar);
    }
}