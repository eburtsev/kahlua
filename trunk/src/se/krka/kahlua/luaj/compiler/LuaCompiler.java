package se.krka.kahlua.luaj.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.luaj.kahluafork.compiler.LexState;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class LuaCompiler implements JavaFunction {

	private final int index;
	
	private static final int LOADSTRING = 0;
	private static final int LOADSTREAM = 1;
	private static final String[] names = new String[] {
		"loadstring",
		"loadstream",
	};
	
	private static final LuaCompiler[] functions = new LuaCompiler[names.length];
	static {
		for (int i = 0; i < names.length; i++) {
			functions[i] = new LuaCompiler(i);
		}
	}

	private LuaCompiler(int index) {
		this.index = index;		
	}
	
	public static void register(LuaState state) {
		LuaTable env = state.getEnvironment();
		for (int i = 0; i < names.length; i++) {
			env.rawset(names[i], functions[i]);
		}
		/*
		LuaTable packageTable = (LuaTable) env.rawget("package");
		LuaTable loadersTable = (LuaTable) packageTable.rawget("loaders");
		*/
	}
	
	public int call(LuaCallFrame callFrame, int nArguments) {
		switch (index) {
		case LOADSTRING: return loadstring(callFrame, nArguments);
		case LOADSTREAM: return loadstream(callFrame, nArguments);
		}
		return 0;
	}
	
	private int loadstream(LuaCallFrame callFrame, int nArguments) {
		try {
			BaseLib.luaAssert(nArguments >= 2, "not enough arguments");
			InputStream is = (InputStream) callFrame.get(0);
			BaseLib.luaAssert(is != null, "No inputstream given");
			String name = (String) callFrame.get(1);
			return callFrame.push(loadis(is, name, callFrame.getEnvironment()));
		} catch (RuntimeException e) {
			return callFrame.push(null, e.getMessage());
		} catch (IOException e) {
			return callFrame.push(null, e.getMessage());
		}
	}

	private int loadstring(LuaCallFrame callFrame, int nArguments) {
		try {
			BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
			String source = (String) callFrame.get(0);
			BaseLib.luaAssert(source != null, "No source given");
			String name = (String) callFrame.get(1);
			if (name == null) {
				name = "<stdin>";
			}
			return callFrame.push(loadstring(source, name, callFrame.getEnvironment()));
		} catch (RuntimeException e) {
			return callFrame.push(null, e.getMessage());
		} catch (IOException e) {
			return callFrame.push(null, e.getMessage());
		}
	}

	public static LuaClosure loadis(InputStream inputStream, String name, LuaTable environment) throws IOException {
		BaseLib.luaAssert(name != null, "no name given the compilation unit");
		return new LuaClosure(LexState.compile(inputStream.read(), inputStream, name), environment);		
	}
	
	public static LuaClosure loadstring(String source, String name, LuaTable environment) throws IOException {
		return loadis(new ByteArrayInputStream(source.getBytes("UTF-8")), name, environment);
	}
}
