package kz.iitu.springlab.notify;

public interface Notifier {

    String send(String message);   // returns what was "sent"

    String channel();              // channel name for the report
}