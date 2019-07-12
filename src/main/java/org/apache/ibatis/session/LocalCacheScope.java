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
package org.apache.ibatis.session;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * @author Eduardo Macarron
 * one-to-zero:
 *  mybatis 的一级缓存是无法关闭的，但是可以通过修改缓存级别达到session不缓存效果，
 *  就是将缓存级别设置成 STATEMENT
 *  在每次查询结束后，myabits 都会将结果缓存起来，但是如果在两个以上的数据库节点，这个缓存出现致命 bug
 *  所以需要去除 mybatis 的一级缓存，二级缓存默认是关闭的
 *  {@link org.apache.ibatis.executor.BaseExecutor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
 *
 *  设置一级缓存，在配置文件中：
 *        <settings>
 *            <setting name="localCacheScope" value="STATEMENT"/>
 *        </settings>
 *
 */
public enum LocalCacheScope {
  /**
   * session 级别的缓存
   * 针对同一个 session 的所有查询，mybatis 默认都会缓存
   */
  SESSION,

  /**
   * statement 级别的缓存
   * 也就是针对当前这个语句缓存，但是同一个 session 的不同查询是不会使用缓存的
   */
  STATEMENT
}
