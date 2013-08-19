package org.jenkinsci.plugins.scriptler.util;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.DelegatingCallable;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptler.config.Parameter;
import org.jenkinsci.plugins.scriptler.share.EnvObjects;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * An implementation that allows any shell script to be executed. Note, that if the script starts with shebang string,
 * then the interpreter will be chosen by it. Examples:<p>
 * Bash: <pre> {@code
 * #/bin/bash
 * echo 'hello'
 * }</pre>
 * Ruby: <pre> {@code
 * #!/usr/bin/ruby
 * puts 'Hey, dude!'
 * }</pre>
 * Python: <pre> {@code
 * #!/usr/bin/python
 * print 'That is again me'
 * }</pre>
 * </p>
 * Those interpreters that are not listed in {@link #INTERPRETER_PARAMS} should go with parameters explicitly specified
 * (like for bash it's -c or for ruby it's -e) in the shebang.
 * You can use Groovy as well, but there is {@link GroovyScript} that offers more functionality because some additional
 * system parameters are injected into it.
 *
 * @author ctapobep
 */
public class ShellScript implements DelegatingCallable<Object, RuntimeException>, Serializable {
    private static final long serialVersionUID = 1L;
    private final String command;
    private final TaskListener taskListener;
    private final FilePath workingDir;
    private final Parameter[] params;
    /**
     * Contains parameters that have to be passed to the interpreters in order for them to recognize that the script
     * itself is passed via command line args.
     */
    private final static Map<String, String> INTERPRETER_PARAMS = new HashMap<String, String>();

    static {
        INTERPRETER_PARAMS.put("bash", "-c");
        INTERPRETER_PARAMS.put("ruby", "-e");
        INTERPRETER_PARAMS.put("python", "-c");
    }

    public ShellScript(String command, FilePath workingDir, TaskListener taskListener, Parameter[] params) {
        this.command = command;
        this.workingDir = workingDir;
        this.params = params;
        this.taskListener = taskListener;
    }

    public ClassLoader getClassLoader() {
        return Jenkins.getInstance().getPluginManager().uberClassLoader;
    }

    public Object call() throws RuntimeException {
        try {
            Launcher launcher = new EnvObjects(taskListener).getLocalLauncher();
            PrintStream logger = taskListener.getLogger();

            FilePath currentFolder = workingDir == null ? new FilePath(launcher.getChannel(), "/tmp") : workingDir;

            List<String> commands = new ArrayList<String>(getInterpreter(command, launcher.getChannel()));
            commands.add(command);
            int resultCode = launcher.launch()
                    .cmds(commands).envs(Parameter.toMap(params))
                    .stderr(logger).stdout(logger).pwd(currentFolder).join();
            return resultCode == 0;
        } catch (IOException e) {
            return logAndRethrowIllegalState(e);
        } catch (InterruptedException e) {
            return logAndRethrowIllegalState(e);
        }
    }

    private Object logAndRethrowIllegalState(Exception e) {
        taskListener.getLogger().println("Error " + e.getMessage());
        throw new IllegalStateException("Error occurred during execution of shell script: ", e);
    }

    /**
     * Parses the interpreter by the command with shebang. If the command does not start with shebang, then system
     * defaults are used.
     *
     * @param command        the whole script body
     * @param virtualChannel to work with OS in order to find out the default interpreter
     * @return an interpreter itself from shebang with respective params
     */
    private List<String> getInterpreter(String command, VirtualChannel virtualChannel) {
        List<String> interpreterWithParams = new ArrayList<String>();
        if (command.startsWith("#!") && command.contains("\n")) {
            String interpreter = command.split("\n")[0].substring(2);
            interpreter = StringUtils.trim(interpreter);
            interpreterWithParams.addAll(asList(interpreter.split(Pattern.quote(" "))));
            interpreterWithParams.addAll(getInterpreterParameter(interpreter));
        } else {
            interpreterWithParams.add(getShellOrDefault(virtualChannel));
        }
        return interpreterWithParams;
    }

    /**
     * Determines what interpreter params should be passed in order the interpreter to take code from stdin.
     * @param interpreterFromShebang the interpreter path (like /bin/bash -e)
     * @return parameters to pass to the interpreter so that it treats one of params as source code
     */
    private List<String> getInterpreterParameter(String interpreterFromShebang) {
        List<String> params = new ArrayList<String>();
        for (Map.Entry<String, String> interpreter : INTERPRETER_PARAMS.entrySet()) {
            if (interpreterFromShebang.contains(interpreter.getKey())) {
                String param = interpreter.getValue();
                if (!interpreterFromShebang.contains(param)) {
                    params.add(param);
                }
            }
        }
        return params;
    }

    public String getShellOrDefault(VirtualChannel channel) {
        String interpreter = null;
        try {
            interpreter = channel.call(new ShellInterpreter());
        } catch (IOException e) {
            taskListener.getLogger().println("[WARN] " + e.getMessage());
        } catch (InterruptedException e) {
            taskListener.getLogger().println("[WARN] " + e.getMessage());
        }
        if (interpreter == null) {
            interpreter = Functions.isWindows() ? "sh" : "/bin/sh";
        }
        return interpreter;
    }

    /** Taken from {@link hudson.tasks.Shell} */
    private static final class ShellInterpreter implements Callable<String, IOException> {
        private static final long serialVersionUID = 1L;

        public String call() throws IOException {
            return Functions.isWindows() ? "sh" : "/bin/sh";
        }
    }
}
