package net.tslat.aoawikihelpermod.util.printers.handlers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.tslat.aoa3.content.block.functional.light.LampBlock;
import net.tslat.aoawikihelpermod.util.FormattingHelper;
import net.tslat.aoawikihelpermod.util.ObjectHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockDataPrintHandler {
	private final Block block;

	private String[][] statePrintout = null;
	private String[] tagsPrintout = null;
	private Pair<String, String> logStripPrintout = null;

	public BlockDataPrintHandler(Block block) {
		this.block = block;
	}

	public String[][] getStatePrintout() {
		if (statePrintout != null)
			return statePrintout;

		Collection<Property<?>> properties = block.getStateDefinition().getProperties();
		this.statePrintout = new String[properties.size()][3];
		int i = 0;

		for (Property<?> property : properties) {
			this.statePrintout[i][0] = property.getName();
			this.statePrintout[i][1] = FormattingHelper.listToString(property.getPossibleValues().stream().map(Object::toString).collect(Collectors.toList()), false);
			this.statePrintout[i][2] = getPropertyDescription(property);

			i++;
		}

		return this.statePrintout;
	}

	public String[] getTagsPrintout() {
		if (tagsPrintout != null)
			return tagsPrintout;

		if (!hasTags()) {
			this.tagsPrintout = new String[] {ObjectHelper.getBlockName(block) + " has no tags."};
		}
		else {
			StringBuilder builder = new StringBuilder();
			List<ResourceLocation> blockTags = getBlockTags();
			List<ResourceLocation> itemTags = getItemTags();
			List<ResourceLocation> fluidTags = getFluidTags();

			if (blockTags != null) {
				builder.append("=== Block tags: ====<br/>");
				builder.append(FormattingHelper.listToString(blockTags.stream().map((id) -> "* " + id.toString()).collect(Collectors.toList()), false));
				builder.append("<br/><br/>");
			}

			if (itemTags != null) {
				builder.append("=== Item tags: ===<br/>");
				builder.append(FormattingHelper.listToString(itemTags.stream().map((id) -> "* " + id.toString()).collect(Collectors.toList()), false));
				builder.append("<br/><br/>");
			}

			if (fluidTags != null) {
				builder.append("=== Fluid tags: ===<br/>");
				builder.append(FormattingHelper.listToString(fluidTags.stream().map((id) -> "* " + id.toString()).collect(Collectors.toList()), false));
			}

			String[] lines = builder.toString().split("<br/>");

			this.tagsPrintout = new String[lines.length + 1];
			this.tagsPrintout[0] = ObjectHelper.getBlockName(block) + " has the following tags:<br/>";

			System.arraycopy(lines, 0, this.tagsPrintout, 1, lines.length);
		}

		return this.tagsPrintout;
	}

	@Nullable
	public String getStrippableBlockDescription() {
		if (logStripPrintout != null)
			return logStripPrintout.getFirst();

		prepLogStripDescription();

		return logStripPrintout.getFirst();
	}

	@Nullable
	public String getStrippedBlockDescription() {
		if (logStripPrintout != null)
			return logStripPrintout.getSecond();

		prepLogStripDescription();

		return logStripPrintout.getSecond();
	}

	public void prepLogStripDescription() {
		ArrayList<Block> strippableBlocks = new ArrayList<Block>(1);
		Block stripsTo = AxeItem.STRIPPABLES.get(this.block);

		for (Map.Entry<Block, Block> entry : AxeItem.STRIPPABLES.entrySet()) {
			if (entry.getValue() == this.block)
				strippableBlocks.add(entry.getKey());
		}

		String strippableDescription = null;
		String strippedDescription = null;

		if (!strippableBlocks.isEmpty()) {
			StringBuilder builder = new StringBuilder();

			if (strippableBlocks.size() == 1) {
				builder.append(block);
				builder.append(" can be made by stripping a ");
				builder.append(FormattingHelper.createLinkableText(strippableBlocks.get(0).getName().getString(), false, true));
				builder.append(" by using an axe on it");
			}
			else {
				builder.append(block);
				builder.append(" can be made by stripping any of the following blocks by using an axe on them:\n");

				for (Block strippableBlock : strippableBlocks) {
					builder.append("* ");
					builder.append(FormattingHelper.createLinkableText(strippableBlock.getName().getString(), false, true));
				}
			}

			strippableDescription = builder.toString();
		}

		if (stripsTo != null) {
			StringBuilder builder = new StringBuilder();

			builder.append(FormattingHelper.lazyPluralise(block.getName().getString()));
			builder.append(" can be stripped into a ");
			builder.append(FormattingHelper.createLinkableText(stripsTo.getName().getString(), false, true));
			builder.append(" by using an axe on it.");

			strippedDescription = builder.toString();
		}

		this.logStripPrintout = Pair.of(strippableDescription, strippedDescription);
	}

	public boolean hasHasStateProperties() {
		return !block.defaultBlockState().getProperties().isEmpty();
	}

	public boolean hasTags() {
		return getBlockTags() != null || getItemTags() != null || getFluidTags() != null;
	}

	@Nullable
	private List<ResourceLocation> getBlockTags() {
		if (block.builtInRegistryHolder().tags().toList().isEmpty())
			return null;

		return block.builtInRegistryHolder().tags().map(TagKey::location).toList();
	}

	@Nullable
	private List<ResourceLocation> getItemTags() {
		if (block.asItem() == Items.AIR || block.asItem().builtInRegistryHolder().tags().toList().isEmpty())
			return null;

		return block.asItem().builtInRegistryHolder().tags().map(TagKey::location).toList();
	}

	@Nullable
	private List<ResourceLocation> getFluidTags() {
		FluidState fluidState = block.getFluidState(block.defaultBlockState());

		if (fluidState.isEmpty() || fluidState.getType().builtInRegistryHolder().tags().toList().isEmpty())
			return null;

		return fluidState.getType().builtInRegistryHolder().tags().map(TagKey::location).toList();
	}

	// Not at all an exhaustive list, this can easily be expanded
	private static String getPropertyDescription(Property<?> property) {
		if (property == LampBlock.LIT) {
			return "Determines whether the block is lit or not.";
		}
		else if (property == LampBlock.TOGGLEABLE) {
			return "Determines whether the lamp block is able to be lit or turned off by block updates or redstone.";
		}
		else if (property == BlockStateProperties.STAIRS_SHAPE) {
			return "The visible shape of the stairs.";
		}
		else if (property == BlockStateProperties.HALF) {
			return "Which vertical half of the block it currently is.";
		}
		else if (property == BlockStateProperties.HORIZONTAL_FACING) {
			return "Which direction the block is facing in all cardinal directions.";
		}
		else if (property == BlockStateProperties.FACING) {
			return "Which direction the block is facing in all directions.";
		}
		else if (property == BlockStateProperties.WATERLOGGED) {
			return "Whether the block is currently waterlogged or not.";
		}
		else if (property == BlockStateProperties.STAGE) {
			return "Determines the stage of the block from 0-1";
		}
		else if (property == BlockStateProperties.LEVEL) {
			return "Determines the level of a fluid from 0-15";
		}
		else if (property == BlockStateProperties.SNOWY) {
			return "Whether the block is a snowy variant of itself";
		}
		else if (property == BlockStateProperties.AXIS) {
			return "Which axis the block is currently rotated on.";
		}
		else if (property == BlockStateProperties.SLAB_TYPE) {
			return "Whether the slab is a half-slab or double-slab variant.";
		}
		else if (property == BlockStateProperties.POWER) {
			return "Determines the amount of redstone power the block is giving or receiving.";
		}
		else if (property == BlockStateProperties.POWERED) {
			return "Whether the block is powered by redstone or not.";
		}
		else if (property == BlockStateProperties.NORTH || property == BlockStateProperties.SOUTH || property == BlockStateProperties.EAST || property == BlockStateProperties.WEST) {
			return "Whether the block is connected to the next block in this direction";
		}
		else {
			return "";
		}
	}
}
