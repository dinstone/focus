package com.dinstone.focus.protocol;

public interface ReplyListener {

    public void complete(Reply reply, Throwable error);

}
