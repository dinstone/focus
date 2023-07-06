/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

public class FutureTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // extracted();

        Executor executor = Executors.newCachedThreadPool();
        CompletableFuture<Integer> result = Stream.of(1, 2)
                .map(x -> CompletableFuture.supplyAsync(() -> compute(x), executor))
                .reduce(CompletableFuture.completedFuture(0), (x, y) -> x.thenCombineAsync(y, Integer::sum, executor));

        // 等待结果
        try {
            System.out.println("[" + Thread.currentThread().getName() + "]: 结果：" + result.get());
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("任务执行异常");
        }
    }

    private static Integer compute(Integer x) {
        try {
            System.out.println("[" + Thread.currentThread().getName() + "]: 任务开始执行: " + x);
            Thread.sleep(1000);
            System.out.println("[" + Thread.currentThread().getName() + "]: 任务完成执行: " + x);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return x;
    }

    private static void time(Function<Void, Void> fn) {
        fn.apply(null);
    }

    private static void extracted() throws InterruptedException, ExecutionException {
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
