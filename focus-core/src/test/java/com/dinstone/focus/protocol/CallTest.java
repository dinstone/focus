package com.dinstone.focus.protocol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.dinstone.photon.message.Headers;

public class CallTest {

    @Test
    public void testAttach() {
        Headers hs = new Headers();
        hs.add("seq", "one").add("seq", "two").add("seq", "three");
//        for (int i = 0; i < 100000; i++) {
//            hs.add("seq", "i" + i);
//        }

        String v = hs.get("seq");
        assertEquals(v, "one");
    }

}
