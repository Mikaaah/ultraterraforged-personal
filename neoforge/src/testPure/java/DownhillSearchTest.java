import nl.townsandkingdoms.terraforged.compat.streams.DownhillSearch;
import java.util.*;
public final class DownhillSearchTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static DownhillSearch.Surface s(int y){return new DownhillSearch.Surface(y,y+0.888);}
    public static void main(String[] args){
        DownhillSearch.Field west=(x,z,y)->Math.abs(x)<=8 && Math.abs(z)<=1 ? s(x<0?5:8):null;
        var r=DownhillSearch.find(west,3,0,s(8),1,0,16,384);
        check(r.outlet() && r.x()<-.9,"Upstream east current was not reversed toward real westward drop");
        r=DownhillSearch.find(west,3,0,s(8),-1,0,16,384);
        check(r.outlet() && r.x()<-.9,"Correct downstream current was changed");
        Set<String> bend=new HashSet<>();
        for(int x=0;x<=3;x++){bend.add(x+",0");bend.add(x+",4");}
        for(int z=0;z<=4;z++)bend.add("3,"+z);
        DownhillSearch.Field curved=(x,z,y)->bend.contains(x+","+z)?s(x==0&&z==4?5:8):null;
        r=DownhillSearch.find(curved,0,0,s(8),-1,0,16,384);
        check(r.outlet() && r.x()>.9,"Search must follow the connected bend, not a straight height ray");
        DownhillSearch.Field crest=(x,z,y)->z==0&&x>=0&&x<=2?s(x==0?8:x==1?10:5):null;
        r=DownhillSearch.find(crest,0,0,s(8),1,0,16,384);
        check(!r.outlet() && r.x()==0 && r.z()==0,"Water must not flow uphill across a crest to a disconnected lower pocket");
        r=DownhillSearch.find((x,z,y)->s(8),0,0,s(8),0,-1,16,64);
        check(!r.outlet() && r.z()==-1 && r.visited()<=64,"Flat/unresolved water must preserve the original flow within the work budget");
        DownhillSearch.Field corner=(x,z,y)->x==0&&z==0?s(8):x==1&&z==1?s(5):null;
        r=DownhillSearch.find(corner,0,0,s(8),1,0,16,384);
        check(!r.outlet(),"Search cut diagonally through two solid banks");
        r=DownhillSearch.find(west,3,0,s(8),1,0,1,384);
        check(!r.outlet(),"Search exceeded radius");
        System.out.println(checks+" actual-water search checks passed");
    }
}
