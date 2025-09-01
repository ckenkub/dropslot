package com.dropslot.user.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMailer implements Mailer {
    private final List<String> sent = new ArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        String entry = String.format("to=%s;subject=%s;body=%s", to, subject, body);
        // best-effort extract a token-like value from body: look for an
        // alphanumeric/hyphen
        // sequence of length >= 6 (our tokens are 8 chars from UUID). This is more
        // robust
        // than splitting on whitespace and stripping punctuation.
        String token = null;
        try {
            Pattern p = Pattern.compile("\\b([A-Za-z0-9-]{6,})\\b");
            Matcher m = p.matcher(body);
            if (m.find()) {
                token = m.group(1);
            }
        } catch (Exception e) {
            // fall back to best-effort previous method in case of unexpected input
            String[] parts = body == null ? new String[0] : body.trim().split("\\s+");
            if (parts.length > 0) {
                token = parts[parts.length - 1].replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "");
            }
        }
        if (token != null && !token.isEmpty()) {
            entry = entry + ";token=" + token;
        }
        sent.add(entry);
        System.out.println("[InMemoryMailer] " + sent.get(sent.size() - 1));
    }

    public List<String> getSent() {
        return sent;
    }
}
