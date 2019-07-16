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
package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a database connection.
 * Handles the connection lifecycle that comprises: its creation, preparation, commit/rollback and close.
 *
 * @author Clinton Begin
 *
 * one-to-zero:
 *  数据库事务(以 mysql 为例)：
 *    1 开启事务：start transaction
 *    2 多条 sql 语句操作
 *    3 commit | rollback
 *
 *  JDBC中使用事务：
 *    当 Jdbc 程序向数据库获得一个 Connection 对象时，默认情况下这个 Connection 对象会自动向数据库提交在它上面发送的SQL语句。
 *    若想关闭这种默认提交方式，让多条SQL在一个事务中执行，则需要：
 *      Connection.setAutoCommit(false); 等价于 => 开启事务(start transaction)
 *      Connection.rollback();           等价于 => 回滚事务(rollback)
 *      Connection.commit();             等价于 => 提交事务(commit)
 *    也就是说 jdbc 控制事务其实就是通过禁用默认自动提交，采用手动提交，程序中，对于事务的操作通常是
 *      1 创建事务（create）
 *      2 提交事务（commit）
 *      3 回滚事务（rollback）
 *      4 关闭事务（close）
 *
 *
 * 事务的四大特性(ACID)：
 *    1 原子性（Atomicity）：是指事务是一个不可分割的工作单位，事务中的操作要么全部成功，要么全部失败
 *    2 一致性（Consistency）：事务必须使数据库从一个一致性状态变换到另外一个一致性状态
 *    3 隔离性（Isolation）：是多个用户并发访问数据库时，数据库为每一个用户开启的事务，不能被其他事务的操作数据所干扰，多个并发事务之间要相互隔离
 *    4 持久性（Durability）：是指一个事务一旦被提交，它对数据库中数据的改变就是永久性的，接下来即使数据库发生故障也不应该对其有任何影响
 *
 * 事务的隔离级别：
 *    1 脏读：指一个事务读取了另外一个事务未提交的数据
 *    2 不可重复读：指在一个事务内读取表中的某一行数据，多次读取结果不同
 *    3 幻读：指在一个事务内读取到了别的事务插入的数据，导致前后读取不一致
 *
 * 数据库共定义了四种隔离级别：
 *    1 Serializable(串行化)：可避免脏读、不可重复读、虚读情况的发生
 *    2 Repeatable read(可重复读)：可避免脏读、不可重复读情况的发生
 *    3 Read committed(读已提交)：可避免脏读情况发生
 *    4 Read uncommitted(读未提交)：最低级别，以上情况均无法保证
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *  注意：和数据源配置一样，通常项目中我们不会单独使用 mybatis 来管理事务。比如选择框架 Spring + mybatis，
 *  这时候没有必要配置事务管理器， 因为 Spring 模块会使用自带的管理器来覆盖前面的配置
 *
 *  事务类 Transaction 最主要的一个功能就是获取连接，根据 {@link org.apache.ibatis.mapping.Environment} 对象
 *  获取连接的操作都是在准备执行 sql 的时候
 *    {@link org.apache.ibatis.executor.SimpleExecutor#prepareStatement}
 *
 */
public interface Transaction {

  /**
   * Retrieve inner database connection.
   * @return DataBase connection
   * @throws SQLException
   */
  Connection getConnection() throws SQLException;

  /**
   * Commit inner database connection.
   * @throws SQLException
   */
  void commit() throws SQLException;

  /**
   * Rollback inner database connection.
   * @throws SQLException
   */
  void rollback() throws SQLException;

  /**
   * Close inner database connection.
   * @throws SQLException
   */
  void close() throws SQLException;

  /**
   * Get transaction timeout if set.
   * @throws SQLException
   */
  Integer getTimeout() throws SQLException;

}
