package fr.nocsy.mcpets.data.livingpets;

import fr.nocsy.mcpets.MCPets;
import fr.nocsy.mcpets.data.Items;
import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.config.FormatArg;
import fr.nocsy.mcpets.data.config.ItemsListConfig;
import fr.nocsy.mcpets.data.config.Language;
import fr.nocsy.mcpets.data.config.PetFoodConfig;
import fr.nocsy.mcpets.utils.PetMath;
import fr.nocsy.mcpets.utils.Utils;
import fr.nocsy.mcpets.utils.debug.Debugger;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PetFood {

    private static final HashMap<String, PetFood> petFoodHashMap = new HashMap<>();


    //----------------- Generic item section ------------------//
    @Getter
    private String id;

    @Getter
    private String itemId;

    @Getter
    private double power;

    @Getter
    private PetFoodType type;

    @Getter
    private PetMath operator;

    @Getter
    private String signal;

    @Getter
    private long cooldown;

    @Getter
    private List<String> petIds;

    @Getter
    private boolean defaultMCItem;

    private ItemStack itemStack;


    //----------------- Evolution item section ------------------//
    @Getter
    private String evolution;

    @Getter
    private int experienceThreshold;

    @Getter
    private int delay;


    //----------------- Unlock item section ------------------//
    @Getter
    private String permission;

    @Getter
    private String unlockedPet;


    //----------------- Buffs item section ------------------//
    @Getter
    private long duration;

    /**
     * Constructor
     */
    public PetFood(
            String id,
            String itemId,
            double power,
            PetFoodType type,
            PetMath operator,
            String signal,
            long cooldown,
            String evolution,
            int experienceThreshold,
            int delay,
            String permission,
            String unlockedPet,
            List<String> petIds,
            long duration) {
        this.id = id;
        this.itemId = itemId;
        this.power = power;
        this.type = type;
        this.operator = operator;
        this.signal = signal;
        this.cooldown = cooldown;
        this.evolution = evolution;
        this.experienceThreshold = experienceThreshold;
        this.delay = delay;
        this.permission = permission;
        this.unlockedPet = unlockedPet;
        this.petIds = petIds;
        this.duration = duration;

        // Setup the item stack
        this.getItemStack();

        petFoodHashMap.put(id, this);
    }

    /**
     * Get the item stack of the pet food
     */
    public ItemStack getItemStack() {
        // Setup the item stack
        if (itemStack == null) {
            if (itemId.contains("itemsadder") && MCPets.getItemsAdder() != null) {
                // Expected: itemsadder-namespace:item_name
                String[] parts = itemId.split("-", 2);
            
                if (parts.length == 2 && parts[0].equalsIgnoreCase("itemsadder")) {
                    try {
                        Class<?> customStackClass = (Class<?>) MCPets.getItemsAdder();
                        Object customStack = customStackClass
                            .getMethod("getInstance", String.class)
                            .invoke(null, parts[1]);
            
                        if (customStack != null) {
                            itemStack = (ItemStack) customStackClass
                                .getMethod("getItemStack")
                                .invoke(customStack);
                            return itemStack;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Fetch the item stack within the registered ones
            itemStack = ItemsListConfig.getInstance().getItemStack(itemId);

            // If no item stack matches, then we'll see if it's a default MC material
            if (itemStack == null) {
                // Checking MC materials
                Material material = Arrays.stream(Material.values()).filter(mat -> mat.name().equalsIgnoreCase(itemId)).findFirst().orElse(null);
                if (material != null && !material.isAir()) {
                    itemStack = new ItemStack(material);
                    defaultMCItem = true;
                    // return the itemstack without adding localized information to it, coz it's a default item
                    return itemStack;
                }
                // We found no match, so we set the item to unknown
                itemStack = Items.UNKNOWN.getItem().clone();
            }
            ItemMeta meta = itemStack.getItemMeta();
            meta.setItemName("MCPets;Food;" + itemId);
            itemStack.setItemMeta(meta);
        }

        // Item Stack already setup
        return itemStack;
    }

    /**
     * Say whether this food is compatible with the given pet
     * If the food doesn't state any pet ids, then the food is compatible with any pet
     */
    public boolean isCompatibleWithPet(Pet pet) {
        if (pet == null)
            return false;
        if (petIds == null || petIds.isEmpty())
            return true;
        return petIds.contains(pet.getId());
    }


    /**
     * Register an owner in the waiting list so that they don't spam an item
     * and loose it unintentionally
     */
    private ArrayList<UUID> waitingListApply = new ArrayList<>();
    public void registerWaitingList(UUID owner, long delay) {
        if (waitingListApply.contains(owner))
            return;
        waitingListApply.add(owner);
        new BukkitRunnable() {
            @Override
            public void run() {
                waitingListApply.remove(owner);
            }
        }.runTaskLater(MCPets.getInstance(), delay);
    }

    private int getRemainingCooldownInSeconds(Pet pet) {
        long foodEatenTimestamp = pet.getFoodEatenTimestamp(id);
        long remainingCooldown = foodEatenTimestamp + cooldown - System.currentTimeMillis();
        return remainingCooldown < 0 ? 0 : (int) (remainingCooldown / 1000L);
    }

    /**
     * Give the food to the pet
     * @return value stating whether or not the food could be applied
     */
    public boolean apply(Pet pet, Player p) {
        if (pet == null)
            return false;

        if (waitingListApply.contains(pet.getOwner()))
            return false;

        if (type == null)
            return false;

        if (!isCompatibleWithPet(pet))
            return false;

        ItemStack mainHandItem = p.getInventory().getItemInMainHand();
        if (mainHandItem != null && mainHandItem.hasItemMeta()) {
            String itemName = mainHandItem.getItemMeta().getDisplayName();
            String expectedItemName = itemStack.getItemMeta().getDisplayName();

            if (itemId.startsWith("itemsadder:")) {
                if (!itemName.equals(expectedItemName)) {
                    return false;
                }
            }
        }

        int foodCooldown = getRemainingCooldownInSeconds(pet);
        if (foodCooldown > 0) {
            Debugger.send("§7NOT applying pet food §6" + this.id + "§7 to §6" + pet.getId() + "§7 because it's on cooldown for " + foodCooldown + "s more");
            Language.PET_FOOD_ON_COOLDOWN.sendMessageFormated(p, new FormatArg("%timeleft%", foodCooldown));
            return false;
        }

        // says whether the petfood was triggered or not
        boolean triggered = false;

        Debugger.send("§7Applying pet food §6" + this.id + "§7 to §6" + pet.getId() + "§7 with type §a" + this.type.getType());
        if (type.getType().equals(PetFoodType.HEALTH.getType())) {
            if (pet.getPetStats() != null && pet.getPetStats().getCurrentHealth() < pet.getPetStats().getCurrentLevel().getMaxHealth()) {
                pet.getPetStats().setHealth(operator.get(pet.getPetStats().getCurrentHealth(), power));
                triggered = true;
            }
            else {
                Debugger.send("§cCould not give HEALTH to the pet because it is already at maximum value.");
            }
        }
        else if (type.getType().toUpperCase().contains("BUFF")) {
            PetFoodBuff buff = new PetFoodBuff(pet, this.type, (float)this.power, this.operator, duration);
            triggered = buff.apply();
        }
        else if (type.getType().equals(PetFoodType.TAME.getType())) {
            if (pet.getTamingProgress() != 1) {
                pet.setTamingProgress(operator.get(pet.getTamingProgress(), power));
                triggered = true;
            }
            else {
                Debugger.send("§cCould not give TAMING PROGRESS to the pet because it has reached maximum value.");
            }
        }
        else if (type.getType().equals(PetFoodType.EXP.getType())) {
            if (pet.getPetStats() != null) {
                triggered = pet.getPetStats().addExperience(power);
                if (!triggered) {
                    Debugger.send("§cCould not give EXP to the pet because it has reached maximum value.");
                }
            }
        }
        else if (type.getType().equals(PetFoodType.EVOLUTION.getType())) {
            Pet evolutionPet = Pet.getFromId(evolution);
            if (pet.getPetStats() != null
                    && evolutionPet != null
                    && pet.getPetStats().getExperience() >= experienceThreshold) {
                PetLevel level = pet.getPetStats().getCurrentLevel();
                level.setDelayBeforeEvolution(delay);
                triggered = level.evolveTo(pet.getOwner(), false, evolutionPet);
            }
            else {
                Debugger.send("§cCould not evolve pet has conditions are not met or the evolution doesn't exist.");
            }
        }
        else if (type.getType().equals(PetFoodType.UNLOCK.getType())) {
            if (p != null) {
                if (permission != null && !p.hasPermission(permission)) {
                    Language.PETUNLOCK_NOPERM.sendMessage(p);
                    return false;
                }

                Pet unlockedPetObject = Pet.getFromId(unlockedPet);
                if (unlockedPetObject == null) {
                    Debugger.send("§7The player §c" + p.getName() + "§7 tried to unlock a pet using an unlock item but the pet §7"+ unlockedPet +"§7 does not exist.");
                    return false;
                }
                else if (p.hasPermission(unlockedPetObject.getPermission())) {
                    Debugger.send("§7The player §c" + p.getName() + "§7 tried to unlock a pet using an unlock item but they already own the pet.");
                    Language.PETUNLOCKED_ALREADY.sendMessageFormated(p, new FormatArg("%petName%", unlockedPetObject.getIcon().getItemMeta().getDisplayName()));
                    return false;
                }

                Utils.givePermission(p.getUniqueId(), unlockedPetObject.getPermission());
                Language.PETUNLOCKED.sendMessageFormated(p, new FormatArg("%petName%", unlockedPetObject.getIcon().getItemMeta().getDisplayName()));
                triggered = true;
            }
        }

        if (triggered) {
            pet.sendSignal(signal);
            pet.applyFoodCooldown(id);
        }

        registerWaitingList(pet.getOwner(), 5L);

        return triggered;
    }

    /**
     * Consume the pet food in the main hand of the player
     */
    public void consume(Player p) {
        if (p == null) return;
    
        ItemStack mainHandItem = p.getInventory().getItemInMainHand();
        if (mainHandItem == null || mainHandItem.getType().isAir()) return;
    
        int currentAmount = mainHandItem.getAmount();
        if (currentAmount <= 1) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        else {
            mainHandItem.setAmount(currentAmount - 1);
        }
    }

    /**
     * Get the pet food object from the item stack if it represents one
     */
    public static PetFood getFromItem(ItemStack it) {
        if (it == null || it.getType() == Material.AIR) {
            return null;
        }
    
        // find itemsadder item for pet food, expected "itemsadder-namespace:itemname"
        for (PetFood petFood : PetFoodConfig.getInstance().list()) {
            if (petFood.getItemId().startsWith("itemsadder-")) {
                String[] parts = petFood.getItemId().split("-", 2);
                if (parts.length == 2 && MCPets.getItemsAdder() != null) {
                    try {
                        Class<?> itemsAdderClass = (Class<?>) MCPets.getItemsAdder();
                        Object customStack = itemsAdderClass
                            .getMethod("getInstance", String.class)
                            .invoke(null, parts[1]);
    
                        if (customStack != null) {
                            ItemStack itemsAdderStack = (ItemStack) itemsAdderClass
                                .getMethod("getItemStack")
                                .invoke(customStack);
    
                            if (itemsAdderStack != null && it.isSimilar(itemsAdderStack)) {
                                return petFood;
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    
        // if the item is a default MC item, then look for possible matches
        for (PetFood petFood : PetFoodConfig.getInstance().list()) {
            if (!petFood.getItemId().startsWith("itemsadder-")) {
                ItemStack petFoodItem = petFood.getItemStack();
                if (petFoodItem != null && it.getType() == petFoodItem.getType() && petFood.isDefaultMCItem()) {
                    return petFood;
                }
            }
        }
    
        // if the item isn't a default MCItem, go through the localized informations
        if (it.hasItemMeta()) {
            ItemMeta itemMeta = it.getItemMeta();
            for (PetFood petFood : PetFoodConfig.getInstance().list()) {
                ItemStack petFoodItem = petFood.getItemStack();
                if (petFoodItem != null && petFoodItem.hasItemMeta()) {
                    ItemMeta petFoodMeta = petFoodItem.getItemMeta();
    
                    if (petFoodMeta.hasDisplayName() && itemMeta.hasDisplayName() &&
                        ChatColor.stripColor(petFoodMeta.getDisplayName()).equals(ChatColor.stripColor(itemMeta.getDisplayName()))) {
                        if (petFoodMeta.hasLore() && itemMeta.hasLore()) {
                            List<String> petFoodLore = petFoodMeta.getLore();
                            List<String> itemLore = itemMeta.getLore();
    
                            if (petFoodLore != null && itemLore != null && petFoodLore.equals(itemLore)) {
                                return petFood;
                            }
                        }
                        else if (!petFoodMeta.hasLore() && !itemMeta.hasLore()) {
                            return petFood;
                        }
                    }
                }
            }
        }
    
        return null;
    }    

    /**
     * Get the PetFood from the id
     */
    public static PetFood getFromId(String id) {
        return petFoodHashMap.get(id);
    }
}
