package io.github.wycst.wast.common.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wangy
 * @Date: 2021/8/9 22:42
 * @Description:
 */
public final class ExecutorServiceUtils {

    /**
     * 关闭线程池
     *
     * @param executorService
     */
    public static void shutdownExecutorService(ExecutorService executorService) {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Throwable e) {
            try {
                executorService.shutdownNow();
            } catch (Throwable throwable) {
            }
        }
    }

}
