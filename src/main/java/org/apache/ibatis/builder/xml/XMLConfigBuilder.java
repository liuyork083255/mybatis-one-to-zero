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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

  private boolean parsed;
  private final XPathParser parser;
  private String environment;
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    /* 从根节点 <configuration></configuration>处开始解析 */
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  /**
   * 解析配置文件核心方法
   * 主要是在该方法中,主要完成的工作是读取配置文件的各个节点,然后将这些数据映射到内存配置对象 {@link Configuration} 中
   */
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      /*
       * 下面是 分别解析相应的节点标签
       * 通过测试，在xml配置文件中，确实只有下面11个标签
       */
      propertiesElement(root.evalNode("properties"));

      /*
       *  在<settings>标签中配置的节点信息必须在 Configuration 类中存在相应的属性，否则会抛出异常。
       *  然后根据标签中配置的值初始化 Configuration 类中的属性值
       */
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      loadCustomLogImpl(settings);
      settingsElement(settings);

      /*
       * 别名设置
       */
      typeAliasesElement(root.evalNode("typeAliases"));

      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));

      // read it after objectFactory and objectWrapperFactory issue #631
      /*
       *
       */
      environmentsElement(root.evalNode("environments"));

      databaseIdProviderElement(root.evalNode("databaseIdProvider"));

      /*
       * java和数据库字段映射器
       */
      typeHandlerElement(root.evalNode("typeHandlers"));

      /*
       * <mappers />标签用来进行 sql 文件映射
       */
      mapperElement(root.evalNode("mappers"));

    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   *
   *  <settings>
   *      开启二级缓存
   *      <setting name="cacheEnabled" value="true" />
   *      开启延迟加载
   *      <setting name="lazyLoadingEnabled" value="true" />
   *  </settings>
   */
  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    /* 读取所有子节点信息 */
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    /*
     * 检查所有setting配置文件的属性是否在 Configuration.class中存在set方法
     * 如果不存在，则抛出异常
     * 也就是类似 cacheEnabled 这种key键，在 Configuration.class 必然有这个属性，如果没有则表示不支持
     */
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      String[] clazzes = value.split(",");
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }

  /**
   * 类型别名是为 Java 类型设置一个短的名字。它只和 XML 配置有关，存在的意义仅在于用来减少类完全限定名的冗余
   *
   * 举例：
   *  <typeAliases>
   *      <typeAlias type="com.ys.po.User" alias="user"/>
   *      <package name="com.ys.po.User"/>
   *  </typeAliases>
   *
   *　不管是通过 package 标签配置，还是通过 typeAlias 标签配置的别名，在mapper.xml文件中使用的时候，转换成小写是相等的，那么就可以使用。
   *　如果不手动设置别名，默认是类名的小写。
   *　如果配置了注解别名，注解别名会覆盖上面的所有配置
   *
   * 除了上面手动配置的别名以外，mybatis 还为我们默认配置了一系列的别名
   * 在 {@link org.apache.ibatis.type.TypeAliasRegistry} 和 {@link Configuration#Configuration()}
   */
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {

        /*
         * 如果是 package 标签
         * 这段代码其实作用就是将配置的别名作为key（全部转成小写，如果有配置注解，以注解为准），别名代表的类作为 value 存入 HashMap 中
         * 递归+反射 解析这个包下的所有类，排除匿名类等
         */
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          /*
           * 如果是 typeAlias 标签
           * 则逻辑和解析 package 差不多，只不过是一个一个解析
           */
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            Class<?> clazz = Resources.classForName(type);
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        interceptorInstance.setProperties(properties);
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties properties = context.getChildrenAsProperties();
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      factory.setProperties(properties);
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
      configuration.setReflectorFactory(factory);
    }
  }

  /**
   *  举例说明：
   *    <properties resource="jdbc.properties">
   *      <property name="username" value="root"/>
   *    <property name="password" value="root"/>
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      /* 先加载 property 子节点下的属性 */
      Properties defaults = context.getChildrenAsProperties();
      /* 读取properties 节点中的属性resource和url */
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");

      /* properties 节点属性可以是 resource 或者 url ，但是不能同时存在 */
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      if (resource != null) {
        /* 读取引入文件的信息，resource引入的文件属性会覆盖子节点的配置 */
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        /* url引入的文件信息也会覆盖子节点的信息 */
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      /*
       * 读取Configuration对象中variables属性信息，如果有，则将其添加到properties对象中
       * 可以看到 mybatis 使用的属性可以是从配置文件中获取，也可以是编程方式引入，也就是在 build 的时候可以传入 Properties
       * 这种方式优先级最高
       */
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      /* 将Properties类设置到XPathParser和Configuration的variables属性中 */
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  /**
   * 给configuration类中的属性初始化
   */
  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * 可以配置多个运行环境，但是每个 SqlSessionFactory 实例只能选择一个运行环境常用： 一、development:开发模式 二、work：工作模式
   *     <environments default="development">
   *         属性必须和上面的default一样
   *         <environment id="development">
   *             使用JDBC的事务管理机制
   *             <transactionManager type="JDBC" />
   *             <dataSource type="POOLED">
   *                 <property name="driver" value="${jdbc.driver}" />
   *                 <property name="url" value="${jdbc.url}" />
   *                 <property name="username" value="${jdbc.username}" />
   *                 <property name="password" value="${jdbc.password}" />
   *             </dataSource>
   *         </environment>
   *     </environments>
   */
  private void environmentsElement(XNode context) throws Exception {
    /*
     * 如果<environments>标签不为null则开始执行
     * 可以发现如果为空并没有报错，这是为了和spring整合时，在spring容器中进行配置
     */
    if (context != null) {
      /* 如果在 build 的时候没有主动指定哪个环境，则默认采用 default */
      if (environment == null) {
        /* 获取<environments default="属性值">中的default属性值 */
        environment = context.getStringAttribute("default");
      }

      /* 遍历<environments/>标签中的子标签<environment/> */
      for (XNode child : context.getChildren()) {
        /* 获取<environment id="属性值">中的id属性值 */
        String id = child.getStringAttribute("id");

        /**
         * 遍历所有<environment>的时候依次判断相应的id是否是default设置的值
         * 也就是 id = {@link #environment} 的时候才会解析
         * 由此可见，一个 Configuration 中只有一个 environment 数据库环境配置
         */
        if (isSpecifiedEnvironment(id)) {
          /* 获取配置的事务管理器 */
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));

          /* 获取配置的数据源信息并创建对应的数据源工厂和完成属性赋值 */
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));

          /* 这个时候数据库还没有连接 */
          DataSource dataSource = dsFactory.getDataSource();

          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
        type = "DB_VENDOR";
      }
      Properties properties = context.getChildrenAsProperties();
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   * 解析 <dataSource type="POOLED" /> 标签
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      /* 获取数据源类型 JNDI POOLED UNPOOLED */
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      /**
       * 1 根据key = JNDI|POOLED|UNPOOLED 获取指定的java类
       * 2 获取到java 类后利用反射实例化
       * 这三种类型是在 {@link Configuration#Configuration()} 中被定义的
       */
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();

      /*
       * 上面一步骤实例化的 factory 并没有属性初始化
       * 这一步骤采用反射实例初始化
       */
      factory.setProperties(props);

      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   * Java数据类型和数据库数据类型是有区别的，而我们想通过Java代码来操作数据库或从数据库中取值的时候，必须要进行类型的转换。而  typeHandlers 便是来完成这一工作的
   * 如何使用可以参考官网：http://www.mybatis.org/mybatis-3/zh/configuration.html#typeHandlers
   */
  private void typeHandlerElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  /**
   * 告诉 MyBatis 到哪里去找到这些 mapper sql 语句
   * Java 在自动查找这方面没有提供一个很好的方法，所以最佳的方式是告诉 MyBatis 到哪里去找映射文件。
   * 可以使用相对于类路径的资源引用， 或完全限定资源定位符（包括 file:/// 的 URL），或类名和包名等
   *
   *  使用相对于类路径的资源引用
   *    <mappers>
   *      <mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
   *      <mapper resource="org/mybatis/builder/BlogMapper.xml"/>
   *      <mapper resource="org/mybatis/builder/PostMapper.xml"/>
   *    </mappers>
   *
   *  使用完全限定资源定位符（URL）
   *    <mappers>
   *      <mapper url="file:///var/mappers/AuthorMapper.xml"/>
   *      <mapper url="file:///var/mappers/BlogMapper.xml"/>
   *      <mapper url="file:///var/mappers/PostMapper.xml"/>
   *    </mappers>
   *
   *  使用映射器接口实现类的完全限定类名
   *    <mappers>
   *      <mapper class="org.mybatis.builder.AuthorMapper"/>
   *      <mapper class="org.mybatis.builder.BlogMapper"/>
   *      <mapper class="org.mybatis.builder.PostMapper"/>
   *    </mappers>
   *
   *  将包内的映射器接口实现全部注册为映射器
   *    <mappers>
   *      <package name="org.mybatis.builder"/>
   *    </mappers>
   *
   *
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          /*
           *  读取子标签属性分别为 resource、url、class的值
           */
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");

          /*
           * 这三个标签有且仅有一个有值，其余两个都为 null，才能正常执行
           */
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            /* 获取 resource 指向目录的字节流，也就是获取 XxxMapper.xml 字节流 */
            InputStream inputStream = Resources.getResourceAsStream(resource);
            /* 将 XxxMapper.xml 解析为 Document */
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
