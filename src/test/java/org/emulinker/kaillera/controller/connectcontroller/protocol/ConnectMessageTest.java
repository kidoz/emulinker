package org.emulinker.kaillera.controller.connectcontroller.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ConnectMessage - the Kaillera connection handshake protocol.
 *
 * <p>
 * Connection flow:
 * <ol>
 * <li>Client sends HELLO + protocol version (e.g., "HELLO0.83\0")</li>
 * <li>Server responds with HELLOD00D + port number (e.g.,
 * "HELLOD00D27889\0")</li>
 * <li>Or server responds with TOO if full</li>
 * <li>PING/PONG for keep-alive</li>
 * </ol>
 */
@DisplayName("ConnectMessage Tests")
class ConnectMessageTest {

    private static final Charset CHARSET = Charset.forName("ISO-8859-1");

    @Nested
    @DisplayName("HELLO Message Tests")
    class HelloMessageTests {

        @Test
        @DisplayName("should parse valid HELLO message with protocol version")
        void shouldParseValidHello() throws MessageFormatException {
            String messageStr = "HELLO0.83\0";
            ByteBuffer buffer = CHARSET.encode(messageStr);

            ConnectMessage message = ConnectMessage.parse(buffer);

            assertNotNull(message);
            assertTrue(message instanceof ConnectMessage_HELLO);
            ConnectMessage_HELLO hello = (ConnectMessage_HELLO) message;
            assertEquals("0.83", hello.getProtocol());
        }

        @Test
        @DisplayName("should parse HELLO with different protocol versions")
        void shouldParseHelloWithDifferentVersions() throws MessageFormatException {
            String[] versions = {"0.83", "0.86", "1.0", "test"};

            for (String version : versions) {
                ByteBuffer buffer = CHARSET.encode("HELLO" + version + "\0");
                ConnectMessage message = ConnectMessage.parse(buffer);

                assertTrue(message instanceof ConnectMessage_HELLO);
                assertEquals(version, ((ConnectMessage_HELLO) message).getProtocol());
            }
        }

        @Test
        @DisplayName("should write HELLO message correctly")
        void shouldWriteHelloCorrectly() {
            ConnectMessage_HELLO hello = new ConnectMessage_HELLO("0.83");

            ByteBuffer buffer = ByteBuffer.allocate(100);
            hello.writeTo(buffer);
            buffer.flip();

            String result = CHARSET.decode(buffer).toString();
            assertEquals("HELLO0.83\0", result);
        }

        @Test
        @DisplayName("should throw exception for HELLO without terminator")
        void shouldThrowForHelloWithoutTerminator() {
            ByteBuffer buffer = CHARSET.encode("HELLO0.83"); // missing \0

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }

        @Test
        @DisplayName("should throw exception for HELLO too short")
        void shouldThrowForHelloTooShort() {
            ByteBuffer buffer = CHARSET.encode("HELLO\0"); // missing protocol

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }
    }

    @Nested
    @DisplayName("HELLOD00D Message Tests")
    class HelloD00DMessageTests {

        @Test
        @DisplayName("should parse valid HELLOD00D message")
        void shouldParseValidHelloD00D() throws MessageFormatException {
            ByteBuffer buffer = CHARSET.encode("HELLOD00D27889\0");

            ConnectMessage message = ConnectMessage.parse(buffer);

            assertNotNull(message);
            assertTrue(message instanceof ConnectMessage_HELLOD00D);
            ConnectMessage_HELLOD00D response = (ConnectMessage_HELLOD00D) message;
            assertEquals(27889, response.getPort());
        }

        @Test
        @DisplayName("should parse HELLOD00D with different port numbers")
        void shouldParseHelloD00DWithDifferentPorts() throws MessageFormatException {
            int[] ports = {27888, 27889, 27900, 30000, 65535};

            for (int port : ports) {
                ByteBuffer buffer = CHARSET.encode("HELLOD00D" + port + "\0");
                ConnectMessage message = ConnectMessage.parse(buffer);

                assertTrue(message instanceof ConnectMessage_HELLOD00D);
                assertEquals(port, ((ConnectMessage_HELLOD00D) message).getPort());
            }
        }

        @Test
        @DisplayName("should write HELLOD00D message correctly")
        void shouldWriteHelloD00DCorrectly() {
            ConnectMessage_HELLOD00D response = new ConnectMessage_HELLOD00D(27889);

            ByteBuffer buffer = ByteBuffer.allocate(100);
            response.writeTo(buffer);
            buffer.flip();

            String result = CHARSET.decode(buffer).toString();
            assertEquals("HELLOD00D27889\0", result);
        }

