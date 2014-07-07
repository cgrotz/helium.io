/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.Reader;
import java.security.*;
import java.security.cert.Certificate;

/**
 * With security manager secured ScriptingEnvironment
 *
 * @author Christoph Grotz
 */
public class SandBoxedScriptingEnvironment implements Invocable, ScriptEngine {
    private static final Logger logger = LoggerFactory.getLogger(SandBoxedScriptingEnvironment.class);
    private ScriptEngineManager mgr = new ScriptEngineManager();
    private ScriptEngine engine = mgr.getEngineByName("JavaScript");
    private AccessControlContext accessControlContext;

    public SandBoxedScriptingEnvironment() {
        Permissions perms = new Permissions();
        perms.add(new RuntimePermission("accessDeclaredMembers"));
        // Cast to Certificate[] required because of ambiguity:
        ProtectionDomain domain = new ProtectionDomain(new CodeSource(null, (Certificate[]) null),
                perms);
        accessControlContext = new AccessControlContext(new ProtectionDomain[]{domain});
        engine.getContext().setAttribute("passwordHelper", PasswordHelper.get(), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(script, context);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(reader, context);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object eval(final String code) {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(code);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object eval(Reader reader) throws ScriptException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(reader);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object eval(String script, Bindings bindings) throws ScriptException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(script, bindings);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return engine.eval(reader, bindings);
            } catch (ScriptException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return ((Invocable) engine).invokeMethod(thiz, name, args);
            } catch (ScriptException | NoSuchMethodException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object invokeFunction(final String code, final Object... args) {
        return AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                return ((Invocable) engine).invokeFunction(code, args);
            } catch (ScriptException | NoSuchMethodException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }, accessControlContext);
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        return ((Invocable) engine).getInterface(clasz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        return ((Invocable) engine).getInterface(thiz, clasz);
    }

    public void put(String key, Object obj) {
        engine.put(key, obj);
    }

    @Override
    public Object get(String key) {
        return engine.get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return engine.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        engine.setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        return engine.createBindings();
    }

    @Override
    public ScriptContext getContext() {
        return engine.getContext();
    }

    @Override
    public void setContext(ScriptContext context) {
        engine.setContext(context);
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return engine.getFactory();
    }
}
