package org.openpnp.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FiniteStateMachine<State, Message, TaskReturnType> {
    private final State initialState;
    private State state;

    private Map<State, Map<Message, Transition>> transitions = new HashMap<>();

    public FiniteStateMachine(State initialState) {
        this.initialState = initialState;
        this.state = initialState;
    }

    public TaskReturnType send(Message message) throws Exception {
        State state = getState();
        Map<Message, Transition> transitions = this.transitions.get(state);
        if (transitions == null) {
            throw new Exception("No defined transitions from " + state);
        }
        Transition transition = transitions.get(message);
        if (transition == null) {
            throw new Exception("No defined transitions from " + state + " for " + message);
        }
        System.out.println(message + " => " + state + " -> " + transition.toState);
        TaskReturnType ret = transition.task.task();
        this.state = transition.toState;
        return ret;
    }

    public void add(State fromState, Message message, State toState, Task task) {
        Map<Message, Transition> t = transitions.get(fromState);
        if (t == null) {
            t = new HashMap<>();
            transitions.put(fromState, t);
        }
        t.put(message, new Transition(toState, task));
    }

    public State getState() {
        return state;
    }
    
    /**
     * Dump the FSM states to Graphviz format. It can be visualized using:
     * http://www.webgraphviz.com/
     * More information about the output format can be found at:
     * http://www.graphviz.org/content/dot-language
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
                sb.append(String.format("    %s -> %s [ label = %s ];\n", entry.getKey(), t.getValue().toState, t.getKey()));
            }
            sb.append("  }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    public class Transition {
        public final State toState;
        public final Task<TaskReturnType> task;

        public Transition(State toState, Task task) {
            this.toState = toState;
            this.task = task;
        }
    }

    public interface Task<T> {
        T task() throws Exception;
    }
}
