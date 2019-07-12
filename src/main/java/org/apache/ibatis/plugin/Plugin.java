/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 * one-to-zero:
 *   Plugin 类其实就是一个代理类，因为它实现了jdk动态代理接口 InvocationHandler
 *   我们核心只需要关注两个方法
 *   wrap：
 *        这个方法就是 mybatis 提供给开发人员使用的一个工具类方法，
 *        目的就是帮助开发人员省略掉 反射解析注解 Intercepts 和 Signature，有兴趣的可以去看看源码 Plugin#getSignatureMap 方法
 *
 *   invoke：
 *        这个方法就是根据 wrap 方法的解析结果，判断当前拦截器是否需要进行拦截，
 *        如果需要拦截：将 目标对象+目标方法+目标参数 封装成一个 Invocation 对象，给我们自定义的拦截器 MyInterceptor 的 intercept 方法
 *                     处理好了之后是否需要调用目标对象的方法，比如 打印了sql语句，是否还要查询数据库呢？答案是肯定的
 *        如果不需要拦截：则直接调用目标对象的方法
 *                     比如直接调用 Executor 的 update 方法进行更新数据库
 */
public class Plugin implements InvocationHandler {

  /**
   * 被拦截目标对象，也就是原始的四大对象之一
   * 这里这么说可能不准确了，因为可能存在多层代理，所以这个 target 可能是前面一个拦截器的代理类
   */
  private final Object target;

  /** 用户自定义拦截器 */
  private final Interceptor interceptor;

  /** {@link #getSignatureMap(Interceptor)} 返回值 */
  private final Map<Class<?>, Set<Method>> signatureMap;

  private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
    this.target = target;
    this.interceptor = interceptor;
    this.signatureMap = signatureMap;
  }

  /**
   * 封装代理对象
   *
   * @param target    四大对象之一，这里这么说可能不准确了，因为可能存在多层代理，所以这个 target 可能是前面一个拦截器的代理类
   * @param interceptor 用户自定义拦截器
   */
  public static Object wrap(Object target, Interceptor interceptor) {
    /* 获取拦截的名称和方法集合 */
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);

    /* 目标类型 */
    Class<?> type = target.getClass();

    /* 拦截的目标接口 */
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap);

    /* 如果有拦截的接口，返回目标对象的代理对象 */
    if (interfaces.length > 0) {
      /* 返回目标对象的代理对象 */
      return Proxy.newProxyInstance(type.getClassLoader(), interfaces, new Plugin(target, interceptor, signatureMap));
    }
    /* 如果没有，直接返回 */
    return target;
  }

  /**
   * 当调用目标对象的任何方法都会进入这里，因为 jdk 动态代理会拦截所有方法
   * @param proxy   代理对象
   * @param method  被代理的当前执行方法
   * @param args    但代理的当前执行方法的参数
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      /*
       * method.getDeclaringClass() 方法获取对应接口类型，就是四大对象之一
       */
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());

      /**
       * 其实这里才是真正判断方法是否匹配，在 {@link #getAllInterfaces}中只是判断了接口类型
       */
      if (methods != null && methods.contains(method)) {
        /* 执行拦截器方法逻辑，这里将目标 对象-方法-参数 封装成了一个 Invocation 方法 */
        return interceptor.intercept(new Invocation(target, method, args));
      }

      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }

  /**
   * 解析自定义拦截器
   * 返回 map
   *  key：  拦截的四大对象类全名
   *  value：拦截具体方法集合
   *
   *  比如拦截 {@link org.apache.ibatis.executor.Executor}
   *     update(MappedStatement ms, Object parameter)
   *     query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
   *  则：
   *  key：  org.apache.ibatis.executor.Executor
   *  value：[updateMethod, queryMethod]
   *
   */
  private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
    /* 获取拦截器类注解 Intercepts */
    Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
    // issue #251
    if (interceptsAnnotation == null) {
      throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
    }

    /* 获取拦截器标记位，因为有多个，所以是数组 */
    Signature[] sigs = interceptsAnnotation.value();

    Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
    for (Signature sig : sigs) {
      /* 定义一个set集合用于存放拦截的方法 */
      Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
      try {
        Method method = sig.type().getMethod(sig.method(), sig.args());
        methods.add(method);
      } catch (NoSuchMethodException e) {
        throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
      }
    }
    return signatureMap;
  }

  /**
   * 根据参数 type 类型，判断 signatureMap 中是否包含这个类型，如果有则以数组形式返回
   *
   * @param type    目标类 类型
   * @param signatureMap  {@link #getSignatureMap} 返回值
   */
  private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (type != null) {
      /*
       * 获取目标类型的接口
       * 因为这个时候 type 是四大对象的之一的实现类
       */
      for (Class<?> c : type.getInterfaces()) {
        /*
         * 只要拦截器有这个类型，就加入
         * 看来并没有方法是否匹配，只判断了接口类型
         */
        if (signatureMap.containsKey(c)) {
          interfaces.add(c);
        }
      }
      /* 获取当前类的父类 */
      type = type.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[interfaces.size()]);
  }

}
