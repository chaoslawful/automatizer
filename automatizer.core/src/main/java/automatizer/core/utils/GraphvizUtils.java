package automatizer.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import com.etao.lz.automaton.Automaton;
import com.etao.lz.automaton.State;
import com.etao.lz.automaton.StatePair;
import com.etao.lz.automaton.Transition;

public class GraphvizUtils {

    /**
     * 将给定的 Graphviz DOT 格式描述的状态机转移图转换为 Automaton 对象，DOT 数据要求为:
     * <ul>
     * <li>initial 状态: 用名为 initial 的结点指向某个节点即表示目标结点是 initial 状态</li>
     * <li>accept 状态: 结点的 shape 属性设置为 doublecircle 即表示其为 accept 状态</li>
     * <li>转移边条件: 以 edge 的 label 属性为转移条件，可以是单个裸字符或 \\uXXXX 的转义序列，或是 ch1-ch2
     * 形式的字符范围，label 为空时表示 epsilon 转移边</li>
     * </ul>
     * 
     * @param sb
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Automaton dotToFsa(StringBuffer sb) throws Exception {
        Parser p = new Parser();
        p.parse(sb);
        ArrayList<Graph> graphs = p.getGraphs();

        if (graphs.size() != 1) {
            throw new RuntimeException("Can't handle more than 1 graphs");
        }

        Graph g = graphs.get(0);
        ArrayList<Edge> edges = g.getEdges();
        Map<String, State> nodeMappings = new HashMap<String, State>();
        Node initNode = null;
        List<StatePair> epsEdges = new ArrayList<StatePair>();
        for (Edge edge : edges) {
            Node srcNode = edge.getSource().getNode();
            boolean srcAccept = false;
            if ("doublecircle".equalsIgnoreCase(srcNode.getAttribute("shape"))) {
                srcAccept = true;
            }
            Node dstNode = edge.getTarget().getNode();
            boolean dstAccept = false;
            if ("doublecircle".equalsIgnoreCase(dstNode.getAttribute("shape"))) {
                dstAccept = true;
            }

            if (!"initial".equalsIgnoreCase(srcNode.getId().getId())) {
                State srcSt = nodeMappings.get(srcNode.getId().getId());
                State dstSt = nodeMappings.get(dstNode.getId().getId());
                String label = edge.getAttribute("label");

                if (srcSt == null) {
                    srcSt = new State();
                    nodeMappings.put(srcNode.getId().getId(), srcSt);
                }
                srcSt.setAccept(srcAccept);

                if (dstSt == null) {
                    dstSt = new State();
                    nodeMappings.put(dstNode.getId().getId(), dstSt);
                }
                dstSt.setAccept(dstAccept);

                Object edgeLabel = parseEdgeLabel(label);
                if (edgeLabel == null
                        || ((edgeLabel instanceof Set) && ((Set) edgeLabel)
                                .size() == 0)) {
                    // 无边标签，增加 epsilon 边
                    StatePair sp = new StatePair(srcSt, dstSt);
                    epsEdges.add(sp);
                } else {
                    // 有边标签，增加普通边
                    if (edgeLabel instanceof Character) {
                        // 单字符 ch
                        char ch = (Character) edgeLabel;
                        Transition tran = new Transition(ch, dstSt);
                        srcSt.addTransition(tran);
                    } else if (edgeLabel instanceof Set) {
                        // 有限字符集 [...]
                        Set<Character> chs = (Set<Character>) edgeLabel;
                        for (char ch : chs) {
                            Transition tran = new Transition(ch, dstSt);
                            srcSt.addTransition(tran);
                        }
                    } else {
                        // 连续字符范围 ch1-ch2
                        char[] chs = (char[]) edgeLabel;
                        Transition tran = new Transition(chs[0], chs[1], dstSt);
                        srcSt.addTransition(tran);
                    }
                }
            } else {
                // 找到了 init 状态
                initNode = dstNode;
                if (!nodeMappings.containsKey(dstNode.getId().getId())) {
                    nodeMappings.put(dstNode.getId().getId(), new State());
                }
            }
        }

        // 构建 FSA
        Automaton atm = new Automaton();
        atm.setInitialState(nodeMappings.get(initNode.getId().getId()));
        atm.addEpsilons(epsEdges);
        Automaton.setStateNumbers(atm.getStates());

        return atm;
    }

    /**
     * 解析 DOT 边上的转移规则，形如:
     * 
     * <ul>
     * <li>ch - 单字符形式</li>
     * <li>ch1-ch2 - 字符范围形式</li>
     * <li>[...] - 有限字符集形式（仅允许出现可见字符）</li>
     * </ul>
     * 
     * 所有形式中 ch 如果是可见字符就是原样显示，非可见字符则是 \\uXXXX 转义序列
     * 
     * @param label
     * @return
     */
    private static final Object parseEdgeLabel(String label) {
        Object res = null;

        if (label != null) {
            if (label.contains("-")) {
                // min-max 形式
                String[] parts = label.split("-", 2);
                char[] chs = new char[2];
                chs[0] = parseEscapedChar(parts[0]);
                chs[1] = parseEscapedChar(parts[1]);
                res = chs;
            } else if (label.length() == 1) {
                // ch 形式
                char ch = parseEscapedChar(label);
                res = ch;
            } else if (label.startsWith("[") && label.endsWith("]")) {
                // [...] 形式
                Set<Character> chs = new TreeSet<Character>();
                for (char ch : label.substring(1, label.length() - 1)
                        .toCharArray()) {
                    chs.add(ch);
                }
                res = chs;
            }
        }

        return res;
    }

    private static final char parseEscapedChar(String s) {
        if (s.isEmpty()) {
            return 0;
        }

        // 字面字符
        if (s.length() == 1) {
            return s.charAt(0);
        }

        // Unicode 转义序列
        if (s.charAt(0) != '\\' || (s.charAt(1) != 'u' && s.charAt(1) != 'U')) {
            throw new RuntimeException("Unknown char escape sequence: " + s);
        }
        int codePoint = Integer.parseInt(s.substring(2), 16);
        return (char) codePoint;
    }

    public static void main(String[] args) throws Exception {
        String s = "digraph Automaton {\n" + "  rankdir = LR;\n"
                + "  0 [shape=circle,label=\"0:\"];\n"
                + "  0 -> 2 [label=\"[abc]\"]\n"
                + "  1 [shape=doublecircle,label=\"1: \"];\n"
                + "  1 -> 2 [label=\"b\"]\n"
                + "  2 [shape=doublecircle,label=\"2: \"];\n"
                + "  3 [shape=circle,label=\"3:\"];\n"
                + "  3 -> 0 [label=\"c\"]\n" + "  3 -> 0 [label=\"t\"]\n"
                + "  3 -> 1 [label=\"b\"]\n"
                + "  4 [shape=circle,label=\"4:\"];\n"
                + "  initial [shape=plaintext,label=\"\"];\n"
                + "  initial -> 4\n" + "  4 -> 3 [label=\"a\"]\n" + "}";
        Automaton atm = dotToFsa(new StringBuffer(s));
        System.out.println(atm.toDot());
    }

}
