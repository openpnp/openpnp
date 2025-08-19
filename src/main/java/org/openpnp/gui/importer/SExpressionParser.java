/*
 * Copyright (C) 2025 <jaytektas@github.com> 
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org> and Cri.S <phone.cri@gmail.com>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */
package org.openpnp.gui.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SExpressionParser {

    public static class Node {

        String token;
        List<String> values;
        List<Node> children;

        public Node(String token) {
            this.token = token;
            this.values = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        public void addChild(Node child) {
            this.children.add(child);
        }

        public void addValue(String value) {
            this.values.add(value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(token);
            for (String value : values) {
                sb.append(" ").append(value);
            }
            if (!children.isEmpty()) {
                for (Node child : children) {
                    sb.append(" ").append(child.toString());
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * Parses an S-expression string into a tree of Node objects.
     *
     * @param sExpression The S-expression string to parse.
     * @return A list of root Nodes of the parsed tree, or an empty list if the
     * input is invalid.
     */
    public static List<Node> parse(String sExpression) {
        List<Node> roots = new ArrayList<>();
        if (sExpression == null || sExpression.trim().isEmpty()) {
            return roots; // Return empty list for null or empty input
        }

        sExpression = sExpression.trim();
        Stack<Node> stack = new Stack<>();
        int i = 0;

        while (i < sExpression.length()) {
            char c = sExpression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '(') {
                i++;
                StringBuilder tokenBuilder = new StringBuilder();
                //handles multi-line tokens
                while (i < sExpression.length() && !Character.isWhitespace(sExpression.charAt(i)) && sExpression.charAt(i) != '(' && sExpression.charAt(i) != ')') {
                    tokenBuilder.append(sExpression.charAt(i));
                    i++;
                }
                String token = tokenBuilder.toString();
                Node newNode = new Node(token);
                if (!stack.isEmpty()) {
                    stack.peek().addChild(newNode);
                }
                stack.push(newNode);

            } else if (c == ')') {
                if (!stack.isEmpty()) {
                    Node completedNode = stack.pop();
                    if (stack.isEmpty()) {
                        roots.add(completedNode);
                    }
                } else {
                    i++; //handle  "()" as input.
                }
                i++;
            } else if (c == '"') {  //handle string literals
                i++;
                StringBuilder stringBuilder = new StringBuilder();
                boolean escaped = false;
                while (i < sExpression.length() && (sExpression.charAt(i) != '"' || escaped)) {
                    if (escaped) {
                        stringBuilder.append(sExpression.charAt(i));
                        escaped = false;
                    } else if (sExpression.charAt(i) == '\\') {
                        escaped = true;
                    } else {
                        stringBuilder.append(sExpression.charAt(i));
                    }
                    i++;
                }
                if (i < sExpression.length()) {
                    i++; // Consume the closing quote
                }
                String stringValue = stringBuilder.toString();
                if (!stack.isEmpty()) {
                    stack.peek().addValue(stringValue);
                }

            } else {
                StringBuilder valueBuilder = new StringBuilder();
                //handles multi-line values
                while (i < sExpression.length() && !Character.isWhitespace(c) && c != '(' && c != ')') {
                    valueBuilder.append(c);
                    i++;
                    if (i < sExpression.length()) {
                        c = sExpression.charAt(i);
                    }
                }
                String value = valueBuilder.toString();
                if (!stack.isEmpty()) {
                    stack.peek().addValue(value);
                }
            }
        }
        return roots;
    }

    /**
     * Searches for nodes with a specific token in the list of nodes.
     *
     * @param nodes The list of nodes to search within.
     * @param token The token to search for.
     * @return A list of nodes that match the given token.
     */
    public static List<Node> findNodesByToken(List<Node> nodes, String token) {
        List<Node> results = new ArrayList<>();
        for (Node node : nodes) {
            if (node.token.equals(token)) {
                results.add(node);
            }
        }
        return results;
    }

    /**
     * Searches for nodes with a specific token and value in the list of nodes.
     *
     * @param nodes The list of nodes to search in
     * @param token The token to search for
     * @param value The value to search for
     * @return A list of nodes that match the token and value
     */
    public static List<Node> findNodesByTokenAndValue(List<Node> nodes, String token, String value) {
        List<Node> results = new ArrayList<>();
        for (Node node : nodes) {
            if (node.token.equals(token) && node.values.contains(value)) {
                results.add(node);
            }
        }
        return results;
    }

    /**
     * Searches for nodes matching the given path string.
     *
     * @param nodes The list of root nodes to start the search from.
     * @param pathString A string representing the path, with tokens separated
     * by "/".
     * @return A list of nodes that match the given path.
     */
    public static List<Node> findNodesByPath(List<Node> nodes, String pathString) {
        List<Node> results = new ArrayList<>();
        if (nodes == null || nodes.isEmpty() || pathString == null || pathString.isEmpty()) {
            return results;
        }

        String[] path = pathString.split("/");
        if (path.length == 0) {
            return results;
        }
        findNodesByPathHelper(nodes, path, 0, results);
        return results;
    }

    private static void findNodesByPathHelper(List<Node> nodes, String[] path, int pathIndex, List<Node> results) {
        if (nodes == null || nodes.isEmpty() || path == null || path.length == 0 || pathIndex >= path.length) {
            return;
        }

        String targetToken = path[pathIndex];
        for (Node node : nodes) {
            if (node.token.equals(targetToken)) {
                if (pathIndex == path.length - 1) {
                    results.add(node); // Found a matching node at the end of the path
                } else {
                    // Continue searching in the children
                    findNodesByPathHelper(node.children, path, pathIndex + 1, results);
                }
            }
        }
    }

    /**
     * Searches for nodes with a specific value in the list of nodes.
     *
     * @param nodes The list of nodes to search within.
     * @param value The value to search for.
     * @return A list of nodes that contain the given value.
     */
    public static List<Node> findNodesByValue(List<Node> nodes, String value) {
        List<Node> results = new ArrayList<>();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node.values.contains(value)) {
                    results.add(node);
                }
            }
        }
        return results;
    }

    public static List<String> getValuesByPath(List<Node> nodes, String pathString) {
        List<SExpressionParser.Node> result = findNodesByPath(nodes, pathString);
        if (!result.isEmpty()) {
            return result.get(0).values;
        }
        return new ArrayList<String>();
    }

    public static String getValueByPath(List<Node> nodes, String pathString) {
        List<String> result = getValuesByPath(nodes, pathString);
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return new String();
    }

    /**
     * Recursively searches for nodes matching the given path string and value.
     *
     * @param nodes The list of root nodes to start the search from.
     * @param pathString The path to search for, with tokens separated by "/".
     * @param value The value that the final node in the path should have.
     * @return A list of nodes that match the path and value.
     */
    public static List<Node> findNodesByPathAndValue(List<Node> nodes, String pathString, String value) {
        List<Node> results = new ArrayList<>();
        if (nodes == null || nodes.isEmpty() || pathString == null || pathString.isEmpty()) {
            return results;
        }

        String[] path = pathString.split("/");
        if (path.length == 0) {
            return results;
        }
        findNodesByPathAndValueHelper(nodes, path, 0, value, results);
        return results;
    }

    private static void findNodesByPathAndValueHelper(List<Node> nodes, String[] path, int pathIndex, String targetValue, List<Node> results) {
        if (nodes == null || nodes.isEmpty() || path == null || path.length == 0 || pathIndex >= path.length) {
            return;
        }

        String targetToken = path[pathIndex];
        for (Node node : nodes) {
            if (node.token.equals(targetToken)) {
                if (pathIndex == path.length - 1) {
                    if (node.values.contains(targetValue)) {
                        results.add(node); // Found matching node at the end of path with correct value.
                    }
                } else {
                    // Continue searching in the children
                    findNodesByPathAndValueHelper(node.children, path, pathIndex + 1, targetValue, results);
                }
            }
        }
    }

    private static void testParsing(String input) {
        System.out.println("Input:\n\"" + input + "\"");
        List<Node> roots = parse(input);
        if (roots != null && !roots.isEmpty()) {
            System.out.println("Parsed Tree(s):");
            for (Node root : roots) {
                System.out.println(root);
            }

        } else {
            System.out.println("Invalid S-expression.");
        }
        System.out.println("-------------------");
    }

    public static void test() {
        String sExpression1 = "(a b c)";
        String sExpression2 = "(a (b c) (d e))";
        String sExpression3 = "(a b (c d e))";
        String sExpression4 = "(a)";
        String sExpression5 = "()";
        String sExpression6 = "(a (b))";
        String sExpression7 = "(a (b c))";
        String sExpression8 = "(a (b c) d)";
        String sExpression9 = "(a (b (c d)))";
        String sExpression10 = "(a (b (c d) e) f)";
        String sExpression11 = "(a b c d)";
        String sExpression12 = "(a (b c) (d e f g))";
        String sExpression13 = "(a (b c) d e f)";
        String sExpression14 = "(a (b (c d) e) f g h)";
        String sExpression15 = "(a)";
        String sExpression16 = "()";
        String sExpression17 = "(a b)";
        String sExpression18 = "(a (b c))";
        String sExpression19 = "(a (b c) d)";
        String sExpression20 = "(a (b (c d)))";
        String sExpression21 = "(a (b (c d) e) f)";
        String sExpression22 = "(a b c d)";
        String sExpression23 = "(a (b c) (d e f g))";
        String sExpression24 = "(a (b c) d e f)";
        String sExpression25 = "(a (b (c d) e) f g h)";
        String sExpression26 = "(a (b (c d) e) f g h i j k l m n o p q r s t u v w x y z))";

        String sExpression27 = "(a\n  b\n  c)"; //multi line
        String sExpression28 = "(a\n  (b\n    c\n  )\n  (d\n    e\n  )\n)";  //multiline children
        String sExpression29 = "(\n  a\n  b\n  (\n    c\n    d\n  )\n)"; //mixed
        String sExpression30 = "(a \"hello world\")"; //string literal
        String sExpression31 = "(a \"hello\\\"world\\\"\")"; //escaped quotes
        String sExpression32 = "(a \"hello\\\\world\")"; //escaped backslash
        String sExpression33 = "(a \"hello\\nworld\")"; //escaped newline
        String sExpression34 = "(a \"\")"; //empty string
        String sExpression35 = "(a \"\\\"\")"; //escaped double quote only
        String sExpression36 = "(a \"b c d\")";
        String sExpression37 = "(a \"b\\\"c\\\"d\")";
        String sExpression38 = "(a \"b\\\\c\\\\d\")";
        String sExpression39 = "(a \"b\\nc\\nd\")";
        String sExpression40 = "(a \"\"\"\")"; // consecutive quotes
        String sExpression41 = "(a \"\\\"abc\")"; // quote at start
        String sExpression42 = "(a \"abc\\\"\")"; // quote at end

        String sExpression43 = "(find a 1)";
        String sExpression44 = "(find b 2)";
        String sExpression45 = "(process (input 10) (config \"value\"))";
        String sExpression46 = "(a (b c (d e f)))";
        String sExpression47 = "(a (b c) (d e))";
        String sExpression48 = "(x (y z \"test\"))";

        testParsing(sExpression1);
        testParsing(sExpression2);
        testParsing(sExpression3);
        testParsing(sExpression4);
        testParsing(sExpression5);
        testParsing(sExpression6);
        testParsing(sExpression7);
        testParsing(sExpression8);
        testParsing(sExpression9);
        testParsing(sExpression10);
        testParsing(sExpression11);
        testParsing(sExpression12);
        testParsing(sExpression13);
        testParsing(sExpression14);
        testParsing(sExpression15);
        testParsing(sExpression16);
        testParsing(sExpression17);
        testParsing(sExpression18);
        testParsing(sExpression19);
        testParsing(sExpression20);
        testParsing(sExpression21);
        testParsing(sExpression22);
        testParsing(sExpression23);
        testParsing(sExpression24);
        testParsing(sExpression25);
        testParsing(sExpression26);
        testParsing(sExpression27);
        testParsing(sExpression28);
        testParsing(sExpression29);
        testParsing(sExpression30);
        testParsing(sExpression31);
        testParsing(sExpression32);
        testParsing(sExpression33);
        testParsing(sExpression34);
        testParsing(sExpression35);
        testParsing(sExpression36);
        testParsing(sExpression37);
        testParsing(sExpression38);
        testParsing(sExpression39);
        testParsing(sExpression40);
        testParsing(sExpression41);
        testParsing(sExpression42);
        testParsing(sExpression43);
        testParsing(sExpression44);
        testParsing(sExpression45);
        testParsing(sExpression46);
        testParsing(sExpression47);
        testParsing(sExpression48);

        System.out.println("\n--- Search by Token ---");
        List<Node> parsedList1 = parse(sExpression43);
        List<Node> foundNodes = findNodesByToken(parsedList1, "find");
        System.out.println("Nodes with token 'find': " + foundNodes);

        List<Node> parsedList2 = parse(sExpression45);
        List<Node> foundNodes2 = findNodesByToken(parsedList2, "input");
        System.out.println("Nodes with token 'input': " + foundNodes2);

        System.out.println("\n--- Search by Token and Value ---");
        List<Node> parsedList3 = parse(sExpression43);
        List<Node> foundNodes3 = findNodesByTokenAndValue(parsedList3, "find", "a");
        System.out.println("Nodes with token 'find' and value 'a': " + foundNodes3);

        List<Node> parsedList4 = parse(sExpression45);
        List<Node> foundNodes4 = findNodesByTokenAndValue(parsedList4, "process", "input");
        System.out.println("Nodes with token 'process' and value 'input': " + foundNodes4);

        System.out.println("\n--- Search by Path ---");
        List<Node> parsedTree1 = parse(sExpression46);
        List<Node> foundNodesByPath1 = findNodesByPath(parsedTree1, "a/b/c");
        System.out.println("Nodes at path 'a/b/c': " + foundNodesByPath1);

        List<Node> parsedTree2 = parse(sExpression46);
        List<Node> foundNodesByPath2 = findNodesByPath(parsedTree2, "a/b/d");
        System.out.println("Nodes at path 'a/b/d': " + foundNodesByPath2);

        List<Node> parsedTree3 = parse(sExpression46);
        List<Node> foundNodesByPath3 = findNodesByPath(parsedTree3, "a");
        System.out.println("Nodes at path 'a': " + foundNodesByPath3);

        List<Node> parsedTree4 = parse(sExpression47);
        List<Node> foundNodesByPath4 = findNodesByPath(parsedTree4, "a/d");
        System.out.println("Nodes at path 'a/d': " + foundNodesByPath4);

        System.out.println("\n--- Search by Value ---");
        List<Node> parsedList5 = parse(sExpression43);
        List<Node> foundNodesByValue1 = findNodesByValue(parsedList5, "1");
        System.out.println("Nodes with value '1': " + foundNodesByValue1);

        List<Node> parsedList6 = parse(sExpression45);
        List<Node> foundNodesByValue2 = findNodesByValue(parsedList6, "value");
        System.out.println("Nodes with value 'value': " + foundNodesByValue2);

        System.out.println("\n--- Search by Path and Value ---");
        List<Node> parsedTree5 = parse(sExpression48);
        List<Node> foundNodesByPathAndValue1 = findNodesByPathAndValue(parsedTree5, "x/y/z", "test");
        System.out.println("Nodes at path 'x/y/z' with value 'test': " + foundNodesByPathAndValue1);

        List<Node> parsedTree6 = parse(sExpression46);
        List<Node> foundNodesByPathAndValue2 = findNodesByPathAndValue(parsedTree6, "a/b/c", "e");
        System.out.println("Nodes at path 'a/b/c' with value 'e': " + foundNodesByPathAndValue2);
    }
}
