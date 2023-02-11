#!/usr/env python3
from string import ascii_lowercase as alc
from functools import reduce
import operator
from jinja2 import Template

lox_content = Template('''
fun get_adder() {
    var this_val = 0;
    // huge amount of var definitions
    {{ var_def }}
    // end of var definition
    fun adder() {
        this_val = this_val + 1;
        return this_val;
    }
    return adder;
}

var cnt = {{ cnt }};
var adder = get_adder();
for (var i = 0; i < cnt; i = i + 1) {
    var val = 0;
    for (var j = 0; j < cnt; j = j + 1) {
        val = adder();
    }
    // print val;
}
''')


def main():
    fname = "lookup.lox"
    cnt = 2000
    var_def = reduce(operator.add,
                     (f"    var {x}{y}{z}{w} = \"{x}{y}{z}{w}\";\n"
                      for x in alc for y in alc for z in alc
                      for w in range(2)))

    with open(fname, "w") as f:
        f.write(lox_content.render(var_def=var_def, cnt=cnt))


if __name__ == "__main__":
    main()
