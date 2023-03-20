package com.anhui.fabricbaasweb.listener;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * springboot监听器参考:
 * <a href="https://blog.csdn.net/qq_36364521/article/details/120553076">springboot监听器参考地址</a>
 *
 * TIPS:
 * 实现ApplicationListener接口针对单一事件监听
 * 实现SmartApplicationListener接口针对多事件监听
 * Order值越小越先执行
 * application.properties中定义的优于其他方式
 */
@Order(4)
@Component
public class FourthListener implements SmartApplicationListener {
    /**
     * 注册感兴趣的事件
     * @param eventType
     * @return
     */
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationStartedEvent.class.isAssignableFrom(eventType) ||
                ApplicationPreparedEvent.class.isAssignableFrom(eventType);
    }

    /**
     * 接收到事件后的动作
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("FourthListener : hello " + event.getClass());
    }
}

