package liu.york.demo.demo2;

import liu.york.demo.demo1.Demo1User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface Demo2UserMapper {

    List<Demo1User> selectUser(@Param("id") Integer id);

}