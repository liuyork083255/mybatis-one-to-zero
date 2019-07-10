/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * @author Lasse Voss
 * one-to-zero:
 *  这个类负责创建具体 Mapper 接口代理对象的工厂类
 */
@SuppressWarnings("all")
public class MapperProxyFactory<T> {

  /** 具体Mapper接口的Class对象 */
  private final Class<T> mapperInterface;

  /**
   * 该接口下面方法的缓存
   * key：方法对象
   * value：对接口中方法对象的封装
   */
  private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<>();

  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  public Map<Method, MapperMethod> getMethodCache() {
    return methodCache;
  }

  protected T newInstance(MapperProxy<T> mapperProxy) {
    /*
     * 可以发现，mybatis 使用的是 jdk 动态代理
     */
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  /**
   * 传入sqlSession 创建一个Mapper接口的代理类
   */
  public T newInstance(SqlSession sqlSession) {
    /**
     * 这里创建了 {@link MapperProxy} 对象 这个类实现了JDK的动态代理接口 InvocationHandler
     */
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }

}
