package liu.york.pointClass;

/**
 * 懒加载也就是查询一个对象，这个对象中包含另一个对象，
 * 比如查询学生，每个学生类中都有一个老师对象
 *     <resultMap id="studentMap" type="liu.york.Student">
 *          <id column="id" property="id" />
 *              <result column="content" property="content" />
 *          <association property="teacher" column="TEACHER" select="selectTeacher" fetchType="lazy"/>
 *     </resultMap>
 */
public class LazyLoadS {
}