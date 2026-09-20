package kz.iitu.springlab.notify;

import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component("console")
@Order(1)
public class ConsoleNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

    @Override
    public String send(String message) {
        log.info("CONSOLE >> {}", message);
        return "console: " + message;
    }

    @Override
    public String channel() { return "console"; }
}