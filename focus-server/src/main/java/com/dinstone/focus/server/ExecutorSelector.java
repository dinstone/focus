package com.dinstone.focus.server;

import java.util.concurrent.Executor;

public interface ExecutorSelector {

    Executor select(String g, String s, String m);

}
