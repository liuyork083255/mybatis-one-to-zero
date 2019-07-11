package liu.york.demo.demo2;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

@Intercepts({
        @Signature(method = "query", type = Executor.class, args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
//        @Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class Demo2Interceptor1 implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        Object username = properties.get("username");
        System.out.println(username);
    }
}