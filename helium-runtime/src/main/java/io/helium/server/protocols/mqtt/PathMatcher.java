package io.helium.server.protocols.mqtt;

import io.helium.common.Path;

/**
 * Created by balu on 26.05.14.
 */
public class PathMatcher {
    public boolean matchPath(String subscribedTopic, Path topic) {
        String[] subscribedTopicElements = subscribedTopic.split("/");
        String[] topicElements = topic.toArray();
        int i = 0;
        for( String currentElement : subscribedTopicElements ) {
           if(currentElement.equalsIgnoreCase("+")) {
                    return (topicElements.length == i+1);
           }
           if(currentElement.equalsIgnoreCase("#")) {
                    return (topicElements.length >= i+1);
           }
           else {
                if(topicElements.length > i+1) {
                    if(topicElements[i].equalsIgnoreCase(currentElement)) {
                        return false;
                    }
                }
                else if(topicElements.length == i+1) {
                    return (topicElements[i].equalsIgnoreCase(currentElement));
                }
                else {
                    return false;
                }
           }
           i = i+1;
        }
        return false;
    }
}
