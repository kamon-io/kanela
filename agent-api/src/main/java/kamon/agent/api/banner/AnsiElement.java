package kamon.agent.api.banner;
public interface AnsiElement {

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiStyle#NORMAL}
     */
    @Deprecated
    AnsiElement NORMAL = new DefaultAnsiElement("0");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiStyle#BOLD}
     */
    @Deprecated
    AnsiElement BOLD = new DefaultAnsiElement("1");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiStyle#FAINT}
     */
    @Deprecated
    AnsiElement FAINT = new DefaultAnsiElement("2");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiStyle#ITALIC}
     */
    @Deprecated
    AnsiElement ITALIC = new DefaultAnsiElement("3");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiStyle#UNDERLINE}
     */
    @Deprecated
    AnsiElement UNDERLINE = new DefaultAnsiElement("4");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#BLACK}
     */
    @Deprecated
    AnsiElement BLACK = new DefaultAnsiElement("30");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#RED}
     */
    @Deprecated
    AnsiElement RED = new DefaultAnsiElement("31");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#GREEN}
     */
    @Deprecated
    AnsiElement GREEN = new DefaultAnsiElement("32");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#YELLOW}
     */
    @Deprecated
    AnsiElement YELLOW = new DefaultAnsiElement("33");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#BLUE}
     */
    @Deprecated
    AnsiElement BLUE = new DefaultAnsiElement("34");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#MAGENTA}
     */
    @Deprecated
    AnsiElement MAGENTA = new DefaultAnsiElement("35");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#CYAN}
     */
    @Deprecated
    AnsiElement CYAN = new DefaultAnsiElement("36");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#WHITE}
     */
    @Deprecated
    AnsiElement WHITE = new DefaultAnsiElement("37");

    /**
     * @deprecated in 1.3.0 in favor of {@link AnsiColor#DEFAULT}
     */
    @Deprecated
    AnsiElement DEFAULT = new DefaultAnsiElement("39");

    /**
     * @return the ANSI escape code
     */
    @Override
    String toString();

    /**
     * Internal default {@link AnsiElement} implementation.
     */
    class DefaultAnsiElement implements AnsiElement {

        private final String code;

        DefaultAnsiElement(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return this.code;
        }

    }

}