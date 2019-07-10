package liu.york.demo.demo1;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface Demo1UserMapper {

    List<Demo1User> selectUser(@Param("id") Integer id);

//    @MapKey("username")
//    Map<String, Map<String, String>> selectUser(@Param("id") Integer id);

    Integer insertUser(@Param("id") Integer id,@Param("username")String username, @Param("password")String password);
}