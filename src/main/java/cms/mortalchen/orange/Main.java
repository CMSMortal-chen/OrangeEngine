package cms.mortalchen.orange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;

public class Main {
    private static final Logger SYS_OUT = LogManager.getLogger("STDOUT");
    private static final Logger SYS_ERR = LogManager.getLogger("STDERR");

    void main() {
        redirectSysOut();
        MainGame mainGame = new MainGame();
        mainGame.startGame();
    }

    private static void redirectSysOut() {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        System.setOut(new PrintStream(new OutputStream() {
            private final StringBuilder sb = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = sb.toString();
                    SYS_OUT.info("[SYS_OUT] {}", line);
                    origOut.println(line);
                    sb.setLength(0);
                } else if (b != '\r') {
                    sb.append((char) b);
                }
            }
            @Override
            public void flush() {
                if (sb.length() > 0) {
                    SYS_OUT.info("[SYS_OUT] {}", sb.toString());
                    origOut.print(sb.toString());
                    sb.setLength(0);
                }
                origOut.flush();
            }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            private final StringBuilder sb = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = sb.toString();
                    SYS_ERR.error("[SYS_ERR] {}", line);
                    origErr.println(line);
                    sb.setLength(0);
                } else if (b != '\r') {
                    sb.append((char) b);
                }
            }
            @Override
            public void flush() {
                if (sb.length() > 0) {
                    SYS_ERR.error("[SYS_ERR] {}", sb.toString());
                    origErr.print(sb.toString());
                    sb.setLength(0);
                }
                origErr.flush();
            }
        }));
    }
}
