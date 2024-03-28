package party.iroiro.luajava.luaj;

import org.luaj.vm2.*;
import party.iroiro.luajava.luaj.values.JavaObject;

import java.util.ArrayList;
import java.util.List;

import static party.iroiro.luajava.luaj.LuaJConsts.LUA_REGISTRYINDEX;

public class LuaJState {
    public static final int MAX_STACK_SLOTS = 2048;
    protected final int address;
    protected final int lid;
    protected final Globals globals;
    protected final LuaThread thread;
    protected final List<List<LuaValue>> luaStacks;
    protected final LuaTable registry;

    protected LuaTable jObjectMetatable = JavaMetatables.objectMetatable();
    protected LuaTable jClassMetatable = JavaMetatables.classMetatable();
    protected LuaTable jArrayMetatable = JavaMetatables.arrayMetatable();

    protected LuaJState(int address, int lid, Globals globals, LuaThread thread) {
        this.address = address;
        this.lid = lid;
        this.globals = globals;
        this.thread = thread;
        luaStacks = new ArrayList<>();
        luaStacks.add(new ArrayList<>());
        registry = LuaValue.tableOf();
    }

    protected List<LuaValue> stack() {
        assert !luaStacks.isEmpty();
        int size = luaStacks.size();
        return luaStacks.get(size - 1);
    }

    public int getTop() {
        return stack().size();
    }

    public void setTop(int top) {
        List<LuaValue> stack = stack();
        if (top == 0) {
            stack.clear();
        } else if (top <= stack.size()) {
            pop(stack.size() - top);
        } else {
            for (int i = 0; i < top - stack.size(); i++) {
                push(LuaValue.NIL);
            }
        }
    }

    public LuaValue getRegistry(String name) {
        return registry.get(name);
    }

    public void setRegistry(String name, LuaValue value) {
        registry.set(name, value);
    }

    public void insert(int i, LuaValue value) {
        if (i == LUA_REGISTRYINDEX) {
            return;
        }
        int index = toAbsoluteIndex(i) - 1;
        List<LuaValue> stack = stack();
        if (index < 0 || index > stack.size()) {
            return;
        }
        if (index == stack.size()) {
            stack.add(value);
        } else {
            stack.add(index, value);
        }
    }

    public void push(LuaValue value) {
        List<LuaValue> stack = stack();
        if (stack.size() >= MAX_STACK_SLOTS) {
            throw new RuntimeException("No more stack space available");
        }
        stack.add(value);
    }

    public void pop(int n) {
        List<LuaValue> stack = stack();
        for (int i = 0; i < n && !stack.isEmpty(); i++) {
            stack.remove(stack.size() - 1);
        }
    }

    public LuaValue toLuaValue(int stackIndex) {
        assert !luaStacks.isEmpty();
        if (stackIndex == LUA_REGISTRYINDEX) {
            return registry;
        }
        List<LuaValue> stack = stack();
        stackIndex = toAbsoluteIndex(stackIndex);
        stackIndex--;
        if (stackIndex < 0 || stackIndex >= stack.size()) {
            return LuaValue.NONE;
        }
        return stack.get(stackIndex);
    }

    public int toAbsoluteIndex(int stackIndex) {
        if (stackIndex < 0) {
            stackIndex = stack().size() + stackIndex + 1;
        }
        return stackIndex;
    }

    public void remove(int i) {
        if (i == LUA_REGISTRYINDEX) {
            return;
        }
        int index = toAbsoluteIndex(i) - 1;
        List<LuaValue> stack = stack();
        if (index < 0 || index >= stack.size()) {
            return;
        }
        stack().remove(index);
    }

    public void replace(int i, LuaValue value) {
        if (i == LUA_REGISTRYINDEX) {
            return;
        }
        int index = toAbsoluteIndex(i) - 1;
        List<LuaValue> stack = stack();
        if (index < 0 || index >= stack.size()) {
            return;
        }
        stack().set(index, value);
    }

    public void pushAll(Varargs args) {
        for (int i = 0; i < args.narg(); i++) {
            push(args.arg(i + 1));
        }
    }

    public void pushFrame() {
        luaStacks.add(new ArrayList<>());
    }

    public void popFrame() {
        luaStacks.remove(luaStacks.size() - 1);
    }

    public Throwable getError() {
        return (Throwable) globals.get("__jthrowable__").touserdata();
    }

    public void setError(Throwable e) {
        globals.set("__jthrowable__", e == null ? LuaValue.NIL : new JavaObject(e, jObjectMetatable, address));
    }
}
