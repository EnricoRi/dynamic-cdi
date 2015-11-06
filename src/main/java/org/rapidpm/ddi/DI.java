/*
 * Copyright [2014] [www.rapidpm.org / Sven Ruppert (sven.ruppert@rapidpm.org)]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.rapidpm.ddi;


import org.rapidpm.ddi.bootstrap.ClassResolverCheck001;
import org.rapidpm.ddi.implresolver.ImplementingClassResolver;
import org.rapidpm.ddi.producer.InstanceCreator;
import org.rapidpm.ddi.reflections.ReflectionsModel;
import org.rapidpm.proxybuilder.VirtualProxyBuilder;
import org.rapidpm.proxybuilder.type.virtual.CreationStrategy;
import org.rapidpm.proxybuilder.type.virtual.ProxyGenerator;
import org.rapidpm.proxybuilder.type.virtual.ProxyType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;


/**
 * Created by Sven Ruppert on 05.12.2014.
 */
public class DI {

  private static ImplementingClassResolver implementingClassResolver = new ImplementingClassResolver();
  private static ReflectionsModel reflectionsModel = new ReflectionsModel();
  private static boolean bootstrapedNeeded = true;

  private DI() {
  }

  public static void checkActiveModel() {
    //hole alle Felder die mit einem @Inject versehen sind.
    //pruefe ob es sich um ein Interface handelt
    //pruefe ob es nur einen Producer / eine Implementierung  dazu gibt
    // -- liste Multiplizitäten
    new ClassResolverCheck001().execute();
  }

  public static synchronized void bootstrap() {
//    reflectionsModel = new ReflectionsModel();
    implementingClassResolver.clearCache();
    if (bootstrapedNeeded) {
      reflectionsModel.rescann("");
    }
    bootstrapedNeeded = false;
  }

  public static synchronized void clearReflectionModel() {
    reflectionsModel = new ReflectionsModel();
    implementingClassResolver.clearCache();
    bootstrapedNeeded = true;
  }

  public static synchronized void activatePackages(String pkg) {
    reflectionsModel.rescann(pkg);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }

  public static synchronized void activatePackages(String pkg, URL... urls) {
    reflectionsModel.rescann(pkg, urls);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }

  public static synchronized void activatePackages(String pkg, Collection<URL> urls) {
    reflectionsModel.rescann(pkg, urls);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }

  public static synchronized void activatePackages(boolean parallelExecutors, String pkg) {
    reflectionsModel.setParallelExecutors(parallelExecutors);
    reflectionsModel.rescann(pkg);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }

  public static synchronized void activatePackages(boolean parallelExecutors, String pkg, URL... urls) {
    reflectionsModel.setParallelExecutors(parallelExecutors);
    reflectionsModel.rescann(pkg, urls);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }

  public static synchronized void activatePackages(boolean parallelExecutors, String pkg, Collection<URL> urls) {
    reflectionsModel.setParallelExecutors(parallelExecutors);
    reflectionsModel.rescann(pkg, urls);
    implementingClassResolver.clearCache();
    bootstrapedNeeded = false;
  }


  public static synchronized <T> T activateDI(T instance) {
    if (bootstrapedNeeded) bootstrap();
    injectAttributes(instance);
    initialize(instance);
    //register at new Scope ?
    return instance;
  }

  private static <T> void injectAttributes(final T rootInstance) throws SecurityException {
    injectAttributesForClass(rootInstance.getClass(), rootInstance);
  }


