package net.earthmc.quarters.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Resident;
import net.earthmc.quarters.api.QuartersMessaging;
import net.earthmc.quarters.object.Quarter;
import net.earthmc.quarters.util.CommandUtil;
import net.earthmc.quarters.util.QuarterUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Objects;

@CommandAlias("quarters|q")
public class ClaimCommand extends BaseCommand {
    @Subcommand("claim")
    @Description("Claim a quarter")
    @CommandPermission("quarters.command.quarters.claim")
    public void onClaim(Player player) {
        if (!CommandUtil.isPlayerInQuarter(player))
            return;

        Quarter quarter = QuarterUtil.getQuarter(player.getLocation());
        assert quarter != null;

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null)
            return;
        if(QuarterUtil.playerHasQuarters(player)!=null){
            QuartersMessaging.sendErrorMessage(player, "您已经拥有一个公寓了，您已经拥有一个公寓了，输入/q home 回到公寓，输入/q unclaim退租之前的公寓，退租记得搬走个人物品噢");
            return;
        }
        if (Objects.equals(quarter.getOwnerResident(), resident)) {
            QuartersMessaging.sendErrorMessage(player, "您已经拥有这个公寓了");
            return;
        }

        if (quarter.getPrice() == null) {
            QuartersMessaging.sendErrorMessage(player, "这个公寓被设置为不出租");
            return;
        }

        if (!quarter.isEmbassy() && quarter.getTown() != resident.getTownOrNull()) {
            QuartersMessaging.sendErrorMessage(player, "你不能租用这个公寓，因为它不是大使馆，也不是你所在城镇的一部分");
            return;
        }

        if (TownyEconomyHandler.isActive() && resident.getAccount().getHoldingBalance() < quarter.getPrice()) {
            QuartersMessaging.sendErrorMessage(player, "你没有足够的资金来租用这个公寓");
            return;
        }

        sendClaimConfirmation(quarter, resident);
    }

    private void sendClaimConfirmation(Quarter quarter, Resident resident) {
        double currentPrice = quarter.getPrice();
        Player player = resident.getPlayer();

        if (currentPrice > 0) {
            Confirmation.runOnAccept(() -> {
                if (quarter.getPrice() != currentPrice) {
                    QuartersMessaging.sendErrorMessage(player, "Quarter purchase cancelled as the quarter's price has changed");
                    return;
                }

                resident.getAccount().withdraw(quarter.getPrice(), "为公寓支付 " + quarter.getUUID());
                quarter.getTown().getAccount().deposit(quarter.getPrice(), "为公寓支付 " + quarter.getUUID());

                setAndSaveQuarter(quarter, resident);

                QuartersMessaging.sendSuccessMessage(player, "您现在租用这个公寓了");

                Location location = player.getLocation();
                QuartersMessaging.sendInfoMessageToTown(quarter.getTown(), player, player.getName() + " 租用了一个公寓 " + QuartersMessaging.getLocationString(location));
            })
            .setTitle("租用这个公寓要花" + quarter.getPrice() + ", 您确定吗？如果确定请输入/confirm")
            .sendTo(resident.getPlayer());
        } else {
            setAndSaveQuarter(quarter, resident);

            QuartersMessaging.sendSuccessMessage(player, "您现在租用这个公寓了");

            Location location = player.getLocation();
            QuartersMessaging.sendInfoMessageToTown(quarter.getTown(), player, player.getName() + " 租用了一个公寓 " + QuartersMessaging.getLocationString(location));
        }
    }

    private void setAndSaveQuarter(Quarter quarter, Resident resident) {
        quarter.setOwner(resident.getUUID());
        quarter.setClaimedAt(Instant.now().toEpochMilli());
        quarter.setOverdueday(0);
        quarter.setOverdueTax((double) 0);
        quarter.setLastPrice(quarter.getPrice());
        quarter.setPrice(null);
        quarter.save();
    }
}
