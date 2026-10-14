/**
 * Defines the central KilnCloud master process.
 */
module at.kilncloud.master {
    requires at.kilncloud.api;
    requires at.kilncloud.common;
    requires at.kilncloud.protocol;
    requires at.kilncloud.setup;
    requires com.fasterxml.jackson.databind;
    requires com.google.protobuf;
    requires io.grpc;
    requires io.grpc.netty.shaded;
    requires io.grpc.stub;
    requires static com.google.errorprone.annotations;
    requires static org.jspecify;

    // SecureConfig creates the configuration record by reflection.
    opens at.kilncloud.master.config to
            com.fasterxml.jackson.databind;
}
