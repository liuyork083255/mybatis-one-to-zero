package liu.york;

import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;

/**
 * mybatis 生命周期
 *  SqlSessionFactoryBuilder：
 *      作用就是创建一个构建器，一旦创建了SqlSessionFactory，它的任务就算完成了，可以回收
 *  SqlSessionFactory：
 *      作用是创建 SqlSession，而 SqlSession 相当于JDBC的一个 Connection 对象，每次应用程序需要访问数据库，
 *      我们就要通过 SqlSessionFactory 创建一个SqlSession，所以 SqlSessionFactory 在整Mybatis整个生命周期中
 *      （每个数据库对应一个SqlSessionFactory，是单例产生的）
 *  SqlSession：
 *      生命周期是存在于请求数据库处理事务的过程中，是一个线程不安全的对象（在多线程的情况下，需要特别注意），
 *      即存活于一个应用的请求和申请，可以执行多条SQL保证事务的一致性
 *
 * SqlSession 一次执行的流程：
 *      通过内部的 Executor 创建一个 StatementHandler，而这 StatementHandler 内部就包含了两个对象(ParameterHandler、ResultSetHandler) {@link BaseStatementHandler}
 *      于是乎 Executor 调用 {@link StatementHandler#prepare} 设置请求数据库相关参数，然后调用 {@link StatementHandler#parameterize} 设置sql参数
 *      参数设置完成后就可以执行数据库操作，因为 StatementHandler 其实就是对 {@link java.sql.PreparedStatement} 封装，所以 StatementHandler 直接访问数据库
 *      又因为 StatementHandler 内部有 ResultSetHandler，所以执行sql完成后将结果集交给 {@link  ResultSetHandler#handleResultSets} 处理
 *
 *
 */
public class MainS {

}