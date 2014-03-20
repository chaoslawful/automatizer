package automatizer.core.models;

import com.etao.lz.automaton.State;

public class StateData {

    State state;
    boolean init;

    public StateData(State st, boolean isInit) {
        state = st;
        init = isInit;
    }

    public State getState() {
        return state;
    }

    public boolean isInit() {
        return init;
    }

    public boolean isAccept() {
        return state.isAccept();
    }

    public String getLabel() {
        return String.valueOf(state.getNumber());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return state.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateData other = (StateData) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }

}
