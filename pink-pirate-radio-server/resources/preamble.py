import time
import math
import random

variables = {}

def set_var(k,v):
    variables[k] = v

def get_var(k):
    if k in variables:
        return variables[k]
    else:
        return None

constants = {
    'pi': math.pi,
    'e': math.e,
    'golden_ratio': (1 + math.sqrt(5)) / 2,
    'sqrt2': math.sqrt(2),
    'sqrt1_2': math.sqrt(0.5),
    'infinity': math.inf
}

def get_constant(name):
    return constants[name]

def add(x,y):
    return x + y

def minus(x,y):
    return x - y

def multiply(x,y):
    return x * y

def divide(x,y):
    return x / y

def eq(x,y):
    return x == y

def neq(x,y):
    return x != y

def lt(x,y):
    return x < y

def lte(x,y):
    return x <= y

def gt(x,y):
    return x > y

def gte(x,y):
    return x >= y

def root(x):
    return math.sqrt(x)

# abs is builtin

def neg(x):
    return -1 * x

def ln(x):
    return math.log(x)

def log10(x):
    return math.log10(x)

def exp(x):
    return math.exp(x)

def power(x,y):
    return math.pow(x,y)

def pow10(x):
    return math.pow(x,10)

def sin(x):
    return math.sin(x)

def cos(x):
    return math.cos(x)

def tan(x):
    return math.tan(x)

def asin(x):
    return math.asin(x)

def acos(x):
    return math.acos(x)

def atan(x):
    return math.atan(x)

def mod(x,y):
    return x % y

def even(x):
    return x % 2 == 0

def odd(x):
    return x % 2 != 0

def prime(x):
    # TODO
    return False

def whole(x):
    return float(x).is_integer()

def positive(x):
    return x > 0

def negative(x):
    return x < 0

def divisible_by(x,y):
    return x % y == 0

def constrain(n, low, high):
    return min(high, max(low, n))

def rand_int(low, high):
    return random.randint(low, high)

def rand_float():
    return random.uniform(0,1)

# round is built-in

def roundup(x):
    return math.ceil(x)

def rounddown(x):
    return math.floor(x)

def logic_negate(x):
    return not x

def logic_and(x,y):
    return x and y

def logic_or(x,y):
    return x or y

def ternary(x,y,z):
    return y if x else z

def color(r,g,b):
    return (r,g,b)

def rand_color():
    r = random.randint(0,255)
    g = random.randint(0,255)
    b = random.randint(0,255)
    return (r,g,b) 

def color_blend(color1, color2, ratio):
    r1, g1, b1 =  color1
    r2, g2, b2 = color2 
    ratio = min(1, max(0, ratio))
    r = round((r1 * (1 - ratio)) + (r2 * ratio))
    g = round((g1 * (1 - ratio)) + (g2 * ratio))
    b = round((b1 * (1 - ratio)) + (b2 * ratio))
    return (r,g,b)

def wait(s):
    time.sleep(s)

def play_sound_clip(clip_id):
    print("play sound {0}".format(clip_id))

def set_display_color(color):
    r,g,b = color
    print("set display to ({0},{1},{2})".format(r,g,b))

def media_player_action(action):
    print("do medial player action {0}".format(action))

