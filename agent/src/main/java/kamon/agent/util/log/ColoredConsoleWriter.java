package kamon.agent.util.log;


import java.io.PrintStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.PropertiesSupport;
import org.pmw.tinylog.writers.Property;
import org.pmw.tinylog.writers.Writer;

/**  Extension of the the tinylog ConsoleWriter enabling to print the level specified in the config.
 * @author Tobias R. Mayer (tobiasrm@me.com)
 * @since 2017-11
 */
@PropertiesSupport(name = "coloredconsole", properties =
        {
                @Property(name = "stream",         type = String.class, optional = true),
                @Property(name = "preLevelTag",    type = String.class, optional = true),
                @Property(name = "preCustomTag1",  type = String.class, optional = true),
                @Property(name = "preCustomTag2",  type = String.class, optional = true),
                @Property(name = "preCustomTag3",  type = String.class, optional = true),
                @Property(name = "preCustomTag4",  type = String.class, optional = true),
                @Property(name = "preCustomTag5",  type = String.class, optional = true),
                @Property(name = "postLevelTag",   type = String.class, optional = true),
                @Property(name = "postCustomTag1", type = String.class, optional = true),
                @Property(name = "postCustomTag2", type = String.class, optional = true),
                @Property(name = "postCustomTag3", type = String.class, optional = true),
                @Property(name = "postCustomTag4", type = String.class, optional = true),
                @Property(name = "postCustomTag5", type = String.class, optional = true),
                @Property(name = "preTrace",       type = String.class, optional = true),
                @Property(name = "preDebug",       type = String.class, optional = true),
                @Property(name = "preInfo",        type = String.class, optional = true),
                @Property(name = "preWarn",        type = String.class, optional = true),
                @Property(name = "preError",       type = String.class, optional = true),
                @Property(name = "postTrace",      type = String.class, optional = true),
                @Property(name = "postDebug",      type = String.class, optional = true),
                @Property(name = "postInfo",       type = String.class, optional = true),
                @Property(name = "postWarn",       type = String.class, optional = true),
                @Property(name = "postError",      type = String.class, optional = true),
                @Property(name = "preCustom1",    type = String.class, optional = true),
                @Property(name = "preCustom2",    type = String.class, optional = true),
                @Property(name = "preCustom3",    type = String.class, optional = true),
                @Property(name = "preCustom4",    type = String.class, optional = true),
                @Property(name = "preCustom5",    type = String.class, optional = true),
                @Property(name = "postCustom1",    type = String.class, optional = true),
                @Property(name = "postCustom2",    type = String.class, optional = true),
                @Property(name = "postCustom3",    type = String.class, optional = true),
                @Property(name = "postCustom4",    type = String.class, optional = true),
                @Property(name = "postCustom5",    type = String.class, optional = true),
        })
public final class ColoredConsoleWriter implements Writer {

    private final PrintStream err;
    private final PrintStream out;


    // level tag specific
    private String preLevelTag;
    private String postLevelTag;

    private Map<Level, String> preLevelParams;
    private Map<Level, String> postLevelParams;


    // custom tag specific
    private String preCustomTag1;
    private String preCustomTag2;
    private String preCustomTag3;
    private String preCustomTag4;
    private String preCustomTag5;

    private String postCustomTag1;
    private String postCustomTag2;
    private String postCustomTag3;
    private String postCustomTag4;
    private String postCustomTag5;

    private String preCustom1;
    private String preCustom2;
    private String preCustom3;
    private String preCustom4;
    private String preCustom5;

    private String postCustom1;
    private String postCustom2;
    private String postCustom3;
    private String postCustom4;
    private String postCustom5;

    private String l;
    private Level lev;


    // --------------------------------------------------------------------

    public ColoredConsoleWriter() {
        err = System.err;
        out = System.out;
        this.initMaps(
                null, null, null, null, null,
                null, null, null, null, null  );
    }

    // --------------------------------------------------------------------

    /** Constructor setting only the print stream but no color values.
     * @param stream
     *            Print stream for outputting log entries
     */
    public ColoredConsoleWriter(final PrintStream stream) {
        err = stream;
        out = stream;
        this.initMaps(
                null, null, null, null, null,
                null, null, null, null, null  );
    }

    // --------------------------------------------------------------------

