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

package io.helium.admin;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.helium.Helium;
import io.helium.common.Path;
import io.helium.json.Node;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class HeliumAdmin {
    private VelocityEngine engine;
    private Helium helium;

    public HeliumAdmin(Helium helium) {
        this.helium = helium;
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        engine = new VelocityEngine(props);
        engine.init();
    }

    public String servePath(String contextPath, String basePath, String absolutePath, Path relPath,
                            String uri) {
        StringWriter writer = new StringWriter();
        if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".html")) {
            try {
                String resourceUri = uri.replaceFirst(contextPath, "");
                URL uuid = Thread.currentThread().getContextClassLoader()
                        .getResource(resourceUri.startsWith("/") ? resourceUri.substring(1) : resourceUri);
                String content = Resources.toString(uuid, Charsets.UTF_8);
                writer.append(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return writer.toString();
        } else {
            VelocityContext context = new VelocityContext();
            context.put("heliumUrl", absolutePath);
            context.put("contextPath", contextPath);
            context.put("pathElements", extractPath(basePath, relPath));
            Object value = this.helium.getPersistence().get(relPath);
            context.put("name", relPath.getLastElement());
            context.put("simple", !(value instanceof Node));
            if (value instanceof Node) {
                context.put("elements", extractValue((Node) value));
                context.put("subs", extractSubs((Node) value, basePath, relPath));
            } else {
                context.put("value", value);
            }
            Template template = null;
            try {
                RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
                URL uuid = Thread.currentThread().getContextClassLoader().getResource("index.html");
                SimpleNode node = runtimeServices.parse(Resources.newReaderSupplier(uuid, Charsets.UTF_8)
                        .getInput(), "Template name");
                template = new Template();
                template.setRuntimeServices(runtimeServices);
                template.setData(node);
                template.initDocument();
            } catch (ResourceNotFoundException e) {
                // couldn't find the template
                e.printStackTrace();
            } catch (ParseErrorException e) {
                // syntax error: problem parsing the template
                e.printStackTrace();
            } catch (MethodInvocationException e) {
                // something invoked in the template
                // threw an exception
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            template.merge(context, writer);
            return writer.toString();
        }
    }

    private List<AdminPath> extractSubs(Node value, String basePath, Path path) {
        List<AdminPath> elements = Lists.newArrayList();
        for (String key : value.keys()) {
            if (value.get(key) instanceof Node) {
                elements.add(new AdminPath(basePath, path.append(key)));
            }
        }
        return elements;
    }

    private List<AdminElement> extractValue(Node value) {
        List<AdminElement> elements = Lists.newArrayList();
        for (String key : value.keys()) {
            if (!(value.get(key) instanceof Node)) {
                elements.add(new AdminElement(key, value.get(key)));
            }
        }
        return elements;
    }

    private List<AdminPath> extractPath(String basePath, Path relPath) {
        List<AdminPath> paths = Lists.newArrayList();
        Path path = relPath;
        while (!path.isEmtpy()) {
            paths.add(new AdminPath(basePath, path));
            path = path.getParent();
        }
        return Lists.reverse(paths);
    }
}
