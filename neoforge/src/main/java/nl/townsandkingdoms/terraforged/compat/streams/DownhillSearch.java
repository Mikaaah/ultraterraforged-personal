package nl.townsandkingdoms.terraforged.compat.streams;

import java.util.*;

/** Bounded search through connected water. Never infers slope from a generator's predicted river route. */
public final class DownhillSearch {
    public record Surface(int y, double height) {}
    public interface Field { Surface sample(int x, int z, int fromY); }
    public record Result(double x, double z, boolean outlet, int visited) {}
    private record Node(int x, int z, Surface surface, int depth, int firstX, int firstZ) {}
    private static final int[][] DIRS = {{1,0},{0,1},{-1,0},{0,-1},{1,1},{-1,1},{-1,-1},{1,-1}};
    private static long key(int x,int z) { return (long)x << 32 ^ (z & 0xffffffffL); }
    public static Result find(Field field, int x, int z, Surface start, double dx, double dz, int radius, int budget) {
        var queue = new ArrayDeque<Node>(); var seen = new HashSet<Long>();
        queue.add(new Node(x,z,start,0,0,0)); seen.add(key(x,z));
        int bestDepth = Integer.MAX_VALUE, visits = 0; double bestScore = -Double.MAX_VALUE;
        Node best = null;
        while (!queue.isEmpty() && visits < budget) {
            Node node = queue.remove(); visits++;
            if (node.depth > bestDepth) break;
            if (node.surface.height < start.height - 0.125) {
                double len = Math.hypot(node.firstX,node.firstZ);
                double score = (node.firstX*dx + node.firstZ*dz) / len;
                if (score > bestScore) { bestScore=score;best=node;bestDepth=node.depth; }
                continue;
            }
            if (node.depth >= radius || best != null) continue;
            for (int[] d : DIRS) {
                int nx=node.x+d[0], nz=node.z+d[1];
                if (Math.abs(nx-x)>radius || Math.abs(nz-z)>radius || seen.contains(key(nx,nz))) continue;
                Surface next=field.sample(nx,nz,node.surface.y);
                if (next==null || next.height > node.surface.height+0.125) continue;
                if (d[0]!=0 && d[1]!=0) {
                    Surface ax=field.sample(node.x+d[0],node.z,node.surface.y);
                    Surface az=field.sample(node.x,node.z+d[1],node.surface.y);
                    // Both orthogonal passages must be open; never cut diagonally through a bank.
                    if (ax==null || az==null || ax.height>node.surface.height+0.125 || az.height>node.surface.height+0.125) continue;
                }
                seen.add(key(nx,nz));
                queue.add(new Node(nx,nz,next,node.depth+1,node.depth==0?d[0]:node.firstX,node.depth==0?d[1]:node.firstZ));
            }
        }
        if (best!=null) {
            double len=Math.hypot(best.firstX,best.firstZ);
            double bx=best.firstX/len,bz=best.firstZ/len;
            // Retain the native smooth tangent only when it agrees with a real downhill path.
            if (dx*bx+dz*bz > 0.7) return new Result(dx,dz,true,visits);
            return new Result(bx,bz,true,visits);
        }
        int ax=(int)Math.round(dx),az=(int)Math.round(dz);
        Surface ahead=field.sample(x+ax,z+az,start.y);
        if (ahead!=null && ahead.height>start.height+0.125) return new Result(0,0,false,visits);
        return new Result(dx,dz,false,visits);
    }
    private DownhillSearch() {}
}
