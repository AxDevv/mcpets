package fr.nocsy.mcpets.data;

import fr.nocsy.mcpets.data.config.FormatArg;
import fr.nocsy.mcpets.data.config.GlobalConfig;
import fr.nocsy.mcpets.data.config.ItemsListConfig;
import fr.nocsy.mcpets.data.config.Language;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public enum Items {

    UNKNOWN("unkown"),

    MOUNT("mount"),
    RENAME("rename"),
    PETMENU("petmenu"),
    INVENTORY("inventory"),
    SKINS("skins"),
    EQUIPMENT("equipment"),
    PAGE_SELECTOR("page_selector");

    @Setter
    @Getter
    private ItemStack item;

    @Getter
    private String name;

    Items(String name) {
        this.name = name;
        if(ItemsListConfig.getInstance().getItemStack(name) != null) {
            item = ItemsListConfig.getInstance().getItemStack(name);
            prepareItem();
            return;
        }

        switch (name) {
            case "mount":
                item = mount();
                break;
            case "rename":
                item = rename();
                break;
            case "petmenu":
                item = petmenu();
                break;
            case "inventory":
                item = inventory();
                break;
            case "skins":
                item = skins();
                break;
            case "equipment":
                item = equipment();
                break;
            case "page_selector":
                item = page_item();
                break;
            default:
                item = unknown();
        }

        prepareItem();
    }

    private void prepareItem() {
        ItemMeta meta = item.getItemMeta();
        meta.setItemName(getLocalizedName());
        item.setItemMeta(meta);
    }

    public String getLocalizedName() {
        return "MCPetsMenu;" + name;
    }

    private static ItemStack unknown() {
        ArrayList<String> lore = new ArrayList<>();

        ItemStack it = new ItemStack(Material.BEDROCK);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("Unknown");
        meta.setLore(lore);
        meta.setItemName("AlmPet;Unknown");

        it.setItemMeta(meta);

        return it;
    }

    private static ItemStack mount() {
        ItemStack it = new ItemStack(Material.SADDLE);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.MOUNT_ITEM_NAME.getMessage());

        ArrayList<String> lore = new ArrayList<>(Arrays.asList(Language.MOUNT_ITEM_DESCRIPTION.getMessage().split("\n")));
        meta.setLore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack rename() {
        ItemStack it = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.RENAME_ITEM_NAME.getMessage());

        ArrayList<String> lore = new ArrayList<>(Arrays.asList(Language.RENAME_ITEM_DESCRIPTION.getMessage().split("\n")));
        meta.setLore(lore);

        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack petmenu() {
        ArrayList<String> lore = new ArrayList<>(Arrays.asList(Language.BACK_TO_PETMENU_ITEM_DESCRIPTION.getMessage().split("\n")));

        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.BACK_TO_PETMENU_ITEM_NAME.getMessage());
        meta.setLore(lore);
        meta.setItemName("AlmPet;BackToPetMenu");

        it.setItemMeta(meta);

        return it;
    }

    private static ItemStack inventory() {
        ItemStack it = new ItemStack(Material.CHEST);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.INVENTORY_ITEM_NAME.getMessage());
        meta.setItemName("AlmPet;Inventory");

        it.setItemMeta(meta);

        return it;
    }

    private static ItemStack skins() {
        ItemStack it = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.SKINS_ITEM_NAME.getMessage());
        meta.setItemName("AlmPet;Skins");

        it.setItemMeta(meta);

        return it;
    }

    private static ItemStack equipment() {
        ItemStack it = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.EQUIPMENT_ITEM_NAME.getMessage());
        meta.setItemName("AlmPet;Inventory");

        it.setItemMeta(meta);

        return it;
    }

    private static ItemStack page_item(){
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        meta.setCustomModelData(9660);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack page(int index, Player p) {
        ItemStack it = ItemsListConfig.getInstance().getItemStack("page_selector");
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.TURNPAGE_ITEM_NAME.getMessageFormatted(new FormatArg("%currentPage%", Integer.toString(index+1)),
                                                                            new FormatArg("%maxPage%", Integer.toString((int)((double)Pet.getAvailablePets(p).size()/54 + 0.5)))));

        meta.setItemName("AlmPetPage;" + index);

        ArrayList<String> lore = new ArrayList<>(Arrays.asList(Language.TURNPAGE_ITEM_DESCRIPTION.getMessage().split("\n")));
        meta.setLore(lore);

        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack page(Category category, int index) {
        ItemStack it = ItemsListConfig.getInstance().getItemStack("page_selector");
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Language.TURNPAGE_ITEM_NAME.getMessageFormatted(new FormatArg("%currentPage%", Integer.toString(index+1)),
                                                                            new FormatArg("%maxPage%", Integer.toString(category.getMaxPages()))));
        meta.setItemName("MCPetsPage;" + category.getId() + ";" + index);

        ArrayList<String> lore = new ArrayList<>(Arrays.asList(Language.TURNPAGE_ITEM_DESCRIPTION.getMessageFormatted(
                                                        new FormatArg("%currentPage%", Integer.toString(index)),
                                                        new FormatArg("%maxPage%", Integer.toString(category.getMaxPages())))
                                                        .split("\n")));
        meta.setLore(lore);

        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack petInfo(Pet pet) {
        Pet objectPet = Pet.getFromId(pet.getId());

        ItemStack it = objectPet.getIcon().clone();
        ItemMeta meta = it.getItemMeta();

        ArrayList<String> lore;
        if (meta.getLore() == null)
            lore = new ArrayList<>();
        else
            lore = new ArrayList<>(meta.getLore());

        if (pet.getCurrentName() != null && GlobalConfig.getInstance().isNameable()) {
            lore.add(" ");
            lore.add(Language.NICKNAME.getMessageFormatted(new FormatArg("%nickname%", pet.getCurrentName())));
            lore.add(" ");
        }

        lore.addAll(Arrays.asList(Language.NICKNAME_ITEM_LORE.getMessage().split("\n")));

        meta.setLore(lore);
        meta.setItemName(null);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack deco(Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§0");

        ArrayList<String> lore = new ArrayList<>();
        meta.setLore(lore);

        it.setItemMeta(meta);

        return it;
    }

    public static boolean isSignalStick(ItemStack it) {
        return it != null &&
                it.hasItemMeta() &&
                it.getItemMeta().hasItemName() &&
                it.getItemMeta().getItemName().contains(Pet.SIGNAL_STICK_TAG);
    }

    public static ItemStack turnIntoSignalStick(ItemStack it, Pet pet) {
        if (it == null || it.getType().isAir() || pet == null)
            return it;
        ItemMeta meta = it.getItemMeta();
        meta.setItemName(buildSignalStickTag(pet));
        it.setItemMeta(meta);
        return it;
    }

    public static String buildSignalStickTag(Pet pet) {
        if (pet == null)
            return null;
        return Pet.SIGNAL_STICK_TAG + ";" + pet.getId();
    }

    public static String getPetTag(ItemStack it) {
        if (it != null && it.hasItemMeta() && it.getItemMeta().hasItemName()) {
            String[] split = it.getItemMeta().getItemName().split(";");
            if (split.length == 2)
                return split[1];
        }

        return null;
    }
}
