package org.jenkinsci.plugins.scriptler.share;

import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.LocalChannel;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.StringUtils;

/**
 * Provides Jenkins system objects related to the current env (master, slave).
 *
 * @author ctapobep
 */
public class EnvObjects {
    private final TaskListener listener;

    public EnvObjects(TaskListener listener) {
        this.listener = listener;
    }

    public Launcher getLocalLauncher() {
        LocalChannel channel = new LocalChannel(Computer.threadPoolForRemoting);
        return new Launcher.LocalLauncher(listener, channel);
    }

    public Launcher getLauncher(String nodeName) {
        if (StringUtils.isEmpty(nodeName)) {
            throw new IllegalArgumentException("The node name cannot be null or empty, no way to determine where to run.");
        }
        Computer comp = Hudson.getInstance().getComputer(nodeName);
        if (comp instanceof SlaveComputer) {
            return new Launcher.RemoteLauncher(listener, comp.getChannel(), true);
        } else {
            return new Launcher.LocalLauncher(listener, comp.getChannel());
        }
    }
}
