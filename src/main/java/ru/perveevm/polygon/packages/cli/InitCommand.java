package ru.perveevm.polygon.packages.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
@CommandLine.Command(name = "init", description = "Saves Polygon API credentials for further use")
public class InitCommand implements Callable<Integer> {
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    InitCommandArguments apiArguments;

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    PolygonLoginArguments loginArguments;

    @Override
    public Integer call() {
        Preferences preferences = Preferences.userRoot().node("ru.perveevm.polygon.packages.api-credentials");
        preferences.put("key", apiArguments.key);
        preferences.put("secret", apiArguments.secret);

        if (loginArguments.login != null) {
            preferences.put("login", loginArguments.login);
            preferences.put("password", loginArguments.password);
        }

        return 0;
    }

    static class InitCommandArguments {
        @CommandLine.Option(names = {"-k", "--key"}, required = true, interactive = true, arity = "0..1",
                description = "API key")
        String key;

        @CommandLine.Option(names = {"-s", "--secret"}, required = true, interactive = true, arity = "0..1",
                description = "API secret")
        String secret;
    }

    static class PolygonLoginArguments {
        @CommandLine.Option(names = {"-l", "--login"}, required = true, interactive = true, arity = "0..1",
                description = "Polygon login (not email) for unsupported API actions (unnecessary)")
        String login;

        @CommandLine.Option(names = {"-p", "--password"}, required = true, interactive = true, arity = "0..1",
                description = "Polygon password for unsupported API actions (unnecessary)")
        String password;
    }
}
