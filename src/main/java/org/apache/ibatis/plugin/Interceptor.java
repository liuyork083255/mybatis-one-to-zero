/**
 *    Copyright 2009-2015 the original author or authors.
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

import java.util.Properties;

/**
 * @author Clinton Begin
 * one-to-zero:
 *  自定义拦截器必须实现的接口
 */
public interface Interceptor {

  /**
   * 拦截的方法用户自定义逻辑
   * 需要注意的是：如果需要保证 mybatis 逻辑不变，就需要调用 {@link Invocation#proceed()}
   */
  Object intercept(Invocation invocation) throws Throwable;

  /**
   * 参数 target 就是四大对象之一
   * 具体可以查看：{@link org.apache.ibatis.session.Configuration} newXxx 方法
   *        以及 {@link InterceptorChain#pluginAll(Object)} 方法
   *
   * 逻辑就是：根据这个代理对象(四大对象之一)和所有的拦截器 {@link InterceptorChain#interceptors} 进行匹配，
   *  匹配规则就是根据用户自定义的拦截器要对哪个对象的那些方法进行拦截(就是利用{@link Intercepts}和{@link Signature}两个注解规则)
   *  具体详见 {@link Plugin#wrap(Object, Interceptor)}
   *
   *  如果需要拦截就生成一个代理对象 {@link Plugin#wrap(Object, Interceptor)}
   *  如果不需要代理则直接返回参数本身 target
   *
   */
  Object plugin(Object target);


  /**
   * 在使用拦截器的时候可以用户自定义输入参数
   *
   * <plugin interceptor="">
   *    <property name="username" value="LiuYork" />
   * </plugin>
   *
   * 这个方法会在被解析 xml 或者初始化拦截器的时候被调用，所以如果有参数传入，
   * 那么需要用成员变量将之保存
   * {@link org.apache.ibatis.builder.xml.XMLConfigBuilder#pluginElement}
   *
   */
  void setProperties(Properties properties);

}
