package com.dinstone.focus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FutureTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletableFuture<String> f = new CompletableFuture<String>();
        executorService.execute(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] supplyAsync ");
            f.complete("Hello");
        });

        // CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        // System.out.println(Thread.currentThread().getName() + " supplyAsync ");
        // return "Hello";
        // });

        // ...
        CompletableFuture<String> future = f.whenComplete((v, e) -> {
            System.out.println("[" + Thread.currentThread().getName() + "] whenComplete");
            System.out.println(v);
            System.out.println(e);
        });
        future = future.thenApply(s -> {
            System.out.println("[" + Thread.currentThread().getName() + "] apply");
            return s + " World";
        });
        System.out.println("get result : " + future.get());

        future.thenAccept(s -> {
            System.out.println("[" + Thread.currentThread().getName() + "] accept");
            System.out.println("accept : " + s);
        }).thenRun(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] run");
        });
    }

}
