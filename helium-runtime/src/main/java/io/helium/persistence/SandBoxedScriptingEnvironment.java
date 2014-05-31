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

package io.helium.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.UUID;

/**
 * With security manager secured ScriptingEnvironment
 *
 * @author Christoph Grotz
 */
public class SandBoxedScriptingEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(SandBoxedScriptingEnvironment.class);
    private ScriptEngineManager mgr = new ScriptEngineManager();
    private ScriptEngine engine = mgr.getEngineByName("JavaScript");
    private AccessControlContext accessControlContext;


    public static void main(String args[]){
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try {
            engine.eval("function convert(){ return {\"test\": \"Hallo Welt\" };}");
            System.out.println(((Invocable) engine).invokeFunction("convert"));
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public SandBoxedScriptingEnvironment() {
        Permissions perms = new Permissions();
        perms.add(new RuntimePermission("accessDeclaredMembers"));
        // Cast to Certificate[] required because of ambiguity:
        ProtectionDomain domain = new ProtectionDomain(new CodeSource(null, (Certificate[]) null),
                perms);
        accessControlContext = new AccessControlContext(new ProtectionDomain[]{domain});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object eval(final String code) {
        return AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    long startTime = System.currentTimeMillis();
                    engine.eval("function convert(){ return "+code+";}");
                    LOGGER.info("evaluation of convert function " + (System.currentTimeMillis() - startTime) + "ms");
                    Object retValue = ((Invocable) engine).invokeFunction("convert");
                    LOGGER.info("invocation of convert function " + (System.currentTimeMillis() - startTime) + "ms");
                    return retValue;
                    //return engine.eval(code);
                } catch (ScriptException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (NoSuchMethodException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return null;
            }
        }, accessControlContext);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object invokeFunction(final String code, final Object... args) {
        return AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    long startTime = System.currentTimeMillis();
                    String functionName = "rule" + UUID.randomUUID().toString().replaceAll("-","");
                    engine.eval("var " + functionName + " = " + code + ";");
                    LOGGER.info("evaluation of rule function " + (System.currentTimeMillis() - startTime) + "ms");
                    Object returnValue = ((Invocable) engine).invokeFunction(functionName, args);
                    LOGGER.info("invocation of rule function " + (System.currentTimeMillis() - startTime) + "ms");
                    return returnValue;
                } catch (ScriptException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (NoSuchMethodException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return null;
            }
        }, accessControlContext);
    }

    public void put(String key, Object obj) {
        engine.put(key, obj);
    }
}