    /** Constructor setting only the output stream as string but no color values.
     * @param stream
     */
    ColoredConsoleWriter(final String stream){
        this(stream, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null );
    }

    // --------------------------------------------------------------------

    /** Constructor with properties being provided.
     * Note that those properties are provided to the constructor
     * in the EXACT sequence. If not provided they are 'null' and we don't need to overload the constructor.
     * @param stream name of system print stream for outputting log entries ("out" for {@link System.out} or "err" for {@link System.err})
     * @param preLevelTag  pre-level tag to be replaced with the data of the corresponding log level
     * @param postLevelTag pre-level tag to be replaced with the data of the corresponding log level
     * @param preTrace  the 'trace' data for pre-level tag
     * @param preDebug  the 'debug' data for pre-level tag
     * @param preInfo   the 'info' data for pre-level tag
     * @param preWarn   the 'warn' data for pre-level tag
     * @param preError  the 'error' data for pre-level tag
     * @param postTrace the 'trace' data for post-level tag
     * @param postDebug the 'debug' data for post-level tag
     * @param postInfo  the 'info' data for post-level tag
     * @param postWarn  the 'warn' data for post-level tag
     * @param postError the 'error' data for post-level tag
     */
    ColoredConsoleWriter(final String stream,
                         final String preLevelTag,  final String preCustomTag1,  final String preCustomTag2,  final String preCustomTag3,  final String preCustomTag4,  final String preCustomTag5,
                         final String postLevelTag, final String postCustomTag1, final String postCustomTag2, final String postCustomTag3, final String postCustomTag4, final String postCustomTag5,
                         final String preTrace,    final String preDebug,    final String preInfo,     final String preWarn,     final String preError,
                         final String postTrace,   final String postDebug,   final String postInfo,    final String postWarn,    final String postError,
                         final String preCustom1,  final String preCustom2,  final String preCustom3,  final String preCustom4,  final String preCustom5,
                         final String postCustom1, final String postCustom2, final String postCustom3, final String postCustom4, final String postCustom5) {

        if (stream == null) {
            err = System.err;
            out = System.out;
        } else if ("err".equalsIgnoreCase(stream)) {
            err = System.err;
            out = System.err;
        } else if ("out".equalsIgnoreCase(stream)) {
            err = System.out;
            out = System.out;
        } else {
            throw new IllegalArgumentException("Stream must be \"out\" or \"err\", \"" + stream + "\" is not a valid stream name");
        }

        // set the user individualized level tag
        this.preLevelTag  = preLevelTag;
        this.postLevelTag = postLevelTag;

        this.preCustomTag1 = preCustomTag1;
        this.preCustomTag2 = preCustomTag2;
        this.preCustomTag3 = preCustomTag3;
        this.preCustomTag4 = preCustomTag4;
        this.preCustomTag5 = preCustomTag5;

        this.postCustomTag1 = postCustomTag1;
        this.postCustomTag2 = postCustomTag2;
        this.postCustomTag3 = postCustomTag3;
        this.postCustomTag4 = postCustomTag4;
        this.postCustomTag5 = postCustomTag5;

        this.preCustom1  = preCustom1;
        this.preCustom2  = preCustom2;
        this.preCustom3  = preCustom3;
        this.preCustom4  = preCustom4;
        this.preCustom5  = preCustom5;

        this.postCustom1 = postCustom1;
        this.postCustom2 = postCustom2;
        this.postCustom3 = postCustom3;
        this.postCustom4 = postCustom4;
        this.postCustom5 = postCustom5;


        // setup the maps with level specific data for tag replacement
        this.initMaps(
                preTrace,  preDebug,  preInfo, preWarn, preError,
                postTrace, postDebug, postInfo, postWarn, postError);
    }

