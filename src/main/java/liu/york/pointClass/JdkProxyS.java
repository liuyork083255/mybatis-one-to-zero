package liu.york.pointClass;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
/**
 * JDK 动态代理
 *
 * {@link Proxy} 就是创建代理类，核心方法是 {@link Proxy#newProxyInstance} 里面调用的 {@link Proxy#getProxyClass0}
 *
 * {@link Proxy#getProxyClass0}：
 *      如果代理类被指定的类加载器loader定义了，并实现了给定的接口interfaces，那么就返回缓存的代理类对象，否则使用ProxyClassFactory创建代理类
 *
 */
public class JdkProxyS {
    public static void main(String[] args) {
        /* 创建用户业务逻辑类 */
        UserService target = new UserServiceImpl();

        /* 创建代理逻辑 handler，这个不是代理类，而是代理类需要做的具体操作 handler */
        UserServiceInvocationHandler handler = new UserServiceInvocationHandler(target);
        /* 创建代理类 */
        Object o = Proxy.newProxyInstance(JdkProxyS.class.getClassLoader(), target.getClass().getInterfaces(), handler);

        /* 类型转换，因为代理类也实现了相同的接口 */
        UserService userService = (UserService) o;

        userService.say();
        userService.print("LiuYork");
    }
}


/**
 * jdk 代理必须基于接口实现
 */
interface UserService {
    void say();

    void print(String name);
}

/**
 * 创建接口实现类，也就是用户的业务逻辑类
 */
class UserServiceImpl implements UserService {

    @Override
    public void say() {
        System.out.println("I am LiuYork");
    }

    @Override
    public void print(String name) {
        System.out.println(name);
    }
}

/**
 * 代理类，也就是对用户的业务逻辑类的代理
 */
class UserServiceInvocationHandler implements InvocationHandler {

    /**
     * 代理目标类，其实就是用户的业务逻辑类
     */
    private Object target;

    UserServiceInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     *
     * @param proxy     代理类，也就是jdk为业务逻辑类创建的代理类
     * @param method    当前执行的接口方法，比如代理类此时代理的方法是 {@link UserService#print(String)}
     * @param args      被代理接口方法的参数，比如 {@link UserService#print} 方法调用的传入参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("invocation before...");
        /* 如果用户的业务逻辑类有很多方法，那么这个地方会根据参数自动识别，调用哪一个方法 */
        Object value = method.invoke(target, args);
        System.out.println("invocation after ...");
        return value;
    }
}