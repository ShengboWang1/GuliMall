package com.shengbo.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 * 1.@EnableScheduling开启定时任务
 * 2.@Scheduled(cron="* * * * * ?")开启一个定时任务
 *
 * 异步任务
 * 1.@EnableAsync开启异步任务功能
 * 2.给希望异步执行的任务标注@Async
 */
@Slf4j
@Component
@EnableScheduling
@EnableAsync
public class HelloSchedule {

    /**
     * 在Spring中
     * 1。不允许第七位年
     * 2。周一到周日就是1-7 不是2345671
     * 3。定时任务默认是阻塞的, 但是他不应该阻塞(加Thread.sleep() 如果在sleep时正好该到定时任务触发这个时候不会触发 被阻塞了）
     *   那应该怎么办呢？
     *   1）可以让业务运行以异步的方式自己提交到线程池 CompletableFuture
     *   2）让定时任务异步执行 调用@Asyncv 这个！！！
     *
     *  解决办法：使用异步+定时任务解决定时任务不阻塞的功能
     *
     */
//    @Async
//    @Scheduled(cron="* * 0 * * 1") //周二的 每秒一次
//    public void hello() throws InterruptedException {
//        log.info("hello...");
//        Thread.sleep(3000);
//    }
}
