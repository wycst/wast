//package org.framework.light.clients.http.netty;
//
//import org.framework.light.clients.http.definition.HttpClientResponse;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
///**
// * @Author: wangy
// * @Date: 2020/7/2 18:32
// * @Description:
// */
//public class HttpClientResponseFuture {
//
//    private CountDownLatch countDownLatch = new CountDownLatch(1);
//    private HttpClientResponse httpClientResponse;
//
//    public void setHttpResponse(HttpClientResponse httpClientResponse) {
//        this.httpClientResponse = httpClientResponse;
//        countDownLatch.countDown();
//    }
//
//    public HttpClientResponse getHttpClientResponse(long timeout, TimeUnit unit) {
//        try {
//            countDownLatch.await(timeout, unit);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return httpClientResponse;
//    }
//
//}
