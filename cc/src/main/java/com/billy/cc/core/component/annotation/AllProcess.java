package com.billy.cc.core.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记组件在所有进程内各有一个该组件对象，每个进程调用自身进程内部的对象，从而达到所有进程共用的目的（不会导致跨进程调用）
 * @author billy.qi
 * @since 18/9/14 23:21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AllProcess {
}
