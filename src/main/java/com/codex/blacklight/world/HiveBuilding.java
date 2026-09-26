package com.codex.blacklight.world;

import com.codex.blacklight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/** A ruined three-storey building overrun by branching tissue, with an exposed ground-floor core. */
public final class HiveBuilding {
    private HiveBuilding(){}
    public static boolean clearSite(WorldGenLevel level,BlockPos origin){
        if(origin.getY()<level.getSeaLevel()||origin.getY()+14>=level.getMaxBuildHeight())return false;
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            boolean ground=false;
            for(int d=1;d<=4;d++){var below=level.getBlockState(origin.offset(x,-d,z));if(!below.getFluidState().isEmpty())return false;if(below.isSolid()){ground=true;break;}}
            if(!ground)return false;
            for(int y=0;y<=13;y++){
                var at=origin.offset(x,y,z);var state=level.getBlockState(at);
                if(level.getBlockEntity(at)!=null||(!state.isAir()&&!state.canBeReplaced())||!state.getFluidState().isEmpty())return false;
            }
        }
        return true;
    }
    public static void build(LevelAccessor level,BlockPos origin){
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int d=1;d<=3;d++){
            var at=origin.offset(x,-d,z);if(level.getBlockState(at).isSolid())break;
            level.setBlock(at,Blocks.COBBLESTONE.defaultBlockState(),2);
        }
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=0;y<=12;y++){
            boolean edge=Math.abs(x)==5||Math.abs(z)==5,slab=y==0||y==5||y==9||y==12;
            if(!edge&&!slab)continue;
            // Wide ruptured entrance, broken window bands and a hole above the core.
            if(z==-5&&Math.abs(x)<=2&&y>=1&&y<=4)continue;
            if(!edge&&Math.abs(x)<=1&&Math.abs(z)<=1&&y>0)continue;
            if(edge&&y%4>=2&&Math.abs(x)!=5&&Math.abs(x)%3!=0)continue;
            if(edge&&y%4>=2&&Math.abs(z)!=5&&Math.abs(z)%3!=0)continue;
            var block=(x*19+z*11+y*7)%5==0?Blocks.CRACKED_STONE_BRICKS:slab?Blocks.SMOOTH_STONE:Blocks.STONE_BRICKS;
            level.setBlock(origin.offset(x,y,z),block.defaultBlockState(),2);
        }
        // Veins branch across walls and roots continue onto the ground.
        for(int side=0;side<4;side++)for(int branch=-1;branch<=1;branch++)for(int y=0;y<=12;y++){
            int across=branch*3+(int)Math.round(Math.sin(y*.55+side+branch)*1.2);
            int x=side%2==0?across:(side==1?6:-6),z=side%2==0?(side==0?-6:6):across;
            if(z<0&&Math.abs(x)<3&&y<5)continue;
            level.setBlock(origin.offset(x,y,z),ModBlocks.HIVE_FLESH.get().defaultBlockState(),2);
            if(y%3==0)level.setBlock(origin.offset(x+(side%2==0?1:0),y,z+(side%2==1?1:0)),ModBlocks.HIVE_CARAPACE.get().defaultBlockState(),2);
            if(y==6||y==10)level.setBlock(origin.offset(x,y+1,z),ModBlocks.HIVE_SAC.get().defaultBlockState(),2);
        }
        for(int i=-6;i<=6;i++){
            level.setBlock(origin.offset(i,0,6),ModBlocks.HIVE_FLESH.get().defaultBlockState(),2);
            if(Math.abs(i)>2)level.setBlock(origin.offset(i,0,-6),ModBlocks.HIVE_FLESH.get().defaultBlockState(),2);
        }
        for(int side:new int[]{-1,1})for(int x=3;x<=7;x++)for(int z=-2;z<=2;z++)if((x+z)%2==0)
            level.setBlock(origin.offset(x*side,0,z),ModBlocks.HIVE_CARAPACE.get().defaultBlockState(),2);
    }
}
