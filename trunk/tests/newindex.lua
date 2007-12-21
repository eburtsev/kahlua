t1 = {}
t2 = {}
t3 = {}
t4 = {}
setmetatable(t1, {__newindex = t2})
setmetatable(t2, {__newindex = function(t,k,v) last_set = v end})
setmetatable(t3, {__newindex = t4})

assert(last_set == nil)
t1.key = "x"
assert(last_set == "x")
t2.key = "y"
assert(last_set == "y")
t1.key = "x"
assert(last_set == "x")

assert(t4.key == nil)
t3.key = 123
assert(t4.key == 123)
assert(t3.key == nil)
