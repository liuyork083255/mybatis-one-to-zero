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
package org.apache.ibatis.datasource;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * @author Clinton Begin
 * one-to-zero:
 *  DataSource 工厂，mybatis 支持三种类型的 DataSource，
 *  分别是：
 *    JNDI:这个数据源的实现是为了能在如 EJB 或应用服务器这类容器中使用，容器可以集中或在外部配置数据源，然后放置一个 JNDI 上下文的引用
 *    POOLED:这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来，避免了创建新的连接实例时所必需的初始化和认证时间
 *    UNPOOLED:不使用连接池,这个数据源的实现只是每次被请求时打开和关闭连接
 *
 * 由于有三种类型，所以对应的工厂也有三种：
 *    {@link org.apache.ibatis.datasource.jndi.JndiDataSourceFactory}
 *    {@link org.apache.ibatis.datasource.pooled.PooledDataSourceFactory}
 *    {@link org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory}
 *
 */
public interface DataSourceFactory {

  /**
   * 此时的 properties 中就是
   * 解析后的 environments 中的 environment 中的 dataSource 标签下面的属性
   *    driver    = com.mysql.jdbc.Driver
   *    url       = jdbc:mysql://localhost:3306/mybatis-one-to-zero1
   *    username  = root
   *    password  = 123456
   *
   */
  void setProperties(Properties props);

  DataSource getDataSource();

}
