package se.krka.kahluax.annotation;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool to automatically expose java classes and
 * methods to a lua state
 * NOTE: This tool requires annotations (java 1.5 or higher) to work
 * and is therefore not supported in J2ME.
 */
public class LuaJavaClassFactory {
	private LuaState state;
	private Map<String, ClassHolder> classMap;

	public LuaJavaClassFactory(LuaState state) {
		this.state = state;
		classMap = new HashMap<String, ClassHolder>();
	}

	private String getName(Class clazz) {
		String name = clazz.getName();
		if (clazz.isAnnotationPresent(LuaClass.class)) {
			LuaClass luaClass = (LuaClass) clazz.getAnnotation(LuaClass.class);
			name = luaClass.alias();
		}
		return name;
	}

	public void exposeClass(Class clazz, boolean exposeConstructor) {
		String name = getName(clazz);

		//System.out.println("Registering class: " + name);
		ClassHolder classHolder = new ClassHolder();
		classHolder.exposeConstructor = exposeConstructor;
		classHolder.clazz = clazz;
		classHolder.name = name;
		classHolder.reflectMethods();

		state.setUserdataMetatable(clazz, classHolder.mt);

		classMap.put(name, classHolder);
	}

	public void overideMethod(Class clazz, String methodName, JavaFunction javaFunction) {
		String name = getName(clazz);
		ClassHolder ch = classMap.get(name);
		ch.it.rawset(methodName, javaFunction);
	}


	public void registerFactory() {
		state.environment.rawset("createClassInstance", new JavaFunction() {

			public int call(LuaCallFrame luaCallFrame, int i) {
				BaseLib.luaAssert(i > 1, "Not enugh params");
				String name = (String) luaCallFrame.get(0);

				if (!classMap.containsKey(name)) {
					throw new RuntimeException("Unknown class: " + name);
				}

				ClassHolder ch = classMap.get(name);
				if (!ch.exposeConstructor) {
					throw new RuntimeException("Cant create new instance of that class");
				}

				if (i > 1) {

					Object params[] = new Object[i - 1];
					for (int paramIndex = 1; paramIndex < i; paramIndex++) {
						params[paramIndex - 1] = luaCallFrame.get(paramIndex);
					}

					try {
						Object o = createInstance(name, params);
						luaCallFrame.push(o);
						return 1;
					} catch (Exception e) {
						throw new RuntimeException("Failed to create instance: " + e.getMessage());
					}

				} else {
					try {
						Object o = createInstance(name);
						luaCallFrame.push(o);
						return 1;
					} catch (Exception e) {
						throw new RuntimeException("Failed to create instance: " + e.getMessage());
					}
				}

			}
		});
	}

	private Object createInstance(String name, Object[] params) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		if (classMap.containsKey(name)) {
			Class clazz = classMap.get(name).clazz;

			Constructor constructor = null;
			for (Constructor c : clazz.getConstructors()) {
				if (c.isAnnotationPresent(LuaConstructor.class)) {
					constructor = c;
					break;
				}
			}

			if (constructor == null)
				throw new InstantiationException("Cant create class, unknown constructor");


			if (constructor.getParameterTypes().length != params.length)
				throw new InstantiationException("Cant create class, wrong number of params");

			Class[] paramTypes = constructor.getParameterTypes();
			Object[] newParams = castParams(params, paramTypes);

			return constructor.newInstance(newParams);

		}
		throw new InstantiationException("Cant create unknown class: " + name);
	}

	private Object[] castParams(Object[] params, Class[] paramTypes) {
		Object[] newParams = new Object[paramTypes.length];

		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i].equals(Long.class) || paramTypes[i].equals(long.class)) {
				newParams[i] = Long.valueOf(Double.valueOf((Double) params[i]).longValue());

			} else if (paramTypes[i].equals(Integer.class) || paramTypes[i].equals(int.class)) {
				newParams[i] = Integer.valueOf(Double.valueOf((Double) params[i]).intValue());

			} else if (paramTypes[i].equals(Float.class) || paramTypes[i].equals(float.class)) {
				newParams[i] = Float.valueOf(Double.valueOf((Double) params[i]).floatValue());

			} else {
				newParams[i] = params[i];
			}
		}
		return newParams;
	}

	private Object createInstance(String name) throws IllegalAccessException, InstantiationException {
		if (classMap.containsKey(name)) {
			Class clazz = classMap.get(name).clazz;
			return clazz.newInstance();
		}
		throw new InstantiationException("Cant create unknown class: " + name);
	}


	private class ClassHolder {
		String name;
		boolean exposeConstructor;
		Class clazz;
		LuaTable mt;
		LuaTable it;

		private ClassHolder() {
			mt = new LuaTable();
			it = new LuaTable();

			mt.rawset("__index", it);

		}

		public void reflectMethods() {
			Method m[] = clazz.getMethods();
			//System.out.println(m.length);
			for (Method method : clazz.getMethods()) {

				if (method.isAnnotationPresent(LuaMethod.class)) {
					LuaMethod luaMethod = method.getAnnotation(LuaMethod.class);

					String methodName = method.getName();
					if (!luaMethod.alias().equals("[unassigned]")) {
						methodName = luaMethod.alias();
					}

					registerLuaMethod(methodName, method);

				}

			}

		}

		private void registerLuaMethod(String methodName, final Method method) {

			//System.out.println("Registering method: " + methodName);

			it.rawset(methodName, new JavaFunction() {
				public int call(LuaCallFrame luaCallFrame, int i) {
					int nrOfParams = method.getParameterTypes().length;
					BaseLib.luaAssert(i == nrOfParams + 1, "Wrong number of params!");
					Object owner = luaCallFrame.get(0);
					Object returnObject = null;
					if (nrOfParams != 0) {
						Object params[] = new Object[nrOfParams];
						for (int paramIndex = 0; paramIndex < nrOfParams; paramIndex++) {
							params[paramIndex] = luaCallFrame.get(paramIndex + 1);
						}
						try {

							Class[] paramTypes = method.getParameterTypes();
							Object[] newParams = castParams(params, paramTypes);

							returnObject = method.invoke(owner, newParams);

						} catch (IllegalAccessException e) {
							throw new RuntimeException("Illegal access to method, " + e);
						} catch (InvocationTargetException e) {
							throw new RuntimeException("Illegal invocation of method, " + e);
						}
					} else {

						try {
							returnObject = method.invoke(owner);

						} catch (IllegalAccessException e) {
							throw new RuntimeException("Illegal access to method, " + e);
						} catch (InvocationTargetException e) {
							throw new RuntimeException("Illegal invocation of method, " + e);
						}

					}
					if (returnObject != null) {
						if (returnObject instanceof Boolean) {
							luaCallFrame.push(LuaState.toBoolean((Boolean) returnObject));
						} else if (returnObject instanceof String) {
							luaCallFrame.push(((String) returnObject).intern());
						} else if (returnObject instanceof List) {
							List l = (List) returnObject;
							LuaTable t = new LuaTable();
							for (int index = 0; index < l.size(); index++) {
								t.rawset((double) index + 1, l.get(index));
							}
							luaCallFrame.push(t);


						} else {
							luaCallFrame.push(returnObject);
						}
						return 1;
					}
					return 0;
				}
			});
		}


	}

}
