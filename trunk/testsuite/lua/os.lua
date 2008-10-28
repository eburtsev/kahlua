local function assertEquals(a, b)
	assert(a == b, "Expected " .. a .. " == " .. b)
end

function tablesequal(t1, t2)
    for k1,v1 in pairs(t1) do
        if v1 ~= t2[k1] then
            return false
        end
    end
    return true
end

local tbl = "*t"
local str = "%c"

local christmas = {year = 2008, month = 12, day = 25,}
local newyearseve = {year = 2008, month = 12, day = 31,}
local oct25 = {year = 2008, month = 10, day = 25, hour = 2, min = 8, sec = 0, wday = 7, yday = 298,}
local nov1 = {year = 2008, month = 11, day = 1, hour = 2, min = 8, sec = 0, wday = 7, yday = 305,}

local halloween = 1225440000  -- seconds since the epoch for halloween 2008, 8am

assert(os.datediff(os.time(christmas), os.time(newyearseve)) > 0,1)
assert(os.datediff(os.time(newyearseve), os.time(christmas)) < 0,2)
assert(os.datediff(os.time(christmas), os.time(christmas)) == 0,3)

assert(os.datediff(os.time(christmas), os.time(newyearseve)) == 6*24*60*60,4)

local nowdate = os.date(tbl)
local nowtime = os.time(nowdate)

assert(os.time(os.date(tbl, nowtime)) == os.time(nowdate),5)
assert(tablesequal(os.date(tbl, os.time(oct25)), oct25), 6)

-- Locale dependant test, so comment it out
-- assertEquals(os.date(str,halloween), "Fri Oct 31 04:00:00 EDT 2008")
