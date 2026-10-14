package at.kilncloud.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import at.kilncloud.protocol.v1.HeartbeatRequest;
import at.kilncloud.protocol.v1.HeartbeatResponse;
import at.kilncloud.protocol.v1.NodeControlGrpc;
import at.kilncloud.protocol.v1.ProtocolVersion;
import at.kilncloud.protocol.v1.RegisterRequest;
import at.kilncloud.protocol.v1.RegisterResponse;
import at.kilncloud.protocol.v1.RegisterStatus;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the wire contract: field numbers and enum values must never change (ADR 0003), because a
 * different number silently corrupts messages between nodes and masters of different builds.
 */
final class WireContractTest {
    private static int number(Descriptor message, String field) {
        return message.findFieldByName(field).getNumber();
    }

    @Test
    @DisplayName("ProtocolVersion behält seine Feldnummern")
    void protocolVersion_keepsFieldNumbers() {
        Descriptor d = ProtocolVersion.getDescriptor();
        assertEquals(1, number(d, "major"));
        assertEquals(2, number(d, "minor"));
    }

    @Test
    @DisplayName("RegisterRequest behält seine Feldnummern")
    void registerRequest_keepsFieldNumbers() {
        Descriptor d = RegisterRequest.getDescriptor();
        assertEquals(1, number(d, "client_version"));
        assertEquals(2, number(d, "node_name"));
        assertEquals(3, number(d, "join_token"));
    }

    @Test
    @DisplayName("RegisterResponse und Heartbeat-Nachrichten behalten ihre Feldnummern")
    void responsesAndHeartbeat_keepFieldNumbers() {
        assertEquals(1, number(RegisterResponse.getDescriptor(), "server_version"));
        assertEquals(2, number(RegisterResponse.getDescriptor(), "status"));
        assertEquals(1, number(HeartbeatRequest.getDescriptor(), "sequence"));
        assertEquals(2, number(HeartbeatRequest.getDescriptor(), "memory_used_mib"));
        assertEquals(1, number(HeartbeatResponse.getDescriptor(), "sequence"));
    }

    @Test
    @DisplayName("RegisterStatus behält seine Werte")
    void registerStatus_keepsValues() {
        assertEquals(0, RegisterStatus.REGISTER_STATUS_UNSPECIFIED.getNumber());
        assertEquals(1, RegisterStatus.REGISTER_STATUS_OK.getNumber());
        assertEquals(2, RegisterStatus.REGISTER_STATUS_VERSION_INCOMPATIBLE.getNumber());
        assertEquals(3, RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED.getNumber());
        assertEquals(4, RegisterStatus.REGISTER_STATUS_NAME_REJECTED.getNumber());
    }

    @Test
    @DisplayName("Der Dienst heißt kiln.protocol.v1.NodeControl")
    void service_hasStableName() {
        assertEquals("kiln.protocol.v1.NodeControl", NodeControlGrpc.SERVICE_NAME);
    }

    @Test
    @DisplayName("RegisterRequest überlebt Serialisierung und Parsen")
    void registerRequest_roundTrips() throws InvalidProtocolBufferException {
        RegisterRequest original = RegisterRequest.newBuilder()
                .setClientVersion(ProtocolVersions.current())
                .setNodeName("node-1")
                .setJoinToken(ByteString.copyFrom(new byte[] {1, 2, 3}))
                .build();
        assertEquals(original, RegisterRequest.parseFrom(original.toByteArray()));
    }

    @Test
    @DisplayName("Unbekannte Felder einer neueren Minor-Version werden toleriert")
    void parser_toleratesUnknownFields() throws InvalidProtocolBufferException {
        UnknownFieldSet unknown = UnknownFieldSet.newBuilder()
                .addField(99, UnknownFieldSet.Field.newBuilder().addVarint(42).build())
                .build();
        ProtocolVersion newer = ProtocolVersion.newBuilder()
                .setMajor(1)
                .setMinor(5)
                .setUnknownFields(unknown)
                .build();
        ProtocolVersion parsed = ProtocolVersion.parseFrom(newer.toByteArray());
        assertEquals(1, parsed.getMajor());
        assertEquals(5, parsed.getMinor());
    }

    @Test
    @DisplayName("Eine leere RegisterResponse hat den Status UNSPECIFIED, nicht OK")
    void registerResponse_defaultIsNotOk() {
        assertEquals(
                RegisterStatus.REGISTER_STATUS_UNSPECIFIED,
                RegisterResponse.getDefaultInstance().getStatus());
    }
}
