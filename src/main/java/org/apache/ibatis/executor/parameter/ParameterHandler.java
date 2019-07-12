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
package org.apache.ibatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter handler sets the parameters of the {@code PreparedStatement}.
 *
 * @author Clinton Begin
 * one-to-zero:
 *  处理 SQL 参数，参数映射是涉及到两个步骤，
 *  一是将查询请求参数映射到 prepareStatement 中，这步骤就是 ParameterHandler 的工作
 *  二是将查询结果映射到结果集中，这是 ResultSetHandler 的工作
 */
public interface ParameterHandler {

  /**
   * 返回参数对象
   */
  Object getParameterObject();

  /**
   * 设置预编译参数
   */
  void setParameters(PreparedStatement ps) throws SQLException;

}
