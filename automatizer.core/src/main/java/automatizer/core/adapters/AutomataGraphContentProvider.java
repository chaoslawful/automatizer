package automatizer.core.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphContentProvider;

import automatizer.core.models.ConnectionData;
import automatizer.core.models.StateData;

import com.etao.lz.automaton.Automaton;
import com.etao.lz.automaton.State;
import com.etao.lz.automaton.Transition;

/**
 * 供 Zest 使用的 Automaton 向 graph model 转换适配器
 * 
 * @author wxz
 * 
 */
public class AutomataGraphContentProvider implements IGraphContentProvider {

    final static Object[] EMPTY_ARRAY = new Object[] {};

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        List<Object> results = new ArrayList<Object>();
        if (inputElement instanceof Automaton) {
            Automaton atm = (Automaton) inputElement;
            State initState = atm.getInitialState();
            Set<State> states = atm.getStates();
            for (State srcState : states) {
                List<Transition> trans = srcState.getSortedTransitions(false);
                for (Transition tran : trans) {
                    results.add(new ConnectionData(new StateData(srcState,
                            srcState == initState), new StateData(tran
                            .getDest(), tran.getDest() == initState), tran));
                }
            }
            return results.toArray();
        }
        return EMPTY_ARRAY;
    }

    @Override
    public Object getSource(Object rel) {
        return ((ConnectionData) rel).getSource();
    }

    @Override
    public Object getDestination(Object rel) {
        return ((ConnectionData) rel).getTarget();
    }

}
