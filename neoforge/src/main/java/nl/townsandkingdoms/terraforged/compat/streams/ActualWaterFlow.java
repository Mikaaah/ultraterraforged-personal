package nl.townsandkingdoms.terraforged.compat.streams;

import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

/** Corrects final physics and baked-fluid flow against saved, loaded water. No chunk loading or terrain edits. */
public final class ActualWaterFlow {
    public static final LongAdder SEARCHES=new LongAdder(), CORRECTIONS=new LongAdder();
    private static final Map<Level,Map<Key,Entry>> CACHE=new WeakHashMap<>();
    private record Key(long pos,byte angle) {}
    private record Entry(long tick,Vec3 vector) {}
    private static volatile Method manual;
    private static volatile boolean checkedManual;
    private static Level view(BlockGetter getter) {
        if (getter instanceof Level level) return level;
        // Render regions are finite snapshots. Probe the client's loaded level, never outside a render-region array.
        return ActualWaterState.clientActive && getter instanceof BlockAndTintGetter
                ? nl.townsandkingdoms.terraforged.compat.streams.client.ActualWaterClient.level() : null;
    }
    private static boolean active(Level level) {
        if (level==null || !level.dimension().equals(Level.OVERWORLD) || !UtfStreams.ACTUAL_WATER.get()) return false;
        return level instanceof ServerLevel server ? UtfStreams.context(server)!=null : ActualWaterState.clientActive;
    }
    private static boolean manual(BlockGetter view,BlockPos pos) {
        if (!checkedManual) synchronized(ActualWaterFlow.class) {
            if (!checkedManual) {
                try { manual=Class.forName("nl.townsandkingdoms.streamcontrol.FlowOverrides").getMethod("at",BlockGetter.class,int.class,int.class,int.class); }
                catch (ClassNotFoundException|NoSuchMethodException ignored) {}
                checkedManual=true;
            }
        }
        if (manual==null) return false;
        try { return manual.invoke(null,view,pos.getX(),pos.getY(),pos.getZ())!=null; }
        catch (ReflectiveOperationException e) { return false; }
    }
    private static boolean loaded(Level level,int x,int z) { return level.hasChunk(x>>4,z>>4); }
    private static DownhillSearch.Surface surface(Level level,int x,int z,int fromY,boolean start) {
        if (!loaded(level,x,z)) return null;
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos(x,fromY,z);
        int y=fromY;
        var block=level.getBlockState(p);
        if (!(block.getBlock() instanceof LiquidBlock) || !block.getFluidState().is(FluidTags.WATER)) {
            if (start) return null;
            boolean found=false;
            for (int drop=0;drop<8 && y>level.getMinBuildHeight();drop++,y--) {
                block=level.getBlockState(p.set(x,y,z));
                if (block.getBlock() instanceof LiquidBlock && block.getFluidState().is(FluidTags.WATER)) {found=true;break;}
                if (!block.isAir()) return null;
            }
            if (!found) return null;
        }
        for (int rise=0;rise<64;rise++) {
            var above=level.getBlockState(p.set(x,y+1,z));
            if (!(above.getBlock() instanceof LiquidBlock) || !above.getFluidState().is(FluidTags.WATER)) {
                var fluid=level.getFluidState(p.set(x,y,z));
                return new DownhillSearch.Surface(y,y+fluid.getOwnHeight());
            }
            y++;
        }
        return null;
    }
    public static Vec3 direction(BlockGetter getter,BlockPos pos,double dx,double dz) {
        Level level=view(getter);
        if (!active(level) || manual(getter,pos)) return null;
        double length=Math.hypot(dx,dz);
        if (!Double.isFinite(length) || length<1e-6) return null;
        var start=surface(level,pos.getX(),pos.getZ(),pos.getY(),true);
        if (start==null) return null;
        Key key=new Key(BlockPos.asLong(pos.getX(),start.y(),pos.getZ()),StreamFlowGrid.encode(dx,dz));
        long tick=level.getGameTime();
        synchronized(CACHE) {
            var map=CACHE.get(level); var entry=map==null?null:map.get(key);
            if(entry!=null && tick>=entry.tick && tick-entry.tick<10) return entry.vector;
        }
        SEARCHES.increment();
        var result=DownhillSearch.find((x,z,y)->surface(level,x,z,y,false),pos.getX(),pos.getZ(),start,dx/length,dz/length,
                UtfStreams.SCAN_RADIUS.get(),384);
        Vec3 out=new Vec3(result.x(),0,result.z());
        if (out.x*dx/length+out.z*dz/length<0.99) CORRECTIONS.increment();
        synchronized(CACHE) {
            var map=CACHE.computeIfAbsent(level,ignored->new LinkedHashMap<>());
            if(map.size()>=8192) map.clear();
            map.put(key,new Entry(tick,out));
        }
        return out;
    }
    public static Vec3 apply(Vec3 original,BlockGetter level,BlockPos pos,FluidState state) {
        if(!state.is(FluidTags.WATER)) return original;
        Vec3 direction=direction(level,pos,original.x,original.z);
        if(direction==null) return original;
        return new Vec3(direction.x*Math.hypot(original.x,original.z),original.y,direction.z*Math.hypot(original.x,original.z));
    }
    public static byte angle(BlockGetter level,int x,int y,int z,byte original) {
        if(y==Integer.MIN_VALUE) return original;
        BlockPos pos=new BlockPos(x,y,z);
        if(original==-1) {
            Level world=view(level);
            if(!active(world) || !loaded(world,x,z)) return original;
            var state=world.getBlockState(pos);
            if(state.getBlock() instanceof StreamLiquidBlock) {
                int f=state.getValue(StreamLiquidBlock.FLOW);
                if(f<8) original=StreamFlowGrid.encode(StreamLiquidBlock.DIRS[f].x,StreamLiquidBlock.DIRS[f].z);
            }
        }
        if(original==-1) return original;
        Vec3 dir=direction(level,pos,StreamFlowGrid.vx(original),StreamFlowGrid.vz(original));
        return dir==null ? original : dir.lengthSqr()<1e-8 ? (byte)-1 : StreamFlowGrid.encode(dir.x,dir.z);
    }
    public static void clear() { synchronized(CACHE) {CACHE.clear();} }
    private ActualWaterFlow() {}
}
