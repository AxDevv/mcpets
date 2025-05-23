package fr.nocsy.mcpets.events;

import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.livingpets.PetFood;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PetTamedByPlayerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;

    @Getter
    private final Pet pet;
    @Getter
    private final Player player;
    @Getter
    private final PetFood petFood;

    public PetTamedByPlayerEvent(Pet pet, Player player, PetFood petFood) {
        this.pet = pet;
        this.player = player;
        this.petFood = petFood;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        isCancelled = b;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
