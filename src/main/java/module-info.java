import no.ssb.helidon.application.HelidonApplicationBuilder;

module no.ssb.helidon.testing {

    requires no.ssb.helidon.application;
    requires no.ssb.helidon.media.protobuf.json.server;

    requires java.net.http;
    requires org.slf4j;
    requires org.junit.jupiter.api;
    requires io.helidon.config;
    requires io.helidon.webserver;
    requires javax.inject;

    uses HelidonApplicationBuilder;
}