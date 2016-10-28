package org.openpnp.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openpnp.model.AbstractModelObject;
import org.pmw.tinylog.Logger;

public class FiniteStateMachine<State, Message> extends AbstractModelObject {
    private final State initialState;
    private State state;

    private Map<State, Map<Message, Transition>> transitions = new HashMap<>();

    public FiniteStateMachine(State initialState) {
        this.initialState = initialState;
        setState(initialState);
    }

    public void send(Message message) throws Exception {
        while (message != null) {
            State state = getState();
            Map<Message, Transition> transitions = this.transitions.get(state);
            if (transitions == null) {
                throw new Exception("No defined transitions from " + state);
            }
            Transition transition = transitions.get(message);
            if (transition == null) {
                throw new Exception("No defined transitions from " + state + " for " + message);
            }
            if (transition.task != null) {
                transition.task.task();
            }
            setState(transition.toState);
            Logger.trace(message + " => " + state + " -> " + transition.toState);
            message = transition.nextMessage;
        }
    }
    
    public boolean canSend(Message message) {
        State state = getState();
        Map<Message, Transition> transitions = this.transitions.get(state);
        if (transitions == null) {
            return false;
        }
        Transition transition = transitions.get(message);
        if (transition == null) {
            return false;
        }
        return true;
    }

    public void add(State fromState, Message message, State toState) {
        add(fromState, message, toState, null, null);
    }

    public void add(State fromState, Message message, State toState, Task task) {
        add(fromState, message, toState, task, null);
    }
    
    public void add(State fromState, Message message, State toState, Message nextMessage) {
        add(fromState, message, toState, null, nextMessage);
    }

    public void add(State fromState, Message message, State toState, Task task, Message nextMessage) {
        Map<Message, Transition> t = transitions.get(fromState);
        if (t == null) {
            t = new HashMap<>();
            transitions.put(fromState, t);
        }
        t.put(message, new Transition(toState, task, nextMessage));
    }
    
    private void setState(State state) {
        Object oldValue = getState();
        this.state = state;
        firePropertyChange("state", oldValue, state);
    }

    public State getState() {
        return state;
    }

    /**
     * Dump the FSM states to Graphviz format. It can be visualized using:
     * http://www.webgraphviz.com/ More information about the output format can be found at:
     * http://www.graphviz.org/content/dot-language
     * 
     * @return
     */
    public String toGraphviz() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph fsm {\n");
        for (Entry<State, Map<Message, Transition>> entry : transitions.entrySet()) {
            sb.append("  subgraph {\n");
            if (entry.getKey() == initialState) {
                sb.append("    rank=source;\n");
            }
            for (Entry<Message, Transition> t : entry.getValue().entrySet()) {
                sb.append(String.format("    %s -> %s [ label = %s ];\n", entry.getKey(),
                        t.getValue().toState, t.getKey()));
            }
            sb.append("  }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    public class Transition {
        public final State toState;
        public final Task task;
        public final Message nextMessage;

        public Transition(State toState, Task task, Message nextMessage) {
            this.toState = toState;
            this.task = task;
            this.nextMessage = nextMessage;
        }
    }

    public interface Task {
        void task() throws Exception;
    }
}
