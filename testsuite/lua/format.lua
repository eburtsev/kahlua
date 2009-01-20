function testformat(expected, template, ...)
	local t = {...}
	local inputs = ""
	for i = 1, #t do
		inputs = string.format("%s, %q", inputs, t[i])
	end
	
	local output = string.format(template, ...)
	local msg = string.format("string.format(%q%s) == %q, expected %q",
		template, inputs, output, expected)
	assert(output == expected, msg)
end

testformat("     hello", "%10s", "hello")
testformat("     hello", "%010s", "hello") -- zero padding only for numbers!
testformat("hello", "%1s", "hello")
testformat("00012", "%05d", 12)
testformat("12   ", "%-05d", 12)
testformat("12   ", "%-5d", 12)
testformat("a    ", "%-5s", "a")
testformat("  +12", "%+5d", 12)
testformat("+12", "%+1d", 12)
testformat("12.1", "%1.1f", 12.125)
testformat("12.12", "%1.2f", 12.125)
testformat("12.125", "%1.3f", 12.125)
testformat("12.1250", "%1.4f", 12.125)
testformat(" 12.1250", "%8.4f", 12.125)
testformat("12.1250 ", "%-8.4f", 12.125)
testformat("+12.1250 ", "%-+9.4f", 12.125)
testformat("'                hell'", "'%20.4s'", "hello")

testformat("!12.!", "!%#.0f!", 12.3456)
testformat("!12!", "!%.0f!", 12.3456)
testformat("!12.3!", "!%.1f!", 12.3456)
testformat("!12.35!", "!%.2f!", 12.3456)
testformat("!12.346!", "!%.3f!", 12.3456)
testformat("!12.3456!", "!%.4f!", 12.3456)
testformat("!12.34560!", "!%.5f!", 12.3456)
testformat("!12.345600!", "!%.6f!", 12.3456)


testformat("-0.1", "%+1.1f", -0.1)

testformat("+0.1", "%+1.1f", 0.1)
testformat(" 0.1", "% 1.1f", 0.1)

testformat("-123", "% 1.0f", -123)
testformat(" 123", "% 1.0f", 123)
testformat("+000012.23", "%+010.2f", 12.23)
testformat("0.0000001000", "%.10f", 0.0000001)
testformat("0.0000001235", "%.10f", 0.00000012347)

testformat("1.e+00", "%#.e", 1.0)
testformat("1.235e+00", "%#.3e", 1.23456)
testformat("1.235E+03", "%#.3E", 1234.56)
testformat("+00000000001.235E+17", "%+020.3E", 123456789123456789)
testformat("+00000000001.235E-14", "%+020.3E", 0.0000000000000123456789123456789)

testformat("0.0001", "%g", 0.0001)
testformat("0.000100000", "%#g", 0.0001)
testformat("-0.000100000", "%#g", -0.0001)
testformat("1e-05", "%g", 0.00001)
testformat("1.00000e-05", "%#g", 0.00001)
testformat("-1.00000e-05", "%#g", -0.00001)

testformat("1.", "%#.0g", 1.2345678)
testformat("1.", "%#.1g", 1.2345678)
testformat("1.2", "%#.2g", 1.2345678)
testformat("1.23", "%#.3g", 1.2345678)
testformat("1.", "%#.0g", 1)
testformat("1.", "%#.1g", 1)
testformat("1.0", "%#.2g", 1)
testformat("1.00", "%#.3g", 1)
testformat("100.", "%#.3g", 100)
testformat("100.0", "%#.4g", 100)
testformat("100.00", "%#.5g", 100)
testformat("100", "%.4g", 100)
testformat("1e+07", "%.4g", 10000000)
testformat("1.000e+07", "%#.4g", 10000000)

testformat("1.23e+06", "%g", 1230000)
testformat("1.23000e+06", "%#g", 1230000)


-- %, s, q, c, d, E, e, f, g, G, i, o, u, X, and x
testcases = {
	["%"]={"%"},
	["c"]={[255]=string.char(255), [120]='x'},
	["d"]={[-1]='-1', [1]='1', ["1"]='1', [1.1]='1', [10]='10', [255]='255'},
	["e"]={[-1]="-1.000000e+00", [1]="1.000000e+00", ["1"]="1.000000e+00", [1.1]="1.100000e+00", 
				[10]="1.000000e+01", [255]="2.550000e+02"},
	["E"]={[-1]="-1.000000E+00", [1]="1.000000E+00", ["1"]="1.000000E+00", [1.1]="1.100000E+00", 
				[10]="1.000000E+01", [255]="2.550000E+02"},
	["f"]={[-1]="-1.000000", [1]="1.000000", ["1"]="1.000000", [1.1]="1.100000", 
				[10]="10.000000", [255]="255.000000"},
	["g"]={[-1]='-1', [1]='1', ["1"]='1', [1.1]='1.1', [10]='10', [255]='255'},
	["G"]={[-1]='-1', [1]='1', ["1"]='1', [1.1]='1.1', [10]='10', [255]='255'},
	["i"]={[-1]='-1', [1]='1', ["1"]='1', [1.1]='1', [10]='10', [255]='255'},
	["o"]={[1]='1', ["1"]='1', [1.1]='1', [10]='12', [255]="377"},
	["q"]={["\n"]='"\\\n"', ["\r"]='"\\r"', ["\""]='"\\""', ["\t"]='"\t"'},
	["s"]={[""]="", ["abc"]="abc", [1]="1"},
	["u"]={[1]='1', ["1"]='1', [1.1]='1', [10]='10', [255]='255'},
	["x"]={[1]='1', ["1"]='1', [1.1]='1', [10]='a', [255]="ff"},
	["X"]={[1]='1', ["1"]='1', [1.1]='1', [10]='A', [255]='FF'},
}

for t, c in pairs(testcases) do
	local template = "%" .. t
	for k, v in pairs(c) do
		testformat(v, template, k)
		--print(string.format("string.format(%q, %q) == %q", template, k, result))
	end
end
