package liu.york.pointClass;

/**
 * mybatis好 缓存分为一级缓存和二级缓存
 *
 * 一级缓存：
 *      Mybatis 对每一次会话都添加了缓存操作。这个缓存的作用域为一次会话中。缓存随着会话(SqlSession)的创建而产生，
 *      随着会话结束而释放。对一次会话的查询操作，总是先查看缓存中是否存在查询结果，如果存在则直接取缓存中的结果，
 *      不存在则查询数据库。这样的话，一次会话中的完全相同的查询则只会查询一次，节省了系统资源
 *
 *      一级缓存是对象是 {@link org.apache.ibatis.executor.BaseExecutor#localCache}
 *      一级缓存默认开启，如果要关闭只能设置缓存级别为 {@link org.apache.ibatis.session.LocalCacheScope#STATEMENT}
 *
 *
 * 二级缓存：
 *      二级缓存是 mapper 级别的缓存，也就是同一个 namespace 的 mapper.xml，当多个SqlSession使用同一个Mapper操作数据库的时候，
 *      得到的数据会缓存在同一个二级缓存区域。
 *      二级缓存的开启需要两个条件，一个是开启全局开关，第二是就是在 mapper.xml 文件中配置 cache 节点
 *      1 <setting name="cacheEnabled" value="true"/> 查看 {@link org.apache.ibatis.session.Configuration#cacheEnabled} 其实默认是打开的
 *      2 在 Mapper.xml中配置
 *          <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>  ->  当前mapper下所有语句开启二级缓存
 *          这里配置了一个LRU缓存，并每隔60秒刷新，最大存储512个对象，而却返回的对象是只读的
 *          如果想禁用 mapper.xml 文件下的某一个查询： 添加useCache="false"
 *              <select id="selectUser" resultType="rm_map" statementType="CALLABLE" useCache="false">
 *
 * 　1.当一个 sqlSession 执行了一次select后，在关闭此session的时候，会将查询结果缓存到二级缓存
 *　 2.当另一个 sqlSession 执行select时，首先会在他自己的一级缓存中找，如果没找到，就回去二级缓存中找，找到了就返回，
 *     就不用去数据库了，从而减少了数据库压力提高了性能　
 *
 * 这里提一下在和 spring 结合后，一级缓存其实也是有效的，但是问题是所有spring为每一个非事务查询都新建一个 sqlSession，
 * 逻辑就是：spring 里面和 mapperProxy 绑定的 sqlSession 是 SqlSessionTemplate，它里面有一个 sqlSession 的代理对象 sqlSessionProxy
 *          该代理对象逻辑就是在每次调用 {@link org.apache.ibatis.session.SqlSession} 原生方法的时候都进行代理，
 *          代理逻辑就是新建一个 sqlSession，用完之后就立马关闭
 *
 *   引申：事务，如果在方法上使用了 spring 的事务注解，那么这个方法里面的所有查询共享一个 sqlSession （非事务都是新建session）
 *         做法就是将 session 绑定到 threadLocal 中
 *
 *
 */
public class CacheS {
}