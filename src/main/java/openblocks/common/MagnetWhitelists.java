package openblocks.common;

import net.minecraft.block.*;
import net.minecraft.block.BlockJukebox.TileEntityJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import openmods.utils.*;
import openmods.utils.ObjectTester.ClassNameTester;
import openmods.utils.ObjectTester.ClassTester;

public class MagnetWhitelists {
	public final static MagnetWhitelists instance = new MagnetWhitelists();

	public static class BlockCoords extends BlockPos {
		public final Block block;
		public final IBlockState blockState;
		public final World world;

		BlockCoords(Block block, IBlockState blockState, World world, Vec3i pos) {
			super(pos);
			this.block = block;
			this.blockState = blockState;
			this.world = world;
		}
	}

	private MagnetWhitelists() {}

	public final ObjectTester<Entity> entityWhitelist = ObjectTester.create();
	public final ObjectTester<BlockCoords> blockWhitelist = ObjectTester.create();
	public final ObjectTester<TileEntity> tileEntityWhitelist = ObjectTester.create();

	public static class WhitelistRegisterEvent<T> extends Event {
		protected final ObjectTester<T> tester;

		protected WhitelistRegisterEvent(ObjectTester<T> tester) {
			this.tester = tester;
		}

		public void addClass(Class<? extends T> cls) {
			tester.addTester(new ClassTester<T>(cls));
		}

		public void addClassNames(Class<? extends T>... cls) {
			tester.addTester(new ClassNameTester<T>().addClasses(cls));
		}

		public void addClassNames(String... names) {
			tester.addTester(new ClassNameTester<T>().addClasses(names));
		}
	}

	public static class EntityRegisterEvent extends
			WhitelistRegisterEvent<Entity> {
		protected EntityRegisterEvent(ObjectTester<Entity> tester) {
			super(tester);
		}
	}

	public static class BlockRegisterEvent extends WhitelistRegisterEvent<BlockCoords> {
		protected BlockRegisterEvent(ObjectTester<BlockCoords> tester) {
			super(tester);
		}
	}

	public static class TileEntityRegisterEvent extends
			WhitelistRegisterEvent<TileEntity> {
		protected TileEntityRegisterEvent(ObjectTester<TileEntity> tester) {
			super(tester);
		}
	}

	public static ITester<BlockCoords> createBlockIdentityTester(final Block template) {
		return new ITester<BlockCoords>() {
			@Override
			public ITester.Result test(BlockCoords o) {
				return (o.block == template)? Result.ACCEPT : Result.CONTINUE;
			}
		};
	}

	public static ITester<BlockCoords> createBlockClassTester(final Class<? extends Block> cls) {
		return new ITester<BlockCoords>() {
			@Override
			public ITester.Result test(BlockCoords o) {
				return (cls.isInstance(o.block))? Result.ACCEPT : Result.CONTINUE;
			}
		};
	}

	public void initTesters() {
		entityWhitelist.addTester(new ClassTester<Entity>(EntityItem.class));
		entityWhitelist.addTester(new ClassTester<Entity>(EntityBoat.class));
		entityWhitelist.addTester(new ClassTester<Entity>(EntityMinecart.class));
		MinecraftForge.EVENT_BUS.post(new EntityRegisterEvent(entityWhitelist));

		blockWhitelist.addTester(new ITester<BlockCoords>() {
			@Override
			public Result test(BlockCoords o) {
				float hardness = o.block.getBlockHardness(o.world, o);
				return (hardness < 0)? Result.REJECT : Result.CONTINUE;
			}
		});

		blockWhitelist.addTester(new ITester<BlockCoords>() {
			@Override
			public openmods.utils.ITester.Result test(BlockCoords o) {
				return o.block.getRenderType() == 0? Result.ACCEPT : Result.CONTINUE;
			}
		});

		blockWhitelist.addTester(createBlockClassTester(BlockSand.class));
		blockWhitelist.addTester(createBlockClassTester(BlockStairs.class));
		blockWhitelist.addTester(createBlockClassTester(BlockFence.class));
		blockWhitelist.addTester(createBlockClassTester(BlockFenceGate.class));
		blockWhitelist.addTester(createBlockIdentityTester(Blocks.cactus));
		MinecraftForge.EVENT_BUS.post(new BlockRegisterEvent(blockWhitelist));

		tileEntityWhitelist
				.addTester(new ClassTester<TileEntity>(TileEntityBeacon.class))
				.addTester(new ClassTester<TileEntity>(TileEntityBrewingStand.class))
				.addTester(new ClassTester<TileEntity>(TileEntityChest.class))
				.addTester(new ClassTester<TileEntity>(TileEntityCommandBlock.class))
				.addTester(new ClassTester<TileEntity>(TileEntityDispenser.class))
				.addTester(new ClassTester<TileEntity>(TileEntityEnchantmentTable.class))
				.addTester(new ClassTester<TileEntity>(TileEntityEnderChest.class))
				.addTester(new ClassTester<TileEntity>(TileEntityFurnace.class))
				.addTester(new ClassTester<TileEntity>(TileEntityHopper.class))
				.addTester(new ClassTester<TileEntity>(TileEntityNote.class))
				.addTester(new ClassTester<TileEntity>(TileEntityJukebox.class));
	}

	public boolean testBlock(World world, BlockPos pos) {
		final IBlockState blockState = world.getBlockState(pos);
		final Block block = blockState.getBlock();

		if (block.isAir(world, pos)) return false;

		if (block instanceof BlockContainer) {
			TileEntity te = world.getTileEntity(pos);
			return (te != null)? tileEntityWhitelist.check(te) : false;
		}

		return blockWhitelist.check(new BlockCoords(block, blockState, world, pos));
	}
}