  private static <T> void injectAttributesForClass(Class targetClass, T rootInstance) {
    Class<?> superclass = targetClass.getSuperclass();
    if (superclass != null) {
      injectAttributesForClass(superclass, rootInstance);
    }

    Field[] fields = targetClass.getDeclaredFields();
    for (final Field field : fields) {
      if (field.isAnnotationPresent(Inject.class)) {
        Class type = field.getType();
        final Class realClass = implementingClassResolver.resolve(type);
        Object value; //Attribute Type for inject
        if (field.isAnnotationPresent(Proxy.class)) {
          final Proxy annotation = field.getAnnotation(Proxy.class);

          final boolean virtual = annotation.virtual();
          final CreationStrategy creationStrategy = annotation.concurrent();
          final boolean metrics = annotation.metrics();
          final boolean secure = annotation.secure(); //woher die Sec Rules?
          final boolean logging = annotation.logging();

          final Proxy.ProxyType proxyType = annotation.proxyType();

          if (virtual) {
            //interface , realclass

            value = ProxyGenerator.newBuilder()
                .withSubject(type)
                .withType(ProxyType.DYNAMIC)
//                .withRealClass(realClass)
//                .withCreationStrategy(Concurrency.NONE)
                .withServiceFactory(new DDIServiceFactory<>(realClass))
                .withCreationStrategy(creationStrategy)
//                .withServiceStrategyFactory(new ServiceStrategyFactoryNotThreadSafe<>())
                .build()
                .make();
          } else {
            value = new InstanceCreator().instantiate(realClass);
            activateDI(value); //rekursiver abstieg
          }
          if (metrics || secure || logging) {
            final VirtualProxyBuilder virtualProxyBuilder = VirtualProxyBuilder.createBuilder(type, value);
            if (metrics) {
              virtualProxyBuilder.addMetrics();
            }
            if (secure) {
//              virtualProxyBuilder.addSecurityRule(()->{});
            }
            if (logging) {
              //virtualProxyBuilder.addLogging();
            }
            value = virtualProxyBuilder.build();
          }
        } else {
          value = new InstanceCreator().instantiate(realClass);
          activateDI(value); //rekursiver abstieg
        }
        //check Scope ....
//        Object value = scopes.getProperty(clazz, key);
//        if (!type.isPrimitive()) {
//          value = instantiate(type);
//        }

        if (value != null) {
          injectIntoField(field, rootInstance, value);
        }
      }
    }
  }


  private static void injectIntoField(final Field field, final Object instance, final Object target) {
    AccessController.doPrivileged((PrivilegedAction) () -> {
      boolean wasAccessible = field.isAccessible();
      field.setAccessible(true);
      try {
        field.set(instance, target);
        return null; // return nothing...
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        throw new IllegalStateException("Cannot set field: " + field, ex);
      } finally {
        field.setAccessible(wasAccessible);
      }
    });
  }

  private static void initialize(Object instance) {
    Class<?> clazz = instance.getClass();
    invokeMethodWithAnnotation(clazz, instance, PostConstruct.class);
  }

//  private boolean isNotPrimitive(Class<?> type) {
//    return !type.isPrimitive();
//  }


  private static void invokeMethodWithAnnotation(Class clazz, final Object instance,
                                                 final Class<? extends Annotation> annotationClass)
      throws IllegalStateException, SecurityException {

    Method[] declaredMethods = clazz.getDeclaredMethods();
    for (final Method method : declaredMethods) {
      if (method.isAnnotationPresent(annotationClass)) {
        AccessController.doPrivileged((PrivilegedAction) () -> {
          boolean wasAccessible = method.isAccessible();
          try {
            method.setAccessible(true);
            return method.invoke(instance); //TODO Dynamic ObjectAdapter ?
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new IllegalStateException("Problem invoking " + annotationClass + " : " + method, ex);
          } finally {
            method.setAccessible(wasAccessible);
          }
        });
      }
    }
    Class superclass = clazz.getSuperclass();
    if (superclass != null) {
      invokeMethodWithAnnotation(superclass, instance, annotationClass);
    }
  }


  //delegator


  public static <T> Class<? extends T> resolveImplementingClass(final Class<T> interf) {
    return (Class<? extends T>) implementingClassResolver.resolve(interf);
  }

  public static boolean isPkgPrefixActivated(final String pkgPrefix) {
    return reflectionsModel.isPkgPrefixActivated(pkgPrefix);
  }

  public static LocalDateTime getPkgPrefixActivatedTimestamp(final String pkgPrefix) {
    return reflectionsModel.getPkgPrefixActivatedTimestamp(pkgPrefix);
  }

  public static <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
    return reflectionsModel.getSubTypesOf(type);
  }

  public static Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
    return reflectionsModel.getTypesAnnotatedWith(annotation);
  }

  public static Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, final boolean honorInherited) {
    return reflectionsModel.getTypesAnnotatedWith(annotation, honorInherited);
  }

  public static Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
    return reflectionsModel.getTypesAnnotatedWith(annotation);
  }

  public static Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, final boolean honorInherited) {
    return reflectionsModel.getTypesAnnotatedWith(annotation, honorInherited);
  }
}
