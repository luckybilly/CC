package com.billy.cc.core.component;

/**
 * 全局拦截器
 * 注：为了防止开发阶段跨app调用组件时全局拦截器重复执行，全局拦截器应全部放在公共库中，供所有组件依赖，
 * 跨app调用组件时，只会执行调用方app的全局拦截器，被调用方app内的全局拦截器不执行
 * @author billy.qi
 * @since 18/5/26 10:05
 */
public interface IGlobalCCInterceptor extends ICCInterceptor {
    /**
     * 优先级，(可重复,相同的优先级其执行顺序将得不到保障)
     * @return 全局拦截器的优先级，按从大到小的顺序执行
     */
    int priority();

}
