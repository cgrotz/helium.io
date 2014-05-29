package io.helium.server.protocols.mqtt.protocol;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class Connect extends Command {
    private final String protocolName;
    private final short protocolVersion;
    private final boolean cleanSession;
    private final int keepAlive;
    private final String clientId;
    private final Optional<String> username;
    private final Optional<String> password;

    public Connect(CommandType commandType,
                   boolean dupFlag,
                   QosLevel qosLevel,
                   boolean retainFlag,
                   long length,
                   String protocolName,
                   short protocolVersion,
                   boolean cleanSession,
                   int keepAlive,
                   String clientId,
                   Optional<String> username,
                   Optional<String> password) {
        super(commandType, dupFlag, qosLevel, retainFlag, length);
        this.protocolName = protocolName;
        this.protocolVersion = protocolVersion;
        this.cleanSession = cleanSession;
        this.keepAlive = keepAlive;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public short getProtocolVersion() {
        return protocolVersion;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public String getClientId() {
        return clientId;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public Optional<String> getPassword() {
        return password;
    }
}
