import java.util.*;

public class BoardGraph {
    public final int N = 64;

    public final int[][] adjMatrix; // adjacency matrix input
    public final Map<Integer, Set<Integer>> adjList; // for BFS shortest path
    public final List<int[]> randomLinks; // 5 random links {a,b}

    public BoardGraph() {
        adjMatrix = new int[N + 1][N + 1];
        adjList = new HashMap<>();
        for (int i = 1; i <= N; i++) adjList.put(i, new LinkedHashSet<>());

        // default edges: i <-> i+1
        for (int i = 1; i < N; i++) addEdge(i, i + 1);

        randomLinks = new ArrayList<>();
        addFiveRandomLinks();
    }

    public void addEdge(int a, int b) {
        if (a < 1 || a > N || b < 1 || b > N || a == b) return;
        adjMatrix[a][b] = 1;
        adjMatrix[b][a] = 1;
        adjList.get(a).add(b);
        adjList.get(b).add(a);
    }

    public void addFiveRandomLinks() {
        Random rnd = new Random();
        int attempts = 0;

        while (randomLinks.size() < 5 && attempts < 10_000) {
            attempts++;
            int a = 1 + rnd.nextInt(N);
            int b = 1 + rnd.nextInt(N);
            if (a == b) continue;
            if (Math.abs(a - b) == 1) continue; // biar nggak membosankan (optional)

            int min = Math.min(a, b), max = Math.max(a, b);
            boolean already = false;
            for (int[] p : randomLinks) {
                int pmin = Math.min(p[0], p[1]);
                int pmax = Math.max(p[0], p[1]);
                if (pmin == min && pmax == max) { already = true; break; }
            }
            if (already) continue;

            addEdge(a, b);
            randomLinks.add(new int[]{a, b});
        }
    }

    /** BFS shortest path (unweighted) */
    public List<Integer> shortestPath(int start, int target) {
        if (start == target) return List.of(start);

        boolean[] vis = new boolean[N + 1];
        int[] parent = new int[N + 1];
        Arrays.fill(parent, -1);

        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(start);
        vis[start] = true;

        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adjList.get(u)) {
                if (!vis[v]) {
                    vis[v] = true;
                    parent[v] = u;
                    if (v == target) return reconstruct(parent, start, target);
                    q.add(v);
                }
            }
        }
        return List.of();
    }

    private List<Integer> reconstruct(int[] parent, int start, int target) {
        LinkedList<Integer> path = new LinkedList<>();
        int cur = target;
        while (cur != -1) {
            path.addFirst(cur);
            if (cur == start) break;
            cur = parent[cur];
        }
        if (path.isEmpty() || path.getFirst() != start) return List.of();
        return path;
    }

    public static boolean isPrime(int x) {
        if (x < 2) return false;
        if (x == 2) return true;
        if (x % 2 == 0) return false;
        for (int i = 3; i * i <= x; i += 2) {
            if (x % i == 0) return false;
        }
        return true;
    }
}
