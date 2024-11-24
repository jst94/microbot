package net.runelite.client.plugins.microbot.AutoBankSkillerPlugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.AutoBankSkillerPlugin.AutoBankSkillerConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;

@Slf4j
public class AutoBankSkillerScript extends Script {
    private final AutoBankSkillerConfig config;
    private static final int TICKS_BETWEEN_ACTIONS = 2;
    private int currentTick = 0;

    public AutoBankSkillerScript(AutoBankSkillerConfig config) {
        this.config = config;
    }

    @Override
    public boolean run() {
        if (currentTick++ < TICKS_BETWEEN_ACTIONS) {
            return true;
        }
        currentTick = 0;

        try {
            if (Rs2Inventory.isFull()) {
                bank();
            } else {
                skill();
            }
            sleepBetweenActions();
            return true;
        } catch (Exception e) {
            log.error("Error in AutoBankSkiller script", e);
            return false;
        }
    }

    private void bank() throws InterruptedException {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleep(Rs2Random.between(600, 1000));
            return;
        }

        // Deposit all items except knife for log cutting
        if (config.skillType() == AutoBankSkillerConfig.SkillType.FLETCHING && 
            config.fletchingType() == AutoBankSkillerConfig.FletchingType.CUT_LOGS) {
            Rs2Bank.depositAllExcept(config.secondaryItemId()); // Keep the knife
        } else {
            Rs2Bank.depositAll();
        }
        sleep(Rs2Random.between(600, 800));

        // Withdraw items based on fletching type
        if (config.skillType() == AutoBankSkillerConfig.SkillType.FLETCHING) {
            if (config.fletchingType() == AutoBankSkillerConfig.FletchingType.CUT_LOGS) {
                // For log cutting, only withdraw logs if we have a knife
                if (Rs2Inventory.hasItem(config.secondaryItemId())) {
                    Rs2Bank.withdrawX(config.itemId(), 27); // Withdraw 27 logs since we keep the knife
                } else {
                    Rs2Bank.withdrawX(config.secondaryItemId(), 1); // Withdraw knife if we don't have it
                }
            } else {
                // For bow stringing, withdraw 14 of each
                Rs2Bank.withdrawX(config.itemId(), 14);
                sleep(Rs2Random.between(600, 800));
                Rs2Bank.withdrawX(config.secondaryItemId(), 14);
            }
        } else {
            // For other skills, withdraw 14 of each
            Rs2Bank.withdrawX(config.itemId(), 14);
            sleep(Rs2Random.between(600, 800));
            if (config.secondaryItemId() != 0) {
                Rs2Bank.withdrawX(config.secondaryItemId(), 14);
            }
        }

        sleep(Rs2Random.between(600, 800));
        Rs2Bank.closeBank();
        sleep(Rs2Random.between(600, 800));
    }

    private void skill() throws InterruptedException {
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            sleep(Rs2Random.between(600, 800));
            return;
        }

        // Check if we have the required items
        if (!Rs2Inventory.hasItem(config.itemId()) || 
            (config.secondaryItemId() != 0 && !Rs2Inventory.hasItem(config.secondaryItemId()))) {
            return;
        }

        // Perform skilling action based on config
        switch (config.skillType()) {
            case FLETCHING:
                performFletching();
                break;
            case CRAFTING:
                performCrafting();
                break;
            case HERBLORE:
                performHerblore();
                break;
            default:
                log.warn("Unsupported skill type: " + config.skillType());
        }
    }

    private void performFletching() throws InterruptedException {
        if (config.fletchingType() == AutoBankSkillerConfig.FletchingType.CUT_LOGS) {
            // Use knife on logs
            Rs2Inventory.combine(config.secondaryItemId(), config.itemId());
            sleep(Rs2Random.between(600, 800));
            
            // Select the option from the menu
            Rs2Keyboard.typeString(String.valueOf(config.option()));
            sleep(Rs2Random.between(2000, 3000));
        } else {
            // String bow
            Rs2Inventory.combine(config.itemId(), config.secondaryItemId());
            sleep(Rs2Random.between(600, 800));
            Rs2Keyboard.typeString(" ");
            sleep(Rs2Random.between(2000, 3000));
        }
    }

    private void performCrafting() throws InterruptedException {
        Rs2Inventory.combine(config.itemId(), config.secondaryItemId());
        sleep(Rs2Random.between(600, 800));
        Rs2Keyboard.typeString(" ");
        sleep(Rs2Random.between(2000, 3000));
    }

    private void performHerblore() throws InterruptedException {
        Rs2Inventory.combine(config.itemId(), config.secondaryItemId());
        sleep(Rs2Random.between(600, 800));
        Rs2Keyboard.typeString(" ");
        sleep(Rs2Random.between(2000, 3000));
    }

    private void sleepBetweenActions() throws InterruptedException {
        sleep(Rs2Random.between(300, 500));
    }
}
