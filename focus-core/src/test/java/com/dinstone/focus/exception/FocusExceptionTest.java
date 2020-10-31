package com.dinstone.focus.exception;

import org.junit.Test;

public class FocusExceptionTest {

    @Test
    public void test() {
        FocusException f = new FocusException("name is empty", "\t case stack");
        f.printStackTrace();

        new RuntimeException("sdfasdf", new FocusException("asdfasdfasdf")).printStackTrace();
    }

}
