package net.earthmc.quarters.command.quartersadmin.method.set;

import net.earthmc.quarters.api.QuartersMessaging;
import net.earthmc.quarters.object.base.CommandMethod;
import net.earthmc.quarters.object.entity.Quarter;
import net.earthmc.quarters.object.exception.CommandMethodException;
import net.earthmc.quarters.object.state.ActionType;
import net.earthmc.quarters.object.state.PermLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminSetPermMethod extends CommandMethod {

    public AdminSetPermMethod(CommandSender sender, String[] args) {
        super(sender, args, "quarters.command.quartersadmin.set.perm");
    }

    @Override
    public void execute() {
        Player player = getSenderAsPlayerOrThrow();
        Quarter quarter = getQuarterAtPlayerOrThrow(player);

        ActionType type;
        try {
            type = ActionType.valueOf(getArgOrThrow(0, "No action type provided").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandMethodException("Invalid action type provided");
        }

        PermLevel level;
        try {
            level = PermLevel.valueOf(getArgOrThrow(1, "No perm level provided").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandMethodException("Invalid perm level provided");
        }

        boolean allowed = Boolean.parseBoolean(getArgOrThrow(2, "No boolean provided"));

        quarter.getPermissions().setPermission(type, level, allowed);
        quarter.save();

        String lowerCaseLevel = level.name().toLowerCase();
        String lowerCaseType = type.name().toLowerCase();

        QuartersMessaging.sendSuccessMessage(player, "Successfully set " + lowerCaseLevel + " " + lowerCaseType + " permissions to " + allowed);
    }
}