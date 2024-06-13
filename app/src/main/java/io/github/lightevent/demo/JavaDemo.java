package io.github.lightevent.demo;

import android.util.Log;

import java.util.ArrayList;

import io.github.lightevent.EventBus;
import io.github.lightevent.EventHandler;
import io.github.lightevent.ThreadMode;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Java 用例
 */
public class JavaDemo {
    private static final Object subscriber = new Object();

    private static class MessageEvent {
        long time;
        MessageEvent(long time) {
            this.time = time;
        }
    }

    private static class LoginEvent {
    }

    void test() {
        ArrayList<EventHandler<?>> handlers = new ArrayList<>();

        // lambda 写法
        handlers.add(
                new EventHandler<MessageEvent>(
                        MessageEvent.class,
                        ThreadMode.POSTING,
                        false,
                        0,
                        event -> {
                            Log.d("Test", "data:" + event.time);
                            return null;
                        }
                )
        );

        // lambda 展开
        handlers.add(
                new EventHandler<LoginEvent>(
                        LoginEvent.class,
                        ThreadMode.MAIN,
                        false,
                        0,
                        new Function1<LoginEvent, Unit>() {
                            @Override
                            public Unit invoke(LoginEvent event) {
                                Log.d("Test", "event:" + event);
                                return null;
                            }
                        }
                )
        );

        EventBus.getDefault().register(handlers);
        EventBus.getDefault().post(new MessageEvent(System.currentTimeMillis()));
        EventBus.getDefault().unregister(handlers);
    }
}
