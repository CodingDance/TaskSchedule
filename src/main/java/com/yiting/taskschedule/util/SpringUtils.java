/**
 * (C) Copyright Netease.com, Inc. 2015. All Rights Reserved.
 */
package com.yiting.taskschedule.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

/**
 * 获取Spring bean、placeholder值等。
 * @author Feng Changjian (hzfengchj@corp.netease.com)
 * @version $Id: SpringUtils.java, v 1.0 2015年3月6日 下午4:05:53
 */
@Component
public class SpringUtils implements ApplicationContextAware, EmbeddedValueResolverAware {

    private static final Logger log = LoggerFactory.getLogger(SpringUtils.class);

    private static ApplicationContext applicationContext;
    
    private static StringValueResolver stringValueResolver;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        SpringUtils.stringValueResolver = resolver;
    }
    
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    public static <T> T getBean(String beanName, Class<T> cls) {
        return applicationContext.getBean(beanName, cls);
    }

    public static Object getBeanByType(Class cls) {
        String[] beansName = applicationContext.getBeanNamesForType(cls);
        if (beansName.length == 0) {
            log.error("no such type:" + cls);
            throw new RuntimeException("no such type: " + cls);
        } else if (beansName.length > 1) {
            log.error("more than one beans match " + cls);
            throw new RuntimeException("more than one beans match " + cls);
        }
        return getBean(beansName[0]);
    }
    
    public static String getPlaceHolderValue(String name){
        return stringValueResolver.resolveStringValue(name);
    }
}