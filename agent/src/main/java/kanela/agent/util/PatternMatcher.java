package kanela.agent.util;

import net.bytebuddy.matcher.ElementMatcher;

import java.util.regex.Pattern;

public class PatternMatcher extends ElementMatcher.Junction.AbstractBase<String> {
    private final Pattern regex;

    public PatternMatcher(final String regex) {
        this.regex = Pattern.compile(regex);
    }

    @Override
    public boolean matches(final String target) {
        return regex.matcher(target).matches();
    }
}