    // --------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.pmw.tinylog.writers.Writer#getRequiredLogEntryValues()
     */
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.of(LogEntryValue.LEVEL, LogEntryValue.RENDERED_LOG_ENTRY);
    }

    // --------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.pmw.tinylog.writers.Writer#init(org.pmw.tinylog.Configuration)
     */
    public void init(final Configuration configuration) {
    }

    // --------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.pmw.tinylog.writers.Writer#write(org.pmw.tinylog.LogEntry)
     */
    public void write(final LogEntry logEntry) {
        l  = logEntry.getRenderedLogEntry();
        lev = logEntry.getLevel();

        System.out.println("upiupupup tagg->>" + preLevelTag);
        // replace pre-level a tag it has been provided
        if( preLevelTag != null)   l = l.replace(preLevelTag, preLevelParams.get( lev ));

        // replace post-level a tag it has been provided
        if( postLevelTag != null)  l = l.replace(postLevelTag, postLevelParams.get( lev ));

        if( preCustomTag1  != null) l = l.replace(preCustomTag1,  ( preCustom1  == null  ? "" : preCustom1) );
        if( postCustomTag1 != null) l = l.replace(postCustomTag1, ( postCustom1 == null  ? "" : postCustom1) );
        if( preCustomTag2  != null) l = l.replace(preCustomTag2,  ( preCustom2  == null  ? "" : preCustom2) );
        if( postCustomTag2 != null) l = l.replace(postCustomTag2, ( postCustom2 == null  ? "" : postCustom2) );
        if( preCustomTag3  != null) l = l.replace(preCustomTag3,  ( preCustom3  == null  ? "" : preCustom3) );
        if( postCustomTag3 != null) l = l.replace(postCustomTag3, ( postCustom3 == null  ? "" : postCustom3) );
        if( preCustomTag4  != null) l = l.replace(preCustomTag4,  ( preCustom4  == null  ? "" : preCustom4) );
        if( postCustomTag4 != null) l = l.replace(postCustomTag4, ( postCustom4 == null  ? "" : postCustom4) );
        if( preCustomTag5  != null) l = l.replace(preCustomTag5,  ( preCustom5  == null  ? "" : preCustom5) );
        if( postCustomTag5 != null) l = l.replace(postCustomTag5, ( postCustom5 == null  ? "" : postCustom5) );

        getPrintStream(lev).print( l );
    }

    // --------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.pmw.tinylog.writers.Writer#flush()
     */
    public void flush() {
        // Do nothing
    }

    // --------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.pmw.tinylog.writers.Writer#close()
     */
    public void close() {
        // Do nothing
    }

    // --------------------------------------------------------------------

    /** Get the {@link PrintStream} for the indicated Level.
     * @param level the log level
     * @return {@link PrintStream} for the log level to be used
     */
    private PrintStream getPrintStream(final Level level) {
        if (level == Level.ERROR || level == Level.WARNING) {
            return err;
        } else {
            return out;
        }
    }

    // --------------------------------------------------------------------

    /** Prepares the map with data that shall replace the pre-/post level tags.
     * @param preTrace  the 'trace' data for pre-level tag
     * @param preDebug  the 'debug' data for pre-level tag
     * @param preInfo   the 'info' data for pre-level tag
     * @param preWarn   the 'warn' data for pre-level tag
     * @param preError  the 'error' data for pre-level tag
     * @param postTrace the 'trace' data for post-level tag
     * @param postDebug the 'debug' data for post-level tag
     * @param postInfo  the 'info' data for post-level tag
     * @param postWarn  the 'warn' data for post-level tag
     * @param postError the 'error' data for post-level tag
     */
    private void initMaps(
            String preTrace,    String preDebug,    String preInfo,     String preWarn,     String preError,
            String postTrace,   String postDebug,   String postInfo,    String postWarn,    String postError ) {

        preLevelParams  = new HashMap<Level, String>();
        postLevelParams = new HashMap<Level, String>();

        preLevelParams.put(Level.TRACE,    ( preTrace == null  ? "" : preTrace) );
        preLevelParams.put(Level.DEBUG,    ( preDebug == null  ? "" : preDebug) );
        preLevelParams.put(Level.INFO,     ( preInfo == null   ? "" : preInfo) );
        preLevelParams.put(Level.WARNING,  ( preWarn == null   ? "" : preWarn) );
        preLevelParams.put(Level.ERROR,    ( preError == null  ? "" : preError) );

        postLevelParams.put(Level.TRACE,   ( postTrace == null ? "" : postTrace) );
        postLevelParams.put(Level.DEBUG,   ( postDebug == null ? "" : postDebug) );
        postLevelParams.put(Level.INFO,    ( postInfo == null  ? "" : postInfo) );
        postLevelParams.put(Level.WARNING, ( postWarn == null  ? "" : postWarn) );
        postLevelParams.put(Level.ERROR,   ( postError == null ? "" : postError) );

    }
}