package automatizer.core.models;

import com.etao.lz.automaton.Transition;

/**
 * 用于封装 Automaton 转移边和来源状态的 model
 * 
 * @author wxz
 * 
 */
public class ConnectionData {

    StateData source;
    StateData target;
    Transition transition;

    public ConnectionData(StateData src, StateData dst, Transition trans) {
        source = src;
        target = dst;
        transition = trans;
    }

    public StateData getSource() {
        return source;
    }

    public StateData getTarget() {
        return target;
    }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        char min = transition.getMin();
        char max = transition.getMax();
        appendCharString(min, sb);
        if (min != max) {
            sb.append("-");
            appendCharString(max, sb);
        }
        return sb.toString();
    }

    public Transition getTransition() {
        return transition;
    }

    static void appendCharString(char c, StringBuilder sb) {
        if (c >= 0x21 && c <= 0x7e && c != '\\' && c != '"')
            sb.append(c);
        else {
            sb.append("\\u");
            String s = Integer.toHexString(c);
            if (c < 0x10)
                sb.append("000").append(s);
            else if (c < 0x100)
                sb.append("00").append(s);
            else if (c < 0x1000)
                sb.append("0").append(s);
            else
                sb.append(s);
        }
    }

}
