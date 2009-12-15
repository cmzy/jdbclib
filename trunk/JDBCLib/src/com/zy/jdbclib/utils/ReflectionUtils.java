
package com.zy.jdbclib.utils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zy.jdbclib.core.JDBCException;

/**
 * 反射的Utils函数集合. 提供访问私有变量,获取泛型类型Class,提取集合中元素的属性等Utils函数.
 * 
 * @version 1.0
 * @since 1.0
 */
public class ReflectionUtils {

    private static Log log = LogFactory.getLog(ReflectionUtils.class);

    private ReflectionUtils() {
    }

    /**
     * 直接读取对象属性值,无视private/protected修饰符,不经过getter函数.
     */
    public static Object getFieldValue(final Object object, final String fieldName) {
        Field field = getDeclaredField(object, fieldName);

        if (field == null)
            throw new IllegalArgumentException("Could not find field [" + fieldName
                    + "] on target [" + object + "]");

        makeAccessible(field);

        Object result = null;
        try {
            result = field.get(object);
        } catch (IllegalAccessException e) {
            log.error("不可能抛出的异常{}" + e.getMessage());
        }
        return result;
    }

    /**
     * 直接设置对象属性值,无视private/protected修饰符,不经过setter函数.
     */
    public static void setFieldValue(final Object object, final String fieldName, final Object value) {
        Field field = getDeclaredField(object, fieldName);

        if (field == null)
            throw new IllegalArgumentException("Could not find field [" + fieldName
                    + "] on target [" + object + "]");

        makeAccessible(field);

        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            log.error("不可能抛出的异常:{}" + e.getMessage());
        }
    }

    /**
     * 循环向上转型,获取对象的DeclaredField.
     */
    protected static Field getDeclaredField(final Object object, final String fieldName) {
        if (object == null) {
            throw new IllegalArgumentException("object参数不能为空");
        }
        return getDeclaredField(object.getClass(), fieldName);
    }

    /**
     * 循环向上转型,获取类的DeclaredField.
     */
    @SuppressWarnings("unchecked")
    protected static Field getDeclaredField(final Class clazz, final String fieldName) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (fieldName == null || fieldName.length() <= 0) {
            throw new IllegalArgumentException("fieldName  must not be null");
        }
        for (Class superClass = clazz; superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            try {
                return superClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field不在当前类定义,继续向上转型
            }
        }
        return null;
    }

    /**
     * 强制转换fileld可访问.
     */
    protected static void makeAccessible(final Field field) {
        if (!Modifier.isPublic(field.getModifiers())
                || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
            field.setAccessible(true);
        }
    }

    /**
     * 通过反射,获得定义Class时声明的父类的泛型参数的类型. 如public UserDao extends HibernateDao<User>
     * 
     * @param clazz The class to introspect
     * @return the first generic declaration, or Object.class if cannot be
     *         determined
     */
    @SuppressWarnings("unchecked")
    public static Class getSuperClassGenricType(final Class clazz) {
        Type genType = clazz.getGenericSuperclass();
        return getClassGenricType(genType, 0);
    }

    /**
     * 通过反射,获得定义Class时声明的泛型参数的类型.
     * 
     * @param clazz clazz The class to introspect
     * @param index the Index of the generic ddeclaration,start from 0.
     * @return the index generic declaration, or Object.class if cannot be
     *         determined
     */

    @SuppressWarnings("unchecked")
    public static Class getClassGenricType(final Type genType, final int index) {
        Class clazz = genType.getClass();
        if (!(genType instanceof ParameterizedType)) {
            log.warn(clazz.getSimpleName() + " not ParameterizedType");
            return Object.class;
        }

        Type[] params = ((ParameterizedType)genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            log.warn("Index: " + index + ", Size of " + clazz.getSimpleName()
                    + "'s Parameterized Type: " + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            log.warn(clazz.getSimpleName()
                    + " not set the actual class on superclass generic parameter");
            return Object.class;
        }
        return (Class)params[index];
    }

    @SuppressWarnings("unchecked")
    public static PropertyDescriptor[] getPropertyDescriptors(Class mappedClass) {
        try {
            return Introspector.getBeanInfo(mappedClass).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new JDBCException("Cannot get bean property descriptors for object of class ["
                    + mappedClass + "]");
        }
    }

    public static <T extends Object> T instantiateClass(Class<T> clazz){
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (clazz.isInterface()) {
            throw new JDBCException("Specified class is an interface");
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new JDBCException(e);
        } catch (IllegalAccessException e) {
            throw new JDBCException(e);
        }
    }

}