        @Test
        @DisplayName("should throw exception for invalid port number")
        void shouldThrowForInvalidPort() {
            ByteBuffer buffer = CHARSET.encode("HELLOD00Dabc\0"); // not a number

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }
    }

    @Nested
    @DisplayName("TOO Message Tests")
    class TooMessageTests {

        @Test
        @DisplayName("should parse TOO message (server full)")
        void shouldParseTooMessage() throws MessageFormatException {
            ByteBuffer buffer = CHARSET.encode("TOO\0");

            ConnectMessage message = ConnectMessage.parse(buffer);

            assertNotNull(message);
            assertTrue(message instanceof ConnectMessage_TOO);
        }

        @Test
        @DisplayName("should write TOO message correctly")
        void shouldWriteTooCorrectly() {
            ConnectMessage_TOO too = new ConnectMessage_TOO();

            ByteBuffer buffer = ByteBuffer.allocate(100);
            too.writeTo(buffer);
            buffer.flip();

            String result = CHARSET.decode(buffer).toString();
            assertEquals("TOO\0", result);
        }
    }

    @Nested
    @DisplayName("PING/PONG Message Tests")
    class PingPongMessageTests {

        @Test
        @DisplayName("should parse PING message")
        void shouldParsePingMessage() throws MessageFormatException {
            ByteBuffer buffer = CHARSET.encode("PING\0");

            ConnectMessage message = ConnectMessage.parse(buffer);

            assertNotNull(message);
            assertTrue(message instanceof ConnectMessage_PING);
        }

        @Test
        @DisplayName("should parse PONG message")
        void shouldParsePongMessage() throws MessageFormatException {
            ByteBuffer buffer = CHARSET.encode("PONG\0");

            ConnectMessage message = ConnectMessage.parse(buffer);

            assertNotNull(message);
            assertTrue(message instanceof ConnectMessage_PONG);
        }

        @Test
        @DisplayName("should write PING message correctly")
        void shouldWritePingCorrectly() {
            ConnectMessage_PING ping = new ConnectMessage_PING();

            ByteBuffer buffer = ByteBuffer.allocate(100);
            ping.writeTo(buffer);
            buffer.flip();

            String result = CHARSET.decode(buffer).toString();
            assertEquals("PING\0", result);
        }

        @Test
        @DisplayName("should write PONG message correctly")
        void shouldWritePongCorrectly() {
            ConnectMessage_PONG pong = new ConnectMessage_PONG();

            ByteBuffer buffer = ByteBuffer.allocate(100);
            pong.writeTo(buffer);
            buffer.flip();

            String result = CHARSET.decode(buffer).toString();
            assertEquals("PONG\0", result);
        }
    }

    @Nested
    @DisplayName("Invalid Message Handling")
    class InvalidMessageHandling {

        @Test
        @DisplayName("should throw exception for unrecognized message")
        void shouldThrowForUnrecognizedMessage() {
            ByteBuffer buffer = CHARSET.encode("UNKNOWN\0");

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }

        @Test
        @DisplayName("should throw exception for empty message")
        void shouldThrowForEmptyMessage() {
            ByteBuffer buffer = CHARSET.encode("");

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }

        @Test
        @DisplayName("should throw exception for garbage data")
        void shouldThrowForGarbageData() {
            ByteBuffer buffer = ByteBuffer.allocate(10);
            buffer.put(new byte[]{0x00, 0x01, 0x02, 0x03});
            buffer.flip();

            assertThrows(MessageFormatException.class, () -> ConnectMessage.parse(buffer));
        }
    }

    @Nested
    @DisplayName("Message Length Calculations")
    class MessageLengthCalculations {

        @Test
        @DisplayName("should calculate HELLO message length correctly")
        void shouldCalculateHelloLength() {
            ConnectMessage_HELLO hello = new ConnectMessage_HELLO("0.83");

            // HELLO (5) + 0.83 (4) + \0 (1) = 10
            assertEquals(10, hello.getLength());
        }

        @Test
        @DisplayName("should calculate HELLOD00D message length correctly")
        void shouldCalculateHelloD00DLength() {
            ConnectMessage_HELLOD00D response = new ConnectMessage_HELLOD00D(27889);

            // HELLOD00D (9) + 27889 (5) + \0 (1) = 15
            assertEquals(15, response.getLength());
        }
    }
}
