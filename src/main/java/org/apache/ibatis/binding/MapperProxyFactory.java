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
 *  这个类负责创建具体 Mapper 接口实现类
 *  每一个 Mapper 接口都会对应一个工厂类 MapperProxyFactory
 *  不管是 mybatis 还是和 spring 结合，在初始化的时候添加 mapper 都会调用 {@link MapperRegistry#addMapper(Class)} 添加对应工厂
 *
 *  工厂 MapperProxyFactory 被创建后，不会主动创建具体的 mapper 实例，而是用户主动获取或者是 spring 主动注入，才会调用下面的 {@link #newInstance(MapperProxy)}
 *
 */
@SuppressWarnings("all")
public class MapperProxyFactory<T> {

  /** 具体Mapper接口的Class对象，也就是该工厂要创建对象的类型 */
  private final Class<T> mapperInterface;

  /**
   * 该接口下面方法的缓存
   * key：方法对象
   * value：对接口中方法对象的封装
   * 如果一个接口类有n个方法，那么map就会有n个
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

  /**
   * 为 mapper 类型创建代理类
   */
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
