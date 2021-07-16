package com.jdt.fedlearn.core.research.mpc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.jdt.fedlearn.core.exception.WrongValueException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueryAuditor {

    private final List<Query> historicalQuery = new ArrayList<>(); // separated queries in history.

    // a map between substituted old vars and new vars. Key=new var, value=old var
    private final BiMap<String, String> varMap = HashBiMap.create(1024);
    // a map between its user-defined original name and new name;'
    private final Map<String, String> atomicVarMap = new HashMap<>();

    private final Set<String> atomicVarNames = new HashSet<>();

    private List<List<Query>> lastQueryRes = new ArrayList<>();
    private int queryCnt = 0;
    private int varCnt = 0;

    public QueryAuditor(String[] originalVarNames) {
        for(String elem: originalVarNames) {
            String newName = String.valueOf(varCnt);
            atomicVarMap.put(elem, newName);
            atomicVarMap.put(newName, elem);
            atomicVarNames.add(newName);
            varCnt += 1;
        }
    }

    /**
     * Split a query String by "+".
     * Before splitting all "-" are replaced by "+-", all vars with scalar are expanded.
     *
     * @return substring arrays representing each "unique" var.
     */
    private Query splitQueryString(String inStr) {
        String[] rawQuery =  inStr.replace("-", "+-").split("\\+");
        Query q = new Query(rawQuery);
        return renameAtomicVar(q);
    }

    /**
     * Replace vars in a query by Integer Strings. Then add into <code>varMap</code>
     *
     * @param inQuery input query
     * @return same query with updated var names.
     */
    private Query renameAtomicVar(Query inQuery) {
        for(int i = 0; i < inQuery.getLen(); i++) {
            String oldVarName = inQuery.getQuery().get(i);
            String newVarName = atomicVarMap.get(oldVarName);
            if(newVarName!=null) {
                inQuery.resetQuery(i, newVarName);
            } else {
                newVarName = String.valueOf(varCnt);
                inQuery.resetQuery(i, newVarName);
                atomicVarMap.put(oldVarName, newVarName);
                atomicVarMap.put(newVarName, oldVarName);
                varCnt += 1;
            }
        }
        return inQuery;
    }

    /**
     * Find Longest Common Sub-string
     * Time Complexity = O(m+n). Space = O(m+n)
     */
    private Map<String, Integer> findLongestCommonVarSet(LinkedList<String> a, LinkedList<String> b) {
        if((a.size()==0) || b.size()==0 ) {
            throw new WrongValueException("input a or b is empty.");
        }
        List<String> commonVar = new ArrayList<>(Sets.intersection(new HashSet<>(a), new HashSet<>(b)));
        Map<String, Integer> varCnt = new HashMap<>();
        int[] commonVarCnt = commonVar.stream().mapToInt(x -> varCntHelper(a, b, x)).toArray();
        for(int i = 0; i < commonVar.size(); i++) {
            varCnt.put(commonVar.get(i), commonVarCnt[i]);
        }
        return varCnt;
    }
    private int varCntHelper(LinkedList<String> a, LinkedList<String> b, String var) {
        int idxA = a.indexOf(var);
        int idxB = b.indexOf(var);
        int cnt = 0;
        while((idxA<a.size()) && (a.get(idxA).equals(var)) && (idxB<b.size()) && (b.get(idxB).equals(var))) {
            cnt += 1;
            idxA += 1;
            idxB += 1;
        }
        return cnt;
    }

    private void atomicVarSubstitute(LinkedList<String> a, LinkedList<String> b) {
        String newVar;
        Map<String, Integer> commonVarCnt = findLongestCommonVarSet(a, b);

        while(commonVarCnt.keySet().size() >= 2) {
            LinkedList<String> oldVars = new LinkedList<>();
            for (Map.Entry<String, Integer> eachVar : commonVarCnt.entrySet()) {
                int cnt = commonVarCnt.get(eachVar.getKey());
                while (cnt > 0) {
                    oldVars.add(eachVar.getKey());
                    cnt -= 1;
                }
            }
            if (oldVars.size() < 2) {
                break;
            }

            Collections.sort(oldVars);
            String oldVarStr = list2VarStr(oldVars);
            if (varMap.containsValue(oldVarStr)) {
                newVar = varMap.inverse().get(oldVarStr);
            } else {
                newVar = String.valueOf(varCnt++);
                varMap.put(newVar, oldVarStr);
            }
            for(String elem : oldVars) {
                a.remove(elem);
                b.remove(elem);
            }
            a.add(newVar);
            b.add(newVar);
            commonVarCnt = findLongestCommonVarSet(a, b);
        }
    }

    private List<LinkedList<String>> splitAtomicVar(LinkedList<String> a) {
        List<LinkedList<String>> ret = new ArrayList<>();
        LinkedList<String> retTmp1 = new LinkedList<>(a);
        retTmp1.retainAll(atomicVarNames);
        LinkedList<String> retTmp2 = new LinkedList<>(a);
        retTmp2.removeAll(atomicVarNames);
        ret.add(retTmp1);
        ret.add(retTmp2);
        return ret;
    }

    /**
     * Find longest common strings between two queries and substitute them with new variables.
     *
     * @param b query contains only atomic vars
     * @return substring arrays representing each "unique" var.
     */
    private List<LinkedList<String>> twoQuerySubstitute(LinkedList<String> a, LinkedList<String> b) {
        assert atomicVarNames.containsAll(new HashSet<>(b));
        List<LinkedList<String>> ret = new ArrayList<>();
        // split atomic
        List<LinkedList<String>> tmp = splitAtomicVar(a);
        LinkedList<String> aAtomic = tmp.get(0);
        LinkedList<String> aSimplified = tmp.get(1);

        for(String eachSimplifiedVar : aSimplified) {
            // TODO: switch to iteration
            twoQuerySubstitute((LinkedList<String>) varStr2List(varMap.get(eachSimplifiedVar)), b);
        }
        atomicVarSubstitute(aAtomic, b);
        ret.add(aAtomic);
        ret.add(b);
        return ret;
    }

    /**
     * Do substitution between a and each query in qSet.
     *
     * @param qSet set of queries
     * @param a    one query
     * @return List of substituted qSet and a. a is at the tail.
     */
    private List<Query> setQuerySubstitute(List<Query> qSet, Query a) {
        List<Query> res = new ArrayList<>();
        for(Query q : qSet) {
            List<LinkedList<String>> tmp = twoQuerySubstitute(q.getQuery(), a.getQuery());
            res.add(new Query(tmp.get(0)));
        }
        return res;
    }

    /**
     * Reject a query or not.
     * Reject if number of equation larger then number of vars AND if one var contains only a single original variable.
     *
     * @param queryList List of simplified queries from ONE set of equations.
     * @return True if reject, False if permit.
     */
    private boolean rejectQuery(List<Query> queryList) {
        // count var number
        Set<String> cntSet = new HashSet<>();
        for(Query q: queryList) {
            cntSet.addAll(q.getQuery());
        }
        return (cntSet.size() <= queryList.size()) && Sets.intersection(cntSet, atomicVarNames).size() > 0;
    }

    /**
     * For each incoming query, return reject or not.
     * @param inStr a query in String type.
     * @return if this query should be rejected.
     */
    public boolean auditOneIncomingQuery(String inStr) {
        assert historicalQuery.size() == queryCnt;

        if(lastQueryRes.size()==0) {
            Query[] tmp = new Query[]{splitQueryString(inStr)};
            lastQueryRes.add(Arrays.asList(tmp));
            return false;
        }

        Query inQuery = splitQueryString(inStr);
        List<List<Query>> simplified = lastQueryRes.stream()
                .map(q -> setQuerySubstitute(q, inQuery)).collect(Collectors.toList());
        historicalQuery.add(inQuery);
        lastQueryRes = simplified;
        queryCnt += 1;

        for (List<Query> eachSimplified : simplified) {
            if (rejectQuery(eachSimplified)) {
                return true;
            }
        }
        return false;
    }

    private static String list2VarStr(LinkedList<String> in) {
        if(in.size()==0) {
            throw new WrongValueException("input size is 0");
        }
        assert Ordering.natural().isOrdered(in);
        StringBuilder string = new StringBuilder();
//        Collections.sort(in);
        Iterator<?> it = in.descendingIterator();
        while (it.hasNext()) {
            string.append(it.next());
            string.append(",");
        }
        return string.toString();
    }

    private static LinkedList<String> varStr2List(String in) {
        String[] strLst =  in.split(",");
        if(strLst.length==0) {
            throw new WrongValueException("input size is 0");
        }
        return new LinkedList<>(Arrays.asList(strLst));
    }

    public static void main(String[] args) {
        int eqLen = 100;
        int numVar = 10;
        int queryNum = 100;
        String [] allVars = IntStream.range(0, numVar).mapToObj(x -> "a"+x).toArray(String[]::new);
        Random rand = new Random();

        for(int j = 0; j < 100000; j++) {
            StringBuilder eqStr = new StringBuilder(allVars[rand.nextInt(allVars.length)]);
            for (int i = 0; i < eqLen - 1; i++) {
                eqStr.append("+").append(allVars[rand.nextInt(allVars.length)]);
            }
            LinkedList<String> rawQuery = new LinkedList<>(Arrays.asList(eqStr.toString().replace("-", "+-").split("\\+")));
            Collections.sort(rawQuery);
            LinkedList<String> res = varStr2List(list2VarStr(rawQuery));
            for(int i = 0; i < res.size(); i++) {
                assert res.get(i).equals(rawQuery.get(i));
            }
        }

        for(int j = 0; j < queryNum; j++) {
            StringBuilder eqStr = new StringBuilder(allVars[rand.nextInt(allVars.length)]);
            for (int i = 0; i < eqLen - 1; i++) {
                eqStr.append("+").append(allVars[rand.nextInt(allVars.length)]);
            }
            }
        }
}

class Query {
    private LinkedList<String> query;
    private int len;

    public Query(String[] in) {
        if( (in == null) || (in.length == 0)) {
            throw new WrongValueException("Raw query is null or has size 0.");
        }
        this.query = new LinkedList<>(Arrays.asList(in));
        Collections.sort(query);
        this.len = in.length;
    }

    public Query(LinkedList<String> in) {
        if( (in == null) || (in.size() == 0)) {
            throw new WrongValueException("Raw query is null or has size 0.");
        }
        this.query = new LinkedList<>(in);
        Collections.sort(query);
        this.len = in.size();
    }

    public LinkedList<String> getQuery() {
        return query;
    }

    public int getLen() {
        return len;
    }

    public void resetQuery(int pos, String val) {
        if(pos < 0 || pos >= this.len || "".equals(val)) {
            throw new WrongValueException("index is illegal.");
        }
        assert query.size() == this.len;

        query.set(pos, val);
        Collections.sort(query);

    }
    public void resetAll(Set<String> in) {
        if(in.size() == 0) {
            throw new WrongValueException("index is illegal.");
        }
        this.query = new LinkedList<>(in);
        Collections.sort(query);

        this.len = query.size();
    }
}



