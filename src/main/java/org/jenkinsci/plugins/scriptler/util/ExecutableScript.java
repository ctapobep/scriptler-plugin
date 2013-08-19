package org.jenkinsci.plugins.scriptler.util;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jenkinsci.plugins.scriptler.config.Parameter;
import org.jenkinsci.plugins.scriptler.config.Script;

import java.util.List;

/**
 * A builder that returns either {@link GroovyScript} or {@link ShellScript} depending on the specified params.
 *
 * @author ctapobep
 */
public class ExecutableScript {
    private final Script script;
    private Parameter[] params;
    private Launcher launcher;
    private TaskListener listener;
    private AbstractBuild<?, ?> build;

    private ExecutableScript(Script script) {
        this.script = script;
    }

    public static ExecutableScript withScriptInfo(Script script) {
        return new ExecutableScript(script);
    }

    public ExecutableScript withParams(Parameter[] params) {
        this.params = params;
        return this;
    }

    public ExecutableScript withParams(List<Parameter> params) {
        this.params = params.toArray(new Parameter[params.size()]);
        return this;
    }

    public ExecutableScript withLauncher(Launcher launcher) {
        this.launcher = launcher;
        return this;
    }

    public ExecutableScript withListener(TaskListener listener) {
        this.listener = listener;
        return this;
    }

    /** Is not harmful, but needed only for {@link GroovyScript} and only in situations when build var is available */
    public ExecutableScript withBuild(AbstractBuild<?, ?> build) {
        this.build = build;
        return this;
    }

    public Callable<Object, RuntimeException> build() {
        Script.Interpreter interpreter = Script.Interpreter.parse(script.interpreter);
        if (interpreter == Script.Interpreter.SHEBANG) {
            FilePath workspace = build == null ? null : build.getWorkspace();
            return new ShellScript(script.script, workspace, listener, params);
        } else if (interpreter == Script.Interpreter.GROOVY && script.onlyMaster) {
            return new GroovyScript(script.script, params, true, listener, launcher, build);
        } else if (interpreter == Script.Interpreter.GROOVY) {
            return new GroovyScript(script.script, params, true, listener);
        } else {
            throw new IllegalArgumentException("Could not figure out how to run the script: " + this.toString());
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
