package net.tslat.aoawikihelpermod.util.printers.handlers.recipe.thermalexpansion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.tslat.aoa3.util.StringUtil;
import net.tslat.aoawikihelpermod.util.FormattingHelper;
import net.tslat.aoawikihelpermod.util.ObjectHelper;
import net.tslat.aoawikihelpermod.util.printers.handlers.RecipePrintHandler;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TEChillerRecipeHandler extends RecipePrintHandler {
	private final ResourceLocation recipeId;
	private final JsonObject rawRecipe;

	private final HashMap<Item, String[]> printoutData = new HashMap<Item, String[]>();

	public TEChillerRecipeHandler(ResourceLocation recipeId, JsonObject rawRecipe, @Nullable IRecipe<?> recipe) {
		this.recipeId = recipeId;
		this.rawRecipe = rawRecipe;
	}

	@Override
	public String getTableGroup() {
		return "TE Chiller";
	}

	@Override
	public ResourceLocation getRecipeId() {
		return this.recipeId;
	}

	@Override
	public String[] getColumnTitles() {
		return new String[] {"Input", "Energy", "Output"};
	}

	@Override
	public List<ResourceLocation> getIngredientsForLookup() {
		if (this.rawRecipe.has("input")) {
			ResourceLocation input = ObjectHelper.getIngredientItemId(this.rawRecipe.get("input"));

			if (input == null)
				return Collections.emptyList();

			return Collections.singletonList(input);
		}

		return Collections.emptyList();
	}

	@Override
	public List<ResourceLocation> getOutputsForLookup() {
		if (this.rawRecipe.get("result").isJsonObject()) {
			return Collections.singletonList(ObjectHelper.getIngredientItemId(this.rawRecipe.get("result")));
		}
		else if (this.rawRecipe.get("result").isJsonArray()) {
			ArrayList<ResourceLocation> outputs = new ArrayList<ResourceLocation>();

			for (JsonElement ele : this.rawRecipe.get("result").getAsJsonArray()) {
				outputs.add(ObjectHelper.getIngredientItemId(ele));
			}

			return outputs;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public String[] toTableEntry(@Nullable Item targetItem) {
		if (this.printoutData.containsKey(targetItem))
			return this.printoutData.get(targetItem);

		String targetName = targetItem == null ? "" : ObjectHelper.getItemName(targetItem);
		JsonObject input = JSONUtils.getAsJsonObject(this.rawRecipe, "fluid");
		String inputAmount = JSONUtils.getAsInt(input, "amount", 1000) + "mb";
		String inputFluid = StringUtil.toTitleCase(new ResourceLocation(JSONUtils.getAsString(input, "fluid")).getPath());
		int energy = JSONUtils.getAsInt(this.rawRecipe, "energy", 4000);
		List<Triple<Integer, String, String>> result;

		if (this.rawRecipe.get("result").isJsonObject()) {
			result = Collections.singletonList(ObjectHelper.getStackDetailsFromJson(this.rawRecipe.get("result")));
		}
		else if (this.rawRecipe.get("result").isJsonArray()) {
			JsonArray resultArray = this.rawRecipe.get("result").getAsJsonArray();
			result = new ArrayList<Triple<Integer, String, String>>();

			for (JsonElement ele : resultArray) {
				result.add(ObjectHelper.getStackDetailsFromJson(ele));
			}
		}
		else {
			result = Collections.emptyList();
		}

		StringBuilder resultBuilder = new StringBuilder();

		for (Triple<Integer, String, String> resultEntry : result) {
			if (resultBuilder.length() > 0)
				resultBuilder.append(" +<br/>");

			resultBuilder.append(FormattingHelper.createImageBlock(resultEntry.getRight()))
					.append(" ")
					.append(resultEntry.getLeft())
					.append(" ")
					.append(FormattingHelper.createLinkableText(resultEntry.getRight(), resultEntry.getLeft() > 1, resultEntry.getMiddle().equals("minecraft"), !resultEntry.getRight().equals(targetName)));
		}

		String[] printData = new String[3];
		printData[0] = inputAmount + " " + inputFluid;
		printData[1] = String.valueOf(energy);
		printData[2] = resultBuilder.toString();

		this.printoutData.put(targetItem, printData);

		return printData;
	}
}
