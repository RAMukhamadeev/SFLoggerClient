package org.db.example;

import org.db.BayeuxParameters;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Container for BayeuxParameters and the bearerToken.
 * Calls BayeuxParameters supplier in re-authentication scenarios.
 */
public class BearerTokenProvider implements Function<Boolean, String> {

    private final Supplier<BayeuxParameters> sessionSupplier;
    private String bearerToken;

    public BearerTokenProvider(Supplier<BayeuxParameters> sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    public BayeuxParameters login() {
        BayeuxParameters parameters = sessionSupplier.get();
        bearerToken = parameters.bearerToken();
        return parameters;
    }

    @Override
    public String apply(Boolean reAuth) {
        if (reAuth) {
            try {
                bearerToken = sessionSupplier.get().bearerToken();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return bearerToken;
    }
}
