/*
Copyright (c) 2007-2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.CoroutineLib;
import se.krka.kahlua.stdlib.MathLib;
import se.krka.kahlua.stdlib.OsLib;
import se.krka.kahlua.stdlib.StringLib;
import se.krka.kahlua.test.UserdataArray;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;

public class Test {
	private static HashSet excludedFiles = new HashSet();
	private static LuaState getState(File dir) throws FileNotFoundException, IOException {

		LuaState state = new LuaState(System.out);
		
		BaseLib.register(state);
		MathLib.register(state);
		StringLib.register(state);
		UserdataArray.register(state);
		CoroutineLib.register(state);
		OsLib.register(state);

		state = runLua(dir, state, new File(dir, "stdlib.lbc"));
		state = runLua(dir, state, new File(dir, "testhelper.lbc"));
		return state;
	}

	private static LuaState runLua(File dir, LuaState state, File file)
			throws IOException, FileNotFoundException {
		excludedFiles.add(file);
		File stdlib = file;
		LuaClosure closure = LuaPrototype.loadByteCode(new FileInputStream(stdlib), state.environment);
		Object[] results = state.pcall(closure);
		if (results[0] != Boolean.TRUE) {
			System.out.println("Stdlib failed: " + results[1]);
			((Throwable) (results[3])).printStackTrace();
			System.out.println(results[2]);
			state = null;
		}
		return state;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File dir = new File(args[0]);

		LuaState state = getState(dir);
		
		Object runTest = state.environment.rawget("runtest");
		Object generateReportClosure = state.environment.rawget("generatereport");

		File[] children = null;
		for (int i = 1; i < args.length; i++) {
			if (args[i].length() > 0) {
				File f = new File(dir, args[i]);
				if (f.exists() && f.isFile()) {
					if (children == null) {
						children = new File[args.length];
					}
					children[i] =  f;
				} else {
					System.err.println(f + " is not a valid file.");
					System.exit(1);
				}
			}
		}
		if (children == null) {
			children = dir.listFiles();
		}

		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			if (child != null && !excludedFiles.contains(child) && child.getName().endsWith(".lbc")) {
				LuaClosure closure = LuaPrototype.loadByteCode(new FileInputStream(child), state.environment);
				System.out.println("Running " + child + "...");
				Object[] results = state.pcall(runTest, new Object[] {child.getName(), closure});
				if (results[0] != Boolean.TRUE) {
					System.out.println("Crash at " + child + ": " +  results[1]);
					((Throwable) (results[3])).printStackTrace();
					System.out.println(results[2]);
				}
			}
		}
		Object[] results = state.pcall(generateReportClosure);
		if (results[0] == Boolean.TRUE) {
			FileWriter writer = new FileWriter("testsuite/testreport.html");
			writer.append((String) results[1]);
			writer.close();
			Long suitesTotal = new Long(((Double) results[2]).longValue());
			Long suitesSuccess = new Long(((Double) results[3]).longValue());
			Long testsTotal = new Long(((Double) results[4]).longValue());
			Long testsSuccess = new Long(((Double) results[5]).longValue());
			System.out.println(String.format("%d of %d suites ok. (%d of %d tests)", new Object[] { suitesTotal, suitesSuccess, testsTotal, testsSuccess}));
			System.out.println("Detailed test results can be read at testsuite/testreport.html");
		} else {
			System.out.println("Could not generate reports: " +  results[1]);
			((Throwable) (results[3])).printStackTrace();
			System.out.println(results[2]);
		}
	}
}
