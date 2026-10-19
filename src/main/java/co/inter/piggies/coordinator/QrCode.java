package co.inter.piggies.coordinator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Conteudo de um QR Code de pagamento: piggies://pay?merchant={merchantId}&amount={piggies}. */
public record QrCode(String merchantId, long amount) {

    public static QrCode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidQrCodeException("QR Code vazio");
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidQrCodeException("QR Code mal formado");
        }
        if (!"piggies".equals(uri.getScheme()) || !"pay".equals(uri.getHost()) || uri.getQuery() == null) {
            throw new InvalidQrCodeException("QR Code nao e um pagamento Piggies");
        }
        Map<String, String> params = new HashMap<>();
        for (String pair : uri.getQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        String merchantId = params.get("merchant");
        if (merchantId == null || merchantId.isBlank()) {
            throw new InvalidQrCodeException("QR Code sem merchant");
        }
        long amount;
        try {
            amount = Long.parseLong(params.getOrDefault("amount", ""));
        } catch (NumberFormatException e) {
            throw new InvalidQrCodeException("Valor deve ser um numero inteiro de Piggies");
        }
        if (amount <= 0) {
            throw new InvalidQrCodeException("Valor deve ser maior que zero");
        }
        return new QrCode(merchantId, amount);
    }

    public static class InvalidQrCodeException extends RuntimeException {
        public InvalidQrCodeException(String message) {
            super(message);
        }
    }
}
