package liu.york.demo.demo1;

import com.alibaba.fastjson.JSON;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;

public class Demo1Run {

    public static void main(String[] args) throws Exception {
        String resource = "demo/demo1/configuration.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        /**
         * 默认返回的是 {@link org.apache.ibatis.session.defaults.DefaultSqlSessionFactory} 工厂
         */
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,"db1");

        SqlSession sqlSession = sqlSessionFactory.openSession();

        Demo1UserMapper userMapper = sqlSession.getMapper(Demo1UserMapper.class);

        /**
         * 调用的是 {@link org.apache.ibatis.binding.MapperProxy}
         */
        List<Demo1User> users = userMapper.selectUser();

        System.out.println(JSON.toJSONString(users));

        sqlSession.close();
    }


}