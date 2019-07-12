package liu.york.demo.demo2;

import com.alibaba.fastjson.JSON;
import liu.york.demo.demo1.Demo1UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;

public class Demo2Run {

    public static void main(String[] args) throws Exception {
        String resource = "demo/demo2/configuration.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,"db1");

        SqlSession sqlSession = sqlSessionFactory.openSession();
        Demo2UserMapper userMapper = sqlSession.getMapper(Demo2UserMapper.class);

        System.out.println(JSON.toJSONString(userMapper.selectUser(1)));
        System.out.println(JSON.toJSONString(userMapper.selectUser(1)));

        sqlSession.close();
    }

}