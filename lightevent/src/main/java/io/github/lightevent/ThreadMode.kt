package io.github.lightevent

/**
 * 线程模式
 */
enum class ThreadMode {
    /**
     * 直接在当前线程调用action
     */
    POSTING,

    /**
     * 1. 若当前线程为主线程，直接在当前线程调用action
     * 2. 若当前线程为后台线程，将action post到主线程队列
     */
    MAIN,

    /**
     * 将action post到主线程队列
     */
    MAIN_ORDERED,

    /**
     * 1. 若当前线程为主线程，将action post到后台线程队列（串行执行）
     * 2. 若当前线程为后台线程，直接在当前线程调用action
     */
    BACKGROUND,

    /**
     * 将action post到后台线程队列（并发执行）
     */
    ASYNC
}